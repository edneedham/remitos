package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/models"
)

type contextKey string

const UserContextKey contextKey = "user"

type UserClaims struct {
	UserID    string
	CompanyID string
	Role      string
	DeviceID  string
}

type DeviceRepository interface {
	GetByID(ctx context.Context, deviceID uuid.UUID) (*models.Device, error)
}

type UserWarehouseRepository interface {
	HasWarehouseAccess(ctx context.Context, userID, warehouseID uuid.UUID) (bool, error)
}

type AuthDeps struct {
	JwtSvc            *jwt.Service
	DeviceRepo        DeviceRepository
	UserWarehouseRepo UserWarehouseRepository
}

func Auth(deps AuthDeps) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" {
				http.Error(w, "Unauthorized", http.StatusUnauthorized)
				return
			}

			tokenString := strings.TrimPrefix(authHeader, "Bearer ")
			if tokenString == authHeader {
				http.Error(w, "Unauthorized", http.StatusUnauthorized)
				return
			}

			claims, err := deps.JwtSvc.ValidateToken(tokenString)
			if err != nil {
				http.Error(w, "Unauthorized", http.StatusUnauthorized)
				return
			}

			userClaims := UserClaims{
				UserID: claims.UserID.String(),
				Role:   claims.Role,
			}
			userClaims.CompanyID = claims.CompanyID.String()

			deviceIDHeader := r.Header.Get("X-Device-ID")
			if deviceIDHeader != "" {
				deviceID, err := uuid.Parse(deviceIDHeader)
				if err != nil {
					logger.Log.Warn().Str("device_id", deviceIDHeader).Msg("Invalid device ID format")
				} else {
					ctx := r.Context()
					device, err := deps.DeviceRepo.GetByID(ctx, deviceID)
					if err != nil {
						logger.Log.Error().Err(err).Msg("Error fetching device")
					} else if device != nil {
						if device.CompanyID != claims.CompanyID {
							logger.Log.Warn().
								Str("user_id", claims.UserID.String()).
								Str("device_id", deviceID.String()).
								Str("user_company", claims.CompanyID.String()).
								Str("device_company", device.CompanyID.String()).
								Msg("Device company mismatch")
							http.Error(w, "Dispositivo no pertenece a la empresa", http.StatusForbidden)
							return
						}

						// Check warehouse access if user warehouse repo is provided
						if deps.UserWarehouseRepo != nil {
							hasAccess, err := deps.UserWarehouseRepo.HasWarehouseAccess(ctx, claims.UserID, device.WarehouseID)
							if err != nil {
								logger.Log.Error().Err(err).Msg("Error checking warehouse access")
							} else if !hasAccess {
								logger.Log.Warn().
									Str("user_id", claims.UserID.String()).
									Str("device_id", deviceID.String()).
									Str("warehouse_id", device.WarehouseID.String()).
									Msg("User does not have access to device warehouse")
								http.Error(w, "No tienes acceso a este depósito", http.StatusForbidden)
								return
							}
						}

						userClaims.DeviceID = deviceID.String()
						logger.Log.Info().
							Str("user_id", claims.UserID.String()).
							Str("device_id", deviceID.String()).
							Msg("Device validated for request")
					}
				}
			}

			ctx := context.WithValue(r.Context(), UserContextKey, userClaims)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func RequireRole(role string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			userClaims, ok := r.Context().Value(UserContextKey).(UserClaims)
			if !ok {
				http.Error(w, "Unauthorized", http.StatusUnauthorized)
				return
			}

			if userClaims.Role != role {
				http.Error(w, "Forbidden", http.StatusForbidden)
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}

func GetUserClaims(r *http.Request) UserClaims {
	userClaims, _ := r.Context().Value(UserContextKey).(UserClaims)
	return userClaims
}

func AuthRouter(jwtSvc *jwt.Service, deviceRepo DeviceRepository, userWarehouseRepo UserWarehouseRepository) *chi.Mux {
	r := chi.NewRouter()
	r.Group(func(r chi.Router) {
		r.Use(Auth(AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo, UserWarehouseRepo: userWarehouseRepo}))
	})
	return r
}
