package handlers

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/httputil"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	notifymail "server/internal/notifications/email"
	"server/internal/validation"
)

const (
	passwordResetTokenBytes = 32
	passwordResetTTL        = 1 * time.Hour
)

// ForgotPassword issues a reset link by email (same user resolution as login).
func (h *AuthHandler) ForgotPassword(w http.ResponseWriter, r *http.Request) {
	genericOK := map[string]string{
		"message": "Si los datos coinciden con una cuenta, te enviamos un correo con instrucciones.",
	}

	var req models.ForgotPasswordRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	validation.NormalizeForgotPasswordRequest(&req)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	if _, ok := h.mailer.(notifymail.Noop); ok {
		logger.Log.Info().Msg("forgot-password: email disabled; skipping")
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}
	if strings.TrimSpace(h.publicSiteURL) == "" {
		logger.Log.Warn().Msg("forgot-password: PUBLIC_SITE_URL is empty; cannot build reset link")
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}

	ctx := r.Context()
	company, err := h.companyRepo.GetByCode(ctx, req.CompanyCode)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}

	user, err := h.userRepo.GetByEmailAndCompanyID(ctx, req.Username, company.ID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if user == nil {
		user, err = h.userRepo.GetByUsernameAndCompanyID(ctx, req.Username, company.ID)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
			return
		}
	}
	if user == nil {
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}
	if user.Status != "active" {
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}
	if strings.TrimSpace(user.PasswordHash) == "" {
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}

	to := deliverableUserEmail(user)
	if to == "" {
		logger.Log.Info().Str("user_id", user.ID.String()).Msg("forgot-password: no email on file")
		RespondWithJSON(w, http.StatusOK, genericOK)
		return
	}

	if err := h.passwordResetTokenRepo.DeletePendingForUser(ctx, user.ID); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	raw := make([]byte, passwordResetTokenBytes)
	if _, err := rand.Read(raw); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	rawToken := base64.RawURLEncoding.EncodeToString(raw)
	hash := hashResetToken(rawToken)
	expires := time.Now().Add(passwordResetTTL)
	if _, err := h.passwordResetTokenRepo.Insert(ctx, user.ID, hash, expires); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	base := strings.TrimRight(strings.TrimSpace(h.publicSiteURL), "/")
	u, err := url.Parse(base + "/reset-password")
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	q := u.Query()
	q.Set("token", rawToken)
	u.RawQuery = q.Encode()
	resetURL := u.String()

	msg := notifymail.PasswordReset(to, resetURL, h.publicSiteURL)
	if err := h.mailer.Send(ctx, msg); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "No se pudo enviar el correo. Intentá más tarde.", http.StatusServiceUnavailable, err)
		return
	}

	RespondWithJSON(w, http.StatusOK, genericOK)
}

// ResetPassword sets a new password using a valid email token.
func (h *AuthHandler) ResetPassword(w http.ResponseWriter, r *http.Request) {
	var req models.ResetPasswordRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	req.Token = strings.TrimSpace(req.Token)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	hash := hashResetToken(req.Token)
	row, err := h.passwordResetTokenRepo.GetValidByTokenHash(ctx, hash)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if row == nil {
		RespondWithValidationError(w, r, "El enlace no es válido o expiró. Pedí un correo nuevo.", map[string]string{
			"token": "Enlace inválido o vencido.",
		}, http.StatusBadRequest)
		return
	}

	user, err := h.userRepo.GetByID(ctx, row.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if user == nil || user.Status != "active" {
		RespondWithValidationError(w, r, "No se pudo restablecer la contraseña.", map[string]string{
			"token": "Cuenta no disponible.",
		}, http.StatusBadRequest)
		return
	}

	newHash, err := bcrypt.GenerateFromPassword([]byte(req.NewPassword), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.userRepo.UpdatePassword(ctx, user.ID, string(newHash)); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.passwordResetTokenRepo.MarkUsed(ctx, row.ID); err != nil {
		logger.Log.Error().Err(err).Msg("reset-password: mark token used")
	}
	if err := h.refreshTokenRepo.RevokeUserTokens(ctx, user.ID); err != nil {
		logger.Log.Error().Err(err).Msg("reset-password: revoke refresh tokens")
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Contraseña actualizada. Podés iniciar sesión con la nueva clave.",
	})
}

// ChangePassword updates password for the authenticated user (any role).
func (h *AuthHandler) ChangePassword(w http.ResponseWriter, r *http.Request) {
	var req models.ChangePasswordRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	claims := middleware.GetUserClaims(r)
	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}

	ctx := r.Context()
	user, err := h.userRepo.GetByID(ctx, userID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if user == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Usuario no encontrado", http.StatusNotFound)
		return
	}
	if bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.CurrentPassword)) != nil {
		RespondWithValidationError(w, r, "La contraseña actual no es correcta.", map[string]string{
			"current_password": "Contraseña incorrecta.",
		}, http.StatusBadRequest)
		return
	}

	newHash, err := bcrypt.GenerateFromPassword([]byte(req.NewPassword), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.userRepo.UpdatePassword(ctx, user.ID, string(newHash)); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.refreshTokenRepo.RevokeUserTokens(ctx, user.ID); err != nil {
		logger.Log.Error().Err(err).Msg("change-password: revoke refresh tokens")
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Contraseña actualizada. Volvé a iniciar sesión en todos los dispositivos.",
	})
}

func deliverableUserEmail(u *models.User) string {
	if u == nil {
		return ""
	}
	if u.Email != nil {
		s := strings.TrimSpace(*u.Email)
		if s != "" {
			return s
		}
	}
	if u.Username != nil {
		s := strings.TrimSpace(*u.Username)
		if strings.Contains(s, "@") {
			return s
		}
	}
	return ""
}

func hashResetToken(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}
