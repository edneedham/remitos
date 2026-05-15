package handlers

import (
	"context"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"server/internal/httputil"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	notifymail "server/internal/notifications/email"
	"server/internal/repository"
	"server/internal/validation"
)

// PublicHandler serves unauthenticated public marketing endpoints (waitlist, etc.).
type PublicHandler struct {
	waitlistRepo  *repository.WaitlistRepository
	mailer        notifymail.Sender
	publicSiteURL string
}

// PublicHandlerConfig wires PublicHandler.
type PublicHandlerConfig struct {
	WaitlistRepo  *repository.WaitlistRepository
	Mailer        notifymail.Sender
	PublicSiteURL string
}

// NewPublicHandler builds a PublicHandler.
func NewPublicHandler(c PublicHandlerConfig) *PublicHandler {
	return &PublicHandler{
		waitlistRepo:  c.WaitlistRepo,
		mailer:        c.Mailer,
		publicSiteURL: strings.TrimRight(strings.TrimSpace(c.PublicSiteURL), "/"),
	}
}

// Routes mounts POST /waitlist (full path: /public/waitlist).
func (h *PublicHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Use(middleware.AuthEndpointsRateLimit())
	r.Post("/waitlist", h.PostWaitlist)
	return r
}

// PostWaitlist records contact and operational context for early access.
func (h *PublicHandler) PostWaitlist(w http.ResponseWriter, r *http.Request) {
	var req models.WaitlistRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	validation.NormalizeWaitlistRequest(&req)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	fullName := strPtrIfNonEmpty(req.FullName)
	companyName := strPtrIfNonEmpty(req.CompanyName)
	source := strPtrIfNonEmpty(req.Source)

	band := req.DeliveryNotesPerDayBand
	mode := req.ProcessingMode
	var digApp *string
	if mode == "digital" {
		digApp = strPtrIfNonEmpty(req.DigitalApplication)
	}
	logisticsPain := strPtrIfNonEmpty(req.LogisticsPainPoints)

	ctx := r.Context()
	row, err := h.waitlistRepo.Create(ctx, repository.WaitlistCreateParams{
		EmailNormalized:         req.Email,
		FullName:                fullName,
		CompanyName:             companyName,
		Source:                  source,
		DeliveryNotesPerDayBand: &band,
		ProcessingMode:          &mode,
		DigitalApplication:      digApp,
		LogisticsPainPoints:     logisticsPain,
		WarehouseCount:          req.WarehouseCount,
		ProductUpdatesOptIn:     req.ProductUpdatesOptIn,
	})
	if err != nil {
		if err == repository.ErrWaitlistDuplicate {
			RespondWithJSON(w, http.StatusOK, map[string]any{
				"message":            "Ya estás en la lista de espera. Te avisamos cuando abramos el registro.",
				"already_registered": true,
			})
			return
		}
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	h.queueWaitlistEmail(req.Email)

	RespondWithJSON(w, http.StatusCreated, map[string]any{
		"message": "Gracias. Te sumamos a la lista de espera.",
		"id":      row.ID.String(),
	})
}

func strPtrIfNonEmpty(s string) *string {
	s = strings.TrimSpace(s)
	if s == "" {
		return nil
	}
	return &s
}

func (h *PublicHandler) queueWaitlistEmail(recipient string) {
	msg := notifymail.WaitlistJoined(recipient, h.publicSiteURL)
	go func(m notifymail.Message) {
		if err := h.mailer.Send(context.Background(), m); err != nil {
			logger.Log.Error().Err(err).Str("to", recipient).Msg("waitlist confirmation email failed")
		}
	}(msg)
}
