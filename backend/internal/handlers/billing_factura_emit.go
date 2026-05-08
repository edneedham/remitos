package handlers

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/logger"
)

// BillingFacturaHandler triggers AFIP factura emission for a paid invoice (trusted ops only).
type BillingFacturaHandler struct {
	Emitter *billing.FacturaEmitter
}

func NewBillingFacturaHandler(emitter *billing.FacturaEmitter) *BillingFacturaHandler {
	return &BillingFacturaHandler{Emitter: emitter}
}

// PostEmitFactura enqueues an async TryEmit for the given billing_invoices row.
func (h *BillingFacturaHandler) PostEmitFactura(w http.ResponseWriter, r *http.Request) {
	if h == nil || h.Emitter == nil || !h.Emitter.BillingEnabled {
		RespondWithError(w, r, ErrCodeInternalError, "Emisión AFIP no disponible", http.StatusServiceUnavailable)
		return
	}
	raw := strings.TrimSpace(chi.URLParam(r, "invoiceID"))
	invID, err := uuid.Parse(raw)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "invoice_id inválido", http.StatusBadRequest)
		return
	}
	h.Emitter.ScheduleEmit(invID)
	logger.Log.Info().Str("invoice_id", invID.String()).Msg("afip factura emission enqueued (internal)")
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	_ = json.NewEncoder(w).Encode(map[string]string{"status": "queued", "invoice_id": invID.String()})
}
