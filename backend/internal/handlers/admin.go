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

	companyID, err := uuid.Parse(adminClaims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de empresa inválido", http.StatusBadRequest)
		return
	}

	user := &models.User{
		ID:           uuid.New(),
		CompanyID:    companyID,
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

type OperatorResponse struct {
	ID        uuid.UUID `json:"id"`
	Email     *string   `json:"email"`
	Username  *string   `json:"username"`
	Role      string    `json:"role"`
	Status    string    `json:"status"`
	CreatedAt time.Time `json:"created_at"`
}

func (h *AdminHandler) GetOperators(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	adminClaims := middleware.GetUserClaims(r)
	companyID, err := uuid.Parse(adminClaims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de empresa inválido", http.StatusBadRequest)
		return
	}

	users, err := h.userRepo.GetByCompanyID(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error fetching operators")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	var response []OperatorResponse
	for _, user := range users {
		if user.Role == "operator" {
			response = append(response, OperatorResponse{
				ID:        user.ID,
				Email:     user.Email,
				Username:  user.Username,
				Role:      user.Role,
				Status:    user.Status,
				CreatedAt: user.CreatedAt,
			})
		}
	}

	// Make sure we always return an array, even if empty
	if response == nil {
		response = []OperatorResponse{}
	}

	RespondWithJSON(w, http.StatusOK, response)
}

type UpdateStatusRequest struct {
	Status string `json:"status" validate:"required,oneof=active suspended"`
}

func (h *AdminHandler) UpdateOperatorStatus(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de operador inválido", http.StatusBadRequest)
		return
	}

	var req UpdateStatusRequest
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

	// Check if user exists and belongs to the same company
	user, err := h.userRepo.GetByID(ctx, id)
	if err != nil || user == nil {
		RespondWithError(w, ErrCodeNotFound, "Operador no encontrado", http.StatusNotFound)
		return
	}

	adminClaims := middleware.GetUserClaims(r)
	if user.CompanyID.String() != adminClaims.CompanyID {
		RespondWithError(w, ErrCodeForbidden, "No tienes permiso para modificar este operador", http.StatusForbidden)
		return
	}

	if err := h.userRepo.UpdateStatus(ctx, id, req.Status); err != nil {
		logger.Log.Error().Err(err).Msg("Error updating operator status")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Estado de operador actualizado exitosamente",
	})
}

type UpdatePasswordRequest struct {
	Password string `json:"password" validate:"required,min=8,max=72"`
}

func (h *AdminHandler) UpdateOperatorPassword(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de operador inválido", http.StatusBadRequest)
		return
	}

	var req UpdatePasswordRequest
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

	// Check if user exists and belongs to the same company
	user, err := h.userRepo.GetByID(ctx, id)
	if err != nil || user == nil {
		RespondWithError(w, ErrCodeNotFound, "Operador no encontrado", http.StatusNotFound)
		return
	}

	adminClaims := middleware.GetUserClaims(r)
	if user.CompanyID.String() != adminClaims.CompanyID {
		RespondWithError(w, ErrCodeForbidden, "No tienes permiso para modificar este operador", http.StatusForbidden)
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error hashing password")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if err := h.userRepo.UpdatePassword(ctx, id, string(hash)); err != nil {
		logger.Log.Error().Err(err).Msg("Error updating operator password")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Contraseña de operador actualizada exitosamente",
	})
}

func (h *AdminHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Get("/operadores", h.GetOperators)
	r.Post("/operadores", h.CreateOperator)
	r.Put("/operadores/{id}/status", h.UpdateOperatorStatus)
	r.Put("/operadores/{id}/password", h.UpdateOperatorPassword)
	return r
}
