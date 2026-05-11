package handlers

import (
	"context"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/repository"
)

type notificationJSON struct {
	ID        string  `json:"id"`
	Kind      string  `json:"kind"`
	Title     string  `json:"title"`
	Body      *string `json:"body,omitempty"`
	ActionURL *string `json:"action_url,omitempty"`
	ReadAt    *string `json:"read_at,omitempty"`
	CreatedAt string  `json:"created_at"`
}

type notificationsListJSON struct {
	Notifications []notificationJSON `json:"notifications"`
	UnreadCount   int64              `json:"unread_count"`
}

func notificationToJSON(n models.UserNotification) notificationJSON {
	out := notificationJSON{
		ID:        n.ID.String(),
		Kind:      string(n.Kind),
		Title:     n.Title,
		Body:      n.Body,
		ActionURL: n.ActionURL,
		CreatedAt: n.CreatedAt.UTC().Format(time.RFC3339),
	}
	if n.ReadAt != nil {
		s := n.ReadAt.UTC().Format(time.RFC3339)
		out.ReadAt = &s
	}
	return out
}

// GetMeNotifications lists notifications for the authenticated web user.
func (h *AuthHandler) GetMeNotifications(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}

	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Usuario inválido", http.StatusBadRequest)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}

	q := r.URL.Query()
	limit := 50
	if v := strings.TrimSpace(q.Get("limit")); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n > 0 && n <= 100 {
			limit = n
		}
	}
	offset := 0
	if v := strings.TrimSpace(q.Get("offset")); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n >= 0 {
			offset = n
		}
	}
	unreadOnly := strings.EqualFold(q.Get("unread_only"), "1") || strings.EqualFold(q.Get("unread_only"), "true")

	if h.notificationRepo == nil {
		RespondWithError(w, r, ErrCodeInternalError, "Servicio no disponible", http.StatusServiceUnavailable)
		return
	}

	rows, err := h.notificationRepo.ListForUser(r.Context(), userID, companyID, limit, offset, unreadOnly)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	unread, err := h.notificationRepo.CountUnread(r.Context(), userID, companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	out := notificationsListJSON{
		Notifications: make([]notificationJSON, 0, len(rows)),
		UnreadCount:   unread,
	}
	for _, row := range rows {
		out.Notifications = append(out.Notifications, notificationToJSON(row))
	}

	RespondWithJSON(w, http.StatusOK, out)
}

// PatchMeNotificationRead marks a single notification as read.
func (h *AuthHandler) PatchMeNotificationRead(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}

	rawID := chi.URLParam(r, "notificationID")
	id, err := uuid.Parse(strings.TrimSpace(rawID))
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Notificación inválida", http.StatusBadRequest)
		return
	}

	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Usuario inválido", http.StatusBadRequest)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}

	if h.notificationRepo == nil {
		RespondWithError(w, r, ErrCodeInternalError, "Servicio no disponible", http.StatusServiceUnavailable)
		return
	}

	err = h.notificationRepo.MarkRead(r.Context(), id, userID, companyID)
	if errors.Is(err, repository.ErrNotificationNotFound) {
		RespondWithError(w, r, ErrCodeNotFound, "Notificación no encontrada", http.StatusNotFound)
		return
	}
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *AuthHandler) insertSignupWelcomeNotification(ctx context.Context, userID, companyID uuid.UUID) {
	if h.notificationRepo == nil {
		return
	}
	base := strings.TrimRight(strings.TrimSpace(h.publicSiteURL), "/")
	var action *string
	if base != "" {
		s := base + "/panel/aplicacion"
		action = &s
	}
	body := "Tu cuenta de prueba está lista. Instalá la app Android desde el panel para empezar a escanear remitos."
	n := &models.UserNotification{
		ID:        uuid.New(),
		UserID:    userID,
		CompanyID: companyID,
		Kind:      models.UserNotificationKindSignupWelcome,
		Title:     "Bienvenido a En Punto",
		Body:      &body,
		ActionURL: action,
		CreatedAt: time.Now().UTC(),
	}
	if err := h.notificationRepo.Insert(ctx, n); err != nil {
		logger.Log.Error().Err(err).Str("user_id", userID.String()).Msg("signup welcome notification failed")
	}
}
