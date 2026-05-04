package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/google/uuid"
	"server/internal/billing"
)

// BillingRenewalHandler exposes operations invoked by trusted automation (cron, workers).
type BillingRenewalHandler struct {
	svc *billing.RenewalService
}

func NewBillingRenewalHandler(svc *billing.RenewalService) *BillingRenewalHandler {
	return &BillingRenewalHandler{svc: svc}
}

type triggerRenewalRequest struct {
	CompanyID    string `json:"company_id"`
	AmountMinor  int64  `json:"amount_minor"`
	Currency     string `json:"currency"`
	Description  string `json:"description"`
	ExtendMonths int    `json:"extend_months"`
}

// PostTriggerRenewal runs invoice → charge → extend access for one company (paid plans only).
func (h *BillingRenewalHandler) PostTriggerRenewal(w http.ResponseWriter, r *http.Request) {
	var req triggerRenewalRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	companyID, err := uuid.Parse(req.CompanyID)
	if err != nil || companyID == uuid.Nil {
		RespondWithError(w, ErrCodeInvalidRequest, "company_id inválido", http.StatusBadRequest)
		return
	}
	// amount_minor > 0 uses that ARS total; 0 means catalog USD × MEP (bolsa) quote or BILLING_USD_ARS_RATE fallback.
	if req.AmountMinor < 0 {
		RespondWithError(w, ErrCodeInvalidRequest, "amount_minor inválido", http.StatusBadRequest)
		return
	}
	extendMonths := req.ExtendMonths
	if extendMonths <= 0 {
		extendMonths = 1
	}
	description := req.Description
	if description == "" {
		description = "Suscripción mensual"
	}

	res, runErr := h.svc.Run(r.Context(), billing.RenewalRunInput{
		CompanyID:    companyID,
		AmountMinor:  req.AmountMinor,
		Currency:     req.Currency,
		Description:  description,
		ExtendMonths: extendMonths,
	})
	if runErr != nil {
		if res != nil && res.InvoiceID != uuid.Nil && !res.Charged {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusUnprocessableEntity)
			_ = json.NewEncoder(w).Encode(map[string]any{
				"invoice_id": res.InvoiceID.String(),
				"charged":    false,
				"message":    runErr.Error(),
			})
			return
		}
		RespondWithError(w, ErrCodeInternalError, runErr.Error(), http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]any{
		"invoice_id":              res.InvoiceID.String(),
		"charged":                 res.Charged,
		"payment_ref":             res.PaymentRef,
		"subscription_expires_at": res.SubscriptionExpiresAt,
	})
}
