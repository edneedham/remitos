package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/repository"
	"server/internal/validation"
)

type AdminHandler struct {
	userRepo   *repository.UserRepository
	deviceRepo *repository.DeviceRepository
	jwtSvc     *jwt.Service
}

func NewAdminHandler(userRepo *repository.UserRepository, deviceRepo *repository.DeviceRepository, jwtSvc *jwt.Service) *AdminHandler {
	return &AdminHandler{
		userRepo:   userRepo,
		deviceRepo: deviceRepo,
		jwtSvc:     jwtSvc,
	}
}

type CreateOperatorRequest struct {
	Email      *string `json:"email" validate:"omitempty"`
	Password   string  `json:"password" validate:"required,min=8,max=72"`
	DeviceName string  `json:"device_name" validate:"omitempty"`
}

func (h *AdminHandler) CreateOperator(w http.ResponseWriter, r *http.Request) {
	var req CreateOperatorRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	if errs := validation.Struct(req); errs != nil {
		for _, err := range errs {
			RespondWithError(w, ErrCodeInvalidRequest, err, http.StatusBadRequest)
			return
		}
	}

	ctx := r.Context()
	adminClaims := middleware.GetUserClaims(r)
	logger.Log.Info().Str("admin_id", adminClaims.UserID).Msg("Admin creating operator")

	// Check for existing user by email if email provided
	if req.Email != nil && *req.Email != "" {
		existing, err := h.userRepo.GetByEmail(ctx, *req.Email)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Error checking user")
			RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
			return
		}
		if existing != nil {
			RespondWithError(w, ErrCodeConflict, "El usuario ya existe", http.StatusConflict)
			return
		}
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error hashing password")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	user := &models.User{
		ID:           uuid.New(),
		Email:        req.Email,
		PasswordHash: string(hash),
		Role:         "operator",
		Status:       "active",
		CreatedAt:    time.Now(),
	}

	if err := h.userRepo.Create(ctx, user); err != nil {
		logger.Log.Error().Err(err).Msg("Error creating operator")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	logger.Log.Info().Str("operator_id", user.ID.String()).Str("admin_id", adminClaims.UserID).Msg("Operator created by admin")

	RespondWithJSON(w, http.StatusCreated, map[string]string{
		"message": "Operador creado exitosamente",
		"id":      user.ID.String(),
	})
}

func (h *AdminHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Post("/operadores", h.CreateOperator)
	return r
}
