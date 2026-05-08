package billing

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
	arcatuit "server/internal/cuit"
	"server/internal/logger"
	"server/internal/models"
	"server/internal/payments/afip"
	"server/internal/payments/afip/padron"
	"server/internal/payments/afip/wsfev1"
	"server/internal/repository"
)

const (
	wsfeService   = "wsfe"
	padronService = "ws_sr_padron_a5"
)

// FacturaEmitter requests AFIP CAE for paid billing_invoices rows after Mercado Pago settlement.
type FacturaEmitter struct {
	Afip      *afip.Client
	TAMgr     *afip.TAManager
	Invoices  *repository.InvoiceRepository
	Companies *repository.CompanyRepository

	BillingEnabled bool // AFIP_BILLING_ENABLED
	PadronEnabled  bool // AFIP_PADRON_ENABLED
	// PadronCacheTTL: if >0 and company padron snapshot is newer than this, skip getPersona
	// and use stored condicion_iva / razon_social / cuit_estado. 0 = always call padron.
	PadronCacheTTL time.Duration
}

// ScheduleEmit runs TryEmit in a background goroutine (non-blocking webhook / renewal path).
func (e *FacturaEmitter) ScheduleEmit(invoiceID uuid.UUID) {
	if e == nil || !e.BillingEnabled || e.Afip == nil || e.TAMgr == nil {
		return
	}
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 120*time.Second)
		defer cancel()
		if err := e.TryEmit(ctx, invoiceID); err != nil {
			logger.Log.Warn().Err(err).Str("invoice_id", invoiceID.String()).Msg("afip factura emit failed")
		}
	}()
}

// TryEmit performs padron lookup (optional), WSFEv1 ultimo + FECAESolicitar, and persists CAE.
func (e *FacturaEmitter) TryEmit(ctx context.Context, invoiceID uuid.UUID) error {
	if e == nil || !e.BillingEnabled || e.Afip == nil || e.TAMgr == nil {
		return nil
	}

	inv, err := e.Invoices.GetByID(ctx, invoiceID)
	if err != nil || inv == nil {
		return fmt.Errorf("load invoice: %w", err)
	}
	if inv.Status != "paid" || inv.MpPaymentID == nil {
		return errors.New("invoice not in paid state with mp_payment_id")
	}
	if inv.FacturaEmittedAt.Valid {
		return nil
	}
	if inv.FacturaAttempts >= repository.MaxFacturaAttempts {
		return errors.New("max factura attempts reached")
	}

	co, err := e.Companies.GetByIDForBilling(ctx, inv.CompanyID)
	if err != nil || co == nil {
		return fmt.Errorf("load company: %w", err)
	}
	cuit := digitsOnly(co.Cuit)
	if len(cuit) != 11 {
		msg := "company CUIT missing or invalid (11 digits required for factura)"
		_ = e.Invoices.SetFacturaLastError(ctx, invoiceID, msg)
		_ = e.Invoices.RecordFacturaAttempt(ctx, invoiceID, false, "", msg, "", "")
		return errors.New(msg)
	}

	rid, err := e.Invoices.BeginFacturaAttempt(ctx, invoiceID)
	if err != nil {
		return err
	}
	_ = rid

	ws := wsfev1.New(e.Afip.WSFEv1URL(), nil)

	// ---- Padron (receptor fiscal condition) ----
	var recCond padron.CondicionIVA
	if e.PadronEnabled {
		now := time.Now()
		if padronTaxSnapshotFresh(co, e.PadronCacheTTL, now) {
			recCond = padron.CondicionIVA(strings.ToUpper(strings.TrimSpace(co.CondicionIVA)))
			logger.Log.Debug().
				Str("company_id", co.ID.String()).
				Str("cuit", cuit).
				Time("padron_synced_at", *co.PadronSyncedAt).
				Dur("cache_ttl", e.PadronCacheTTL).
				Msg("afip padron cache hit; skipping getPersona for WSFE")
		} else {
			padTA, err := e.TAMgr.Get(ctx, padronService)
			if err != nil {
				e.fail(ctx, invoiceID, "padron ta", err.Error(), "", "")
				return err
			}
			pc := padron.New(e.Afip.PadronURL(), nil)
			persona, err := pc.LookupCUIT(ctx, padron.Auth{
				Token: padTA.Token,
				Sign:  padTA.Sign,
				Cuit:  e.Afip.CUIT,
			}, cuit)
			if err != nil {
				// Fall back to stored company snapshot
				if strings.TrimSpace(co.CondicionIVA) != "" {
					recCond = padron.CondicionIVA(strings.ToUpper(co.CondicionIVA))
				} else {
					recCond = padron.CondicionNoCategorizado
				}
				logger.Log.Warn().Err(err).Str("cuit", cuit).Msg("padron lookup failed; using fallback condicion IVA")
			} else {
				recCond = persona.CondicionIVA
				_ = e.Companies.UpdateCUITPadronResult(ctx, co.ID, cuit, persona.RazonSocial, string(recCond), persona.DomicilioFiscal, persona.EstadoClave)
			}
		}
	} else {
		if s := strings.TrimSpace(co.CondicionIVA); s != "" {
			recCond = padron.CondicionIVA(strings.ToUpper(s))
		} else {
			recCond = padron.CondicionNoCategorizado
		}
	}

	cbteTipo := pickCbteTipo(e.Afip.IssuerCondicion, recCond)
	condRecID := condicionIVAReceptorID(recCond)

	// ---- WSFE ----
	feTA, err := e.TAMgr.Get(ctx, wsfeService)
	if err != nil {
		e.fail(ctx, invoiceID, "wsfe ta", err.Error(), "", "")
		return err
	}
	auth := wsfev1.Auth{Token: feTA.Token, Sign: feTA.Sign, Cuit: e.Afip.CUIT}

	ult, err := ws.FECompUltimoAutorizado(ctx, auth, e.Afip.PuntoVenta, cbteTipo)
	if err != nil {
		e.fail(ctx, invoiceID, "ultimo", err.Error(), "", "")
		return err
	}
	nextNro := ult + 1

	docNro, err := strconv.ParseInt(cuit, 10, 64)
	if err != nil {
		e.fail(ctx, invoiceID, "cuit", err.Error(), "", "")
		return err
	}

	arLoc, _ := time.LoadLocation("America/Argentina/Buenos_Aires")
	cbteFch := inv.IssuedAt.In(arLoc)
	if cbteFch.IsZero() {
		cbteFch = time.Now().In(arLoc)
	}
	servDesde := cbteFch
	servHasta := cbteFch
	vtoPago := cbteFch

	gross := float64(inv.AmountMinor) / 100.0
	req := wsfev1.SolicitarRequest{
		PtoVta:       e.Afip.PuntoVenta,
		CbteTipo:     cbteTipo,
		Concepto:     e.Afip.Concepto,
		DocTipo:      80,
		DocNro:       docNro,
		CbteNro:      nextNro,
		CbteFecha:    cbteFch,
		MonId:        "PES",
		MonCotiz:     1,
		FchServDesde: &servDesde,
		FchServHasta: &servHasta,
		FchVtoPago:   &vtoPago,
	}
	if condRecID != 0 {
		v := condRecID
		req.CondicionIVAReceptorID = &v
	}

	switch cbteTipo {
	case 1: // A
		neto, ivaAmt, tot := SplitGrossARSWithIVA(inv.AmountMinor, e.Afip.DefaultAlicuota)
		req.ImpTotal = tot
		req.ImpNeto = neto
		req.ImpIVA = ivaAmt
		req.IVA = []wsfev1.IVAAliquot{{
			IDAlicuota: 5, // 21 %
			BaseImp:    neto,
			Importe:    ivaAmt,
		}}
	default: // B / C — gross as one lump
		req.ImpTotal = gross
		req.ImpNeto = 0
		req.ImpIVA = 0
	}

	sol, err := ws.FECAESolicitar(ctx, auth, req)
	if err != nil {
		e.fail(ctx, invoiceID, "fecae", err.Error(), "", "")
		return err
	}

	if sol.Resultado != "A" || sol.CAE == "" {
		msg := fmt.Sprintf("AFIP rechazó el comprobante: resultado %q cae=%q obs=%v errs=%v",
			sol.Resultado, sol.CAE, sol.Observaciones, sol.Errors)
		_ = e.Invoices.RecordFacturaAttempt(ctx, invoiceID, false, "FECAE", msg, truncate(sol.RawXML, 12000), "")
		_ = e.Invoices.SetFacturaLastError(ctx, invoiceID, msg)
		return errors.New(msg)
	}
	_ = e.Invoices.RecordFacturaAttempt(ctx, invoiceID, true, "", "", truncate(sol.RawXML, 12000), "")

	caeVto, err := parseCAEVto(sol.CAEFchVto)
	if err != nil {
		logger.Log.Warn().Err(err).Str("vto", sol.CAEFchVto).Msg("parse CAE vto; using today+13d")
		caeVto = time.Now().UTC().AddDate(0, 0, 13)
	}

	if err := e.Invoices.MarkFacturaEmitted(ctx, invoiceID, cbteTipo, e.Afip.PuntoVenta, sol.CbteNro, sol.CAE, caeVto); err != nil {
		return err
	}

	logger.Log.Info().
		Str("invoice_id", invoiceID.String()).
		Str("cae", sol.CAE).
		Int("cbte_tipo", cbteTipo).
		Int64("nro", sol.CbteNro).
		Msg("afip factura emitted")
	return nil
}

// padronTaxSnapshotFresh is true when we can reuse DB-cached receptor data (AFIP padron changes rarely).
// Requires a prior successful sync timestamp and condicion IVA (needed for WSFE receptor tipo).
func padronTaxSnapshotFresh(co *models.Company, ttl time.Duration, now time.Time) bool {
	if ttl <= 0 || co == nil || co.PadronSyncedAt == nil {
		return false
	}
	if strings.TrimSpace(co.CondicionIVA) == "" {
		return false
	}
	synced := co.PadronSyncedAt.UTC()
	return now.Sub(synced) < ttl
}

func (e *FacturaEmitter) fail(ctx context.Context, invoiceID uuid.UUID, phase, detail, reqXML, respXML string) {
	msg := fmt.Sprintf("%s: %s", phase, detail)
	_ = e.Invoices.SetFacturaLastError(ctx, invoiceID, msg)
	_ = e.Invoices.RecordFacturaAttempt(ctx, invoiceID, false, phase, detail, reqXML, respXML)
}

func pickCbteTipo(issuer afip.IssuerCondicion, receptor padron.CondicionIVA) int {
	switch issuer {
	case afip.IssuerMonotributo, afip.IssuerExento:
		return 11 // C
	default:
		if receptor == padron.CondicionRI {
			return 1 // A
		}
		return 6 // B
	}
}

// AFIP Codigo de Condicion frente al IVA del receptor (RG 5616 et seq.) — subset used for SaaS B2B.
func condicionIVAReceptorID(c padron.CondicionIVA) int {
	switch c {
	case padron.CondicionRI:
		return 1
	case padron.CondicionExento:
		return 4
	case padron.CondicionConsumidorFinal:
		return 5
	case padron.CondicionMonotributo:
		return 6
	case padron.CondicionNoCategorizado, padron.CondicionDesconocido:
		return 7
	default:
		return 7
	}
}

func parseCAEVto(s string) (time.Time, error) {
	s = strings.TrimSpace(s)
	if len(s) == 8 {
		return time.ParseInLocation("20060102", s, time.UTC)
	}
	return time.Parse(time.RFC3339, s)
}

func digitsOnly(s string) string {
	var b strings.Builder
	for _, r := range s {
		if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	return b.String()
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}

// LookupPadronPersona resolves a CUIT against AFIP padrón without writing the database.
func LookupPadronPersona(ctx context.Context, e *FacturaEmitter, cuit string) (*padron.Persona, error) {
	if e == nil || e.Afip == nil || e.TAMgr == nil || !e.PadronEnabled {
		return nil, errors.New("afip padron not configured")
	}
	cuit = digitsOnly(cuit)
	if len(cuit) != 11 {
		return nil, errors.New("cuit must be 11 digits")
	}
	if !arcatuit.ValidChecksum(cuit) {
		return nil, errors.New("CUIT checksum invalid")
	}
	padTA, err := e.TAMgr.Get(ctx, padronService)
	if err != nil {
		return nil, err
	}
	pc := padron.New(e.Afip.PadronURL(), nil)
	return pc.LookupCUIT(ctx, padron.Auth{
		Token: padTA.Token,
		Sign:  padTA.Sign,
		Cuit:  e.Afip.CUIT,
	}, cuit)
}

// VerifyCompanyCUIT calls padrón only (no WSFE). Mutates company row on success.
func VerifyCompanyCUIT(ctx context.Context, e *FacturaEmitter, companyID uuid.UUID, cuit string) (*models.Company, error) {
	persona, err := LookupPadronPersona(ctx, e, cuit)
	if err != nil {
		return nil, err
	}
	normal := digitsOnly(cuit)
	if err := e.Companies.UpdateCUITPadronResult(ctx, companyID, normal, persona.RazonSocial, string(persona.CondicionIVA), persona.DomicilioFiscal, persona.EstadoClave); err != nil {
		return nil, err
	}
	return e.Companies.GetByIDForBilling(ctx, companyID)
}
