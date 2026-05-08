package handlers

import (
	"bytes"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/jung-kurt/gofpdf"
	"server/internal/middleware"
)

// GetMeInvoiceFacturaPDF streams a minimal PDF of the AFIP factura for a paid, CAE-issued invoice.
func (h *AuthHandler) GetMeInvoiceFacturaPDF(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	rawID := strings.TrimSpace(chi.URLParam(r, "invoiceID"))
	invoiceID, err := uuid.Parse(rawID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Comprobante inválido", http.StatusBadRequest)
		return
	}

	inv, err := h.invoiceRepo.GetByID(r.Context(), invoiceID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if inv == nil || inv.CompanyID != companyID {
		RespondWithError(w, r, ErrCodeNotFound, "Comprobante no encontrado", http.StatusNotFound)
		return
	}
	if !inv.FacturaEmittedAt.Valid || !inv.FacturaCAE.Valid || !inv.FacturaCAEVto.Valid ||
		!inv.FacturaTipo.Valid || !inv.FacturaPtoVta.Valid || !inv.FacturaNumero.Valid {
		RespondWithError(w, r, ErrCodeNotFound, "Factura electrónica aún no disponible", http.StatusNotFound)
		return
	}

	co, err := h.companyRepo.GetByIDForBilling(r.Context(), companyID)
	if err != nil || co == nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	issuerCUIT := ""
	if h.afipClient != nil {
		issuerCUIT = h.afipClient.CUIT
	}

	pdfBytes, err := buildFacturaPDFBytes(
		co.Name,
		strings.TrimSpace(co.Cuit),
		inv.AmountMinor,
		inv.Currency,
		inv.Description,
		int(inv.FacturaTipo.Int32),
		int(inv.FacturaPtoVta.Int32),
		inv.FacturaNumero.Int64,
		inv.FacturaCAE.String,
		inv.FacturaCAEVto.Time,
		issuerCUIT,
	)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al generar PDF", http.StatusInternalServerError, err)
		return
	}

	w.Header().Set("Content-Type", "application/pdf")
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="remitos-factura-%s.pdf"`, invoiceID.String()))
	_, _ = w.Write(pdfBytes)
}

// buildFacturaPDFBytes generates an A4 PDF text summary (CAE, punto de venta, número).
func buildFacturaPDFBytes(
	companyName, companyCUIT string,
	amountMinor int64,
	currency, description string,
	tipo, ptoVta int,
	numero int64,
	cae string,
	caeVto time.Time,
	issuerCUIT string,
) ([]byte, error) {
	pdf := gofpdf.New("P", "mm", "A4", "")
	pdf.SetTitle("Comprobante AFIP", false)
	pdf.AddPage()
	pdf.SetFont("Helvetica", "B", 14)
	pdf.Cell(0, 10, "Remitos — Factura electrónica")
	pdf.Ln(12)
	pdf.SetFont("Helvetica", "", 11)
	pdf.MultiCell(0, 6, fmt.Sprintf("Receptor: %s\nCUIT receptor: %s\n", sanitizePDFText(companyName), companyCUIT), "", "L", false)
	pdf.Ln(4)
	line := fmt.Sprintf("Tipo comprobante: %d  Punto de venta: %04d  Número: %08d", tipo, ptoVta, numero)
	pdf.Cell(0, 6, line)
	pdf.Ln(8)
	ars := fmt.Sprintf("%s %.2f", strings.ToUpper(currency), float64(amountMinor)/100.0)
	pdf.Cell(0, 6, "Importe: "+ars)
	pdf.Ln(6)
	if description != "" {
		pdf.MultiCell(0, 6, "Concepto: "+sanitizePDFText(description), "", "L", false)
		pdf.Ln(2)
	}
	pdf.SetFont("Helvetica", "B", 11)
	pdf.Cell(0, 6, "CAE: "+cae)
	pdf.Ln(6)
	pdf.SetFont("Helvetica", "", 10)
	pdf.Cell(0, 6, "Vencimiento CAE: "+caeVto.Format("02/01/2006"))
	pdf.Ln(8)
	pdf.SetFont("Helvetica", "", 9)
	if issuerCUIT != "" {
		pdf.MultiCell(0, 5, "Emisor (Remitos) CUIT: "+issuerCUIT, "", "L", false)
	}
	pdf.MultiCell(0, 5, "Verificación: https://www.afip.gob.ar/fe/consultas/", "", "L", false)

	var buf bytes.Buffer
	if err := pdf.Output(&buf); err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func sanitizePDFText(s string) string {
	s = strings.ReplaceAll(s, "\r", " ")
	s = strings.ReplaceAll(s, "\n", " ")
	return strings.TrimSpace(s)
}
