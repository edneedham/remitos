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
	ErrCodeConflict       ErrorCode = "CONFLICT"
	ErrCodeNotFound       ErrorCode = "NOT_FOUND"
	ErrCodeInternalError  ErrorCode = "INTERNAL_ERROR"
)

type ErrorResponse struct {
	Error   ErrorCode `json:"error"`
	Message string    `json:"message"`
}

func RespondWithError(w http.ResponseWriter, code ErrorCode, message string, status int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	logger.Log.Error().Str("code", string(code)).Msg(message)
	json.NewEncoder(w).Encode(ErrorResponse{Error: code, Message: message})
}

func RespondWithJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}
