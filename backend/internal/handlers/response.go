package handlers

import (
	"encoding/json"
	"net/http"

	"server/internal/logger"
)

type ErrorCode string

const (
	ErrCodeInvalidRequest ErrorCode = "INVALID_REQUEST"
	ErrCodeUnauthorized   ErrorCode = "UNAUTHORIZED"
	ErrCodeForbidden      ErrorCode = "FORBIDDEN"
	ErrCodeConflict       ErrorCode = "CONFLICT"
	ErrCodeNotFound       ErrorCode = "NOT_FOUND"
	ErrCodeInternalError  ErrorCode = "INTERNAL_ERROR"
)

type ErrorResponse struct {
	Error   ErrorCode         `json:"error"`
	Message string            `json:"message"`
	Fields  map[string]string `json:"fields,omitempty"`
}

func RespondWithError(w http.ResponseWriter, code ErrorCode, message string, status int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	logger.Log.Error().Str("code", string(code)).Msg(message)
	json.NewEncoder(w).Encode(ErrorResponse{Error: code, Message: message})
}

// RespondWithValidationError returns INVALID_REQUEST with optional per-field messages (JSON keys).
func RespondWithValidationError(w http.ResponseWriter, message string, fields map[string]string, status int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	ev := logger.Log.Warn().Str("code", string(ErrCodeInvalidRequest)).Str("message", message)
	if len(fields) > 0 {
		ev = ev.Interface("fields", fields)
	}
	ev.Msg("validation error")
	json.NewEncoder(w).Encode(ErrorResponse{
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
