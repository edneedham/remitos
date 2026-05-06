package handlers

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"

	"github.com/jackc/pgx/v5"
	"github.com/rs/zerolog"
	"server/internal/logger"
	"server/internal/middleware"
)

type ErrorCode string

const (
	ErrCodeInvalidRequest  ErrorCode = "INVALID_REQUEST"
	ErrCodeUnauthorized    ErrorCode = "UNAUTHORIZED"
	ErrCodeForbidden       ErrorCode = "FORBIDDEN"
	ErrCodeConflict        ErrorCode = "CONFLICT"
	ErrCodeNotFound        ErrorCode = "NOT_FOUND"
	ErrCodeInternalError   ErrorCode = "INTERNAL_ERROR"
	ErrCodePaymentRequired ErrorCode = "PAYMENT_REQUIRED"
)

type ErrorResponse struct {
	Error   ErrorCode         `json:"error"`
	Message string            `json:"message"`
	Fields  map[string]string `json:"fields,omitempty"`
}

// RespondWithError writes a JSON error and logs it. Optional cause (first non-nil) is included for server-side diagnosis.
// Expected NOT_FOUND with sql.ErrNoRows / pgx.ErrNoRows is logged at debug to reduce noise.
func RespondWithError(w http.ResponseWriter, r *http.Request, code ErrorCode, message string, status int, cause ...error) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)

	causeErr := firstCause(cause)
	var ev *zerolog.Event
	logMsg := "api error"
	if code == ErrCodeNotFound && isExpectedNoRows(causeErr) {
		ev = logger.Log.Debug()
		logMsg = "api error (expected not found)"
	} else {
		ev = logger.Log.Error()
	}

	ev = ev.Str("code", string(code)).Str("client_message", message)
	ev = enrichFromRequest(ev, r)

	if causeErr != nil && !(code == ErrCodeNotFound && isExpectedNoRows(causeErr)) {
		ev = ev.Err(causeErr)
	}
	ev.Msg(logMsg)
	_ = json.NewEncoder(w).Encode(ErrorResponse{Error: code, Message: message})
}

func firstCause(cause []error) error {
	if len(cause) > 0 {
		return cause[0]
	}
	return nil
}

func isExpectedNoRows(err error) bool {
	if err == nil {
		return false
	}
	return errors.Is(err, sql.ErrNoRows) || errors.Is(err, pgx.ErrNoRows)
}

func enrichFromRequest(ev *zerolog.Event, r *http.Request) *zerolog.Event {
	if r == nil {
		return ev
	}
	if id := middleware.GetRequestID(r); id != "" {
		ev = ev.Str("request_id", id)
	}
	claims := middleware.GetUserClaims(r)
	if claims.UserID != "" {
		ev = ev.Str("user_id", claims.UserID)
	}
	if claims.CompanyID != "" {
		ev = ev.Str("company_id", claims.CompanyID)
	}
	if ip := middleware.ClientIP(r); ip != "" {
		ev = ev.Str("client_ip", ip)
	}
	return ev
}

// RespondWithValidationError returns INVALID_REQUEST with optional per-field messages (JSON keys).
func RespondWithValidationError(w http.ResponseWriter, r *http.Request, message string, fields map[string]string, status int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	ev := logger.Log.Warn().Str("code", string(ErrCodeInvalidRequest)).Str("message", message)
	ev = enrichFromRequest(ev, r)
	if len(fields) > 0 {
		ev = ev.Interface("fields", fields)
	}
	ev.Msg("validation error")
	_ = json.NewEncoder(w).Encode(ErrorResponse{
		Error:   ErrCodeInvalidRequest,
		Message: message,
		Fields:  fields,
	})
}

func RespondWithJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}
