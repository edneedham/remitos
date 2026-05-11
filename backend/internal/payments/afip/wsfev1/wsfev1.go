// Package wsfev1 is a hand-rolled SOAP client for AFIP/ARCA's WSFEv1 (Factura Electrónica).
//
// This implementation focuses on the minimum surface needed for SaaS subscription invoices:
//
//	FEDummy                 — health probe (auth not required for app/db/auth pings)
//	FECompUltimoAutorizado  — last authorized comprobante number for (ptoVta, tipo)
//	FEParamGetTiposCbte     — catalogue of comprobante types for the issuer
//	FECAESolicitar          — request a CAE for one or more comprobantes (we send 1 at a time)
//
// The client is auth-aware: callers pass a TA (from wsaa) and the issuer CUIT in every call.
package wsfev1

import (
	"bytes"
	"context"
	"encoding/xml"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// Client is a thin SOAP wrapper around the WSFEv1 service.asmx endpoint.
type Client struct {
	URL  string
	HTTP *http.Client
}

func New(url string, hc *http.Client) *Client {
	if hc == nil {
		hc = &http.Client{Timeout: 60 * time.Second}
	}
	return &Client{URL: url, HTTP: hc}
}

// Auth carries the WSAA-issued credentials plus the emisor CUIT, included in every call's
// <Auth> element.
type Auth struct {
	Token string
	Sign  string
	Cuit  string
}

// SolicitarRequest is the high-level payload for FECAESolicitar (single-comprobante variant).
// AFIP supports batches but we keep it simple: one CAE request per billing_invoice.
type SolicitarRequest struct {
	PtoVta     int
	CbteTipo   int       // 1=A, 6=B, 11=C, ...
	Concepto   int       // 1=Productos, 2=Servicios, 3=Productos y Servicios
	DocTipo    int       // 80=CUIT, 86=CUIL, 96=DNI, 99=ConsumidorFinal
	DocNro     int64     // receptor identification (0 for Consumidor Final w/ tope)
	CbteNro    int64     // (ult+1) — caller responsibility
	CbteFecha  time.Time // YYYYMMDD
	ImpTotal   float64   // total ARS (gross)
	ImpNeto    float64   // sólo factura A: importe neto gravado
	ImpIVA     float64   // sólo factura A
	ImpTotConc float64   // 0 para SaaS estándar
	ImpOpEx    float64   // 0
	ImpTrib    float64   // 0
	MonId      string    // "PES"
	MonCotiz   float64   // 1
	// Cuando Concepto != 1, AFIP exige FchServDesde / FchServHasta / FchVtoPago.
	FchServDesde *time.Time
	FchServHasta *time.Time
	FchVtoPago   *time.Time
	// Sólo factura A: alícuotas IVA. Para B/C, AFIP no acepta IVA discriminado.
	IVA []IVAAliquot
	// Receptor condición IVA (RG 5616 / "CondicionIVAReceptorId"). Optional but recommended.
	CondicionIVAReceptorID *int
}

// IVAAliquot is one row of the FECAE IVA array.
type IVAAliquot struct {
	IDAlicuota int // 5=21%, 4=10.5%, 6=27%, 8=5%, 9=2.5%, 3=0%
	BaseImp    float64
	Importe    float64
}

// SolicitarResponse is the parsed outcome of a successful FECAESolicitar call. Errors at the
// SOAP layer return Go errors; AFIP "Rechazado" responses come back here with Resultado="R"
// and Observaciones populated for the caller to log/persist.
type SolicitarResponse struct {
	Resultado     string // "A" approved, "R" rejected, "P" partial
	CAE           string
	CAEFchVto     string // YYYYMMDD
	CbteNro       int64
	Observaciones []ObservacionEntry
	Errors        []ErrorEntry
	RawXML        string // full SOAP body for audit
}

type ObservacionEntry struct {
	Code int    `xml:"Code"`
	Msg  string `xml:"Msg"`
}

type ErrorEntry struct {
	Code int    `xml:"Code"`
	Msg  string `xml:"Msg"`
}

// FEDummy probes auth/app/db readiness. Returns the three OK/ER strings or any transport error.
func (c *Client) FEDummy(ctx context.Context) (appServer, dbServer, authServer string, err error) {
	const action = "http://ar.gov.afip.dif.FEV1/FEDummy"
	body := `<FEDummy xmlns="http://ar.gov.afip.dif.FEV1"/>`
	raw, err := c.callSOAP(ctx, action, body)
	if err != nil {
		return "", "", "", err
	}
	type result struct {
		AppServer  string `xml:"AppServer"`
		DbServer   string `xml:"DbServer"`
		AuthServer string `xml:"AuthServer"`
	}
	var r result
	if err := unmarshalSOAPResult(raw, "FEDummyResult", &r); err != nil {
		return "", "", "", err
	}
	return r.AppServer, r.DbServer, r.AuthServer, nil
}

// FECompUltimoAutorizado returns the last comprobante number issued for (ptoVta, tipo).
func (c *Client) FECompUltimoAutorizado(ctx context.Context, auth Auth, ptoVta, cbteTipo int) (int64, error) {
	const action = "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado"
	body := fmt.Sprintf(`<FECompUltimoAutorizado xmlns="http://ar.gov.afip.dif.FEV1">
  %s
  <PtoVta>%d</PtoVta>
  <CbteTipo>%d</CbteTipo>
</FECompUltimoAutorizado>`, authXML(auth), ptoVta, cbteTipo)
	raw, err := c.callSOAP(ctx, action, body)
	if err != nil {
		return 0, err
	}
	type result struct {
		PtoVta   int   `xml:"PtoVta"`
		CbteTipo int   `xml:"CbteTipo"`
		CbteNro  int64 `xml:"CbteNro"`
		Errors   struct {
			Err []ErrorEntry `xml:"Err"`
		} `xml:"Errors"`
	}
	var r result
	if err := unmarshalSOAPResult(raw, "FECompUltimoAutorizadoResult", &r); err != nil {
		return 0, err
	}
	if len(r.Errors.Err) > 0 {
		return 0, fmt.Errorf("wsfev1: FECompUltimoAutorizado: %s", formatErrors(r.Errors.Err))
	}
	return r.CbteNro, nil
}

// FECAESolicitar requests a CAE for the provided single comprobante. Caller is responsible
// for choosing CbteNro = ultimoAutorizado + 1.
func (c *Client) FECAESolicitar(ctx context.Context, auth Auth, in SolicitarRequest) (*SolicitarResponse, error) {
	const action = "http://ar.gov.afip.dif.FEV1/FECAESolicitar"
	body := buildFECAESolicitarBody(auth, in)
	raw, err := c.callSOAP(ctx, action, body)
	if err != nil {
		return nil, err
	}
	parsed, perr := parseFECAESolicitarResult(raw)
	if perr != nil {
		return nil, perr
	}
	parsed.RawXML = string(raw)
	return parsed, nil
}

func authXML(a Auth) string {
	return fmt.Sprintf(`<Auth>
    <Token>%s</Token>
    <Sign>%s</Sign>
    <Cuit>%s</Cuit>
  </Auth>`, escape(a.Token), escape(a.Sign), escape(a.Cuit))
}

func buildFECAESolicitarBody(auth Auth, in SolicitarRequest) string {
	fecha := in.CbteFecha.UTC().Format("20060102")
	var optServiceDates string
	if in.Concepto != 1 {
		desde := fmtDateOrCbte(in.FchServDesde, in.CbteFecha)
		hasta := fmtDateOrCbte(in.FchServHasta, in.CbteFecha)
		vto := fmtDateOrCbte(in.FchVtoPago, in.CbteFecha)
		optServiceDates = fmt.Sprintf(`
              <FchServDesde>%s</FchServDesde>
              <FchServHasta>%s</FchServHasta>
              <FchVtoPago>%s</FchVtoPago>`, desde, hasta, vto)
	}

	var ivaXML string
	if len(in.IVA) > 0 {
		var b strings.Builder
		b.WriteString("<Iva>")
		for _, a := range in.IVA {
			b.WriteString(fmt.Sprintf(`<AlicIva><Id>%d</Id><BaseImp>%s</BaseImp><Importe>%s</Importe></AlicIva>`,
				a.IDAlicuota, ars(a.BaseImp), ars(a.Importe)))
		}
		b.WriteString("</Iva>")
		ivaXML = b.String()
	}

	var condReceptor string
	if in.CondicionIVAReceptorID != nil {
		condReceptor = fmt.Sprintf("<CondicionIVAReceptorId>%d</CondicionIVAReceptorId>", *in.CondicionIVAReceptorID)
	}

	monID := strings.TrimSpace(in.MonId)
	if monID == "" {
		monID = "PES"
	}
	monCotiz := in.MonCotiz
	if monCotiz <= 0 {
		monCotiz = 1
	}

	return fmt.Sprintf(`<FECAESolicitar xmlns="http://ar.gov.afip.dif.FEV1">
  %s
  <FeCAEReq>
    <FeCabReq>
      <CantReg>1</CantReg>
      <PtoVta>%d</PtoVta>
      <CbteTipo>%d</CbteTipo>
    </FeCabReq>
    <FeDetReq>
      <FECAEDetRequest>
        <Concepto>%d</Concepto>
        <DocTipo>%d</DocTipo>
        <DocNro>%d</DocNro>
        <CbteDesde>%d</CbteDesde>
        <CbteHasta>%d</CbteHasta>
        <CbteFch>%s</CbteFch>
        <ImpTotal>%s</ImpTotal>
        <ImpTotConc>%s</ImpTotConc>
        <ImpNeto>%s</ImpNeto>
        <ImpOpEx>%s</ImpOpEx>
        <ImpTrib>%s</ImpTrib>
        <ImpIVA>%s</ImpIVA>%s
        <MonId>%s</MonId>
        <MonCotiz>%s</MonCotiz>
        %s
        %s
      </FECAEDetRequest>
    </FeDetReq>
  </FeCAEReq>
</FECAESolicitar>`,
		authXML(auth),
		in.PtoVta,
		in.CbteTipo,
		in.Concepto,
		in.DocTipo,
		in.DocNro,
		in.CbteNro, in.CbteNro,
		fecha,
		ars(in.ImpTotal),
		ars(in.ImpTotConc),
		ars(in.ImpNeto),
		ars(in.ImpOpEx),
		ars(in.ImpTrib),
		ars(in.ImpIVA),
		optServiceDates,
		monID,
		ars(monCotiz),
		ivaXML,
		condReceptor,
	)
}

func fmtDateOrCbte(d *time.Time, cbteFecha time.Time) string {
	if d != nil && !d.IsZero() {
		return d.UTC().Format("20060102")
	}
	return cbteFecha.UTC().Format("20060102")
}

func parseFECAESolicitarResult(raw []byte) (*SolicitarResponse, error) {
	type detResp struct {
		Concepto      int    `xml:"Concepto"`
		DocTipo       int    `xml:"DocTipo"`
		DocNro        int64  `xml:"DocNro"`
		CbteDesde     int64  `xml:"CbteDesde"`
		CbteHasta     int64  `xml:"CbteHasta"`
		CbteFch       string `xml:"CbteFch"`
		Resultado     string `xml:"Resultado"`
		CAE           string `xml:"CAE"`
		CAEFchVto     string `xml:"CAEFchVto"`
		Observaciones struct {
			Obs []ObservacionEntry `xml:"Obs"`
		} `xml:"Observaciones"`
	}
	type result struct {
		FeCabResp struct {
			Resultado string `xml:"Resultado"`
		} `xml:"FeCabResp"`
		FeDetResp struct {
			FECAEDetResponse []detResp `xml:"FECAEDetResponse"`
		} `xml:"FeDetResp"`
		Errors struct {
			Err []ErrorEntry `xml:"Err"`
		} `xml:"Errors"`
	}
	var r result
	if err := unmarshalSOAPResult(raw, "FECAESolicitarResult", &r); err != nil {
		return nil, err
	}
	resp := &SolicitarResponse{
		Resultado: r.FeCabResp.Resultado,
		Errors:    append([]ErrorEntry(nil), r.Errors.Err...),
	}
	if len(r.FeDetResp.FECAEDetResponse) > 0 {
		d := r.FeDetResp.FECAEDetResponse[0]
		resp.CAE = d.CAE
		resp.CAEFchVto = d.CAEFchVto
		resp.CbteNro = d.CbteDesde
		if d.Resultado != "" {
			resp.Resultado = d.Resultado
		}
		resp.Observaciones = append([]ObservacionEntry(nil), d.Observaciones.Obs...)
	}
	return resp, nil
}

func formatErrors(errs []ErrorEntry) string {
	parts := make([]string, 0, len(errs))
	for _, e := range errs {
		parts = append(parts, fmt.Sprintf("[%d] %s", e.Code, e.Msg))
	}
	return strings.Join(parts, "; ")
}

// callSOAP wraps the body element in a SOAP 1.1 envelope and returns the body XML.
func (c *Client) callSOAP(ctx context.Context, soapAction, innerBody string) ([]byte, error) {
	envelope := fmt.Sprintf(`<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Header/>
  <soapenv:Body>
    %s
  </soapenv:Body>
</soapenv:Envelope>`, innerBody)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.URL, strings.NewReader(envelope))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "text/xml; charset=utf-8")
	req.Header.Set("SOAPAction", soapAction)

	resp, err := c.HTTP.Do(req)
	if err != nil {
		return nil, fmt.Errorf("wsfev1: post: %w", err)
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, fmt.Errorf("wsfev1: status %d: %s", resp.StatusCode, truncate(string(raw), 1024))
	}
	return raw, nil
}

// unmarshalSOAPResult walks the SOAP envelope to find the *Result child element and decodes
// it into v. AFIP wraps response payloads with names like "FECAESolicitarResult" inside
// "FECAESolicitarResponse" — we don't validate the outer name to stay flexible.
func unmarshalSOAPResult(soap []byte, resultElement string, v interface{}) error {
	dec := xml.NewDecoder(bytes.NewReader(soap))
	for {
		tok, err := dec.Token()
		if err == io.EOF {
			return fmt.Errorf("element %s not found", resultElement)
		}
		if err != nil {
			return err
		}
		if se, ok := tok.(xml.StartElement); ok && se.Name.Local == resultElement {
			return dec.DecodeElement(v, &se)
		}
	}
}

func ars(v float64) string {
	return fmt.Sprintf("%.2f", v)
}

func escape(s string) string {
	r := strings.NewReplacer(
		"&", "&amp;",
		"<", "&lt;",
		">", "&gt;",
	)
	return r.Replace(s)
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}
