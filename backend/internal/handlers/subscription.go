package handlers

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/httputil"
	"server/internal/middleware"
	"server/internal/repository"
)

type SubscriptionHandler struct {
	subscriptionRepo *repository.SubscriptionRepository
}

func NewSubscriptionHandler(subscriptionRepo *repository.SubscriptionRepository) *SubscriptionHandler {
	return &SubscriptionHandler{
		subscriptionRepo: subscriptionRepo,
	}
}

func (h *SubscriptionHandler) GetMySubscription(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	userClaims := middleware.GetUserClaims(r)
	if userClaims.UserID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
		return
	}

	userID, err := uuid.Parse(userClaims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Invalid user ID", http.StatusBadRequest)
		return
	}

	sub, err := h.subscriptionRepo.GetByUserID(ctx, userID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Internal server error", http.StatusInternalServerError, err)
		return
	}
	if sub == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Subscription not found", http.StatusNotFound)
		return
	}

	RespondWithJSON(w, http.StatusOK, sub)
}

type LinkDeviceRequest struct {
	DeviceID string `json:"device_id" validate:"required"`
}

func (h *SubscriptionHandler) LinkDevice(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	userClaims := middleware.GetUserClaims(r)
	if userClaims.UserID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
		return
	}

	userID, err := uuid.Parse(userClaims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Invalid user ID", http.StatusBadRequest)
		return
	}

	var req LinkDeviceRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAPIBody, &req) {
		return
	}

	deviceID, err := uuid.Parse(req.DeviceID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Invalid device ID", http.StatusBadRequest)
		return
	}

	if err := h.subscriptionRepo.LinkDevice(ctx, userID, deviceID); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Failed to link device", http.StatusInternalServerError, err)
		return
	}

	sub, err := h.subscriptionRepo.GetByUserID(ctx, userID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Internal server error", http.StatusInternalServerError, err)
		return
	}

	RespondWithJSON(w, http.StatusOK, sub)
}

func (h *SubscriptionHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Get("/", h.GetMySubscription)
	r.Post("/link-device", h.LinkDevice)
	return r
}
