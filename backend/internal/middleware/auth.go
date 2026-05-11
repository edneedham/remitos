package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/apierror"
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
			var tokenString string
			if authHeader := r.Header.Get("Authorization"); authHeader != "" {
				if t := strings.TrimPrefix(authHeader, "Bearer "); t != authHeader {
					tokenString = strings.TrimSpace(t)
				}
			}
			if tokenString == "" {
				if c, err := r.Cookie(CookieWebAccess); err == nil {
					tokenString = strings.TrimSpace(c.Value)
				}
			}
			if tokenString == "" {
				apierror.Write(w, http.StatusUnauthorized, string(apierror.Unauthorized), "Unauthorized", nil)
				return
			}

			claims, err := deps.JwtSvc.ValidateToken(tokenString)
			if err != nil {
				logger.Log.Debug().
					Err(err).
					Str("request_id", GetRequestID(r)).
					Str("client_ip", ClientIP(r)).
					Msg("auth: invalid or expired JWT")
				apierror.Write(w, http.StatusUnauthorized, string(apierror.Unauthorized), "Unauthorized", nil)
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
				} else if tryAuthPass(claims.UserID, deviceID, claims.CompanyID) {
					userClaims.DeviceID = deviceID.String()
				} else {
					ctx := r.Context()
					var device *models.Device
					if cid, wid, ok := tryDeviceSnapshot(deviceID); ok {
						device = &models.Device{
							ID:          deviceID,
							CompanyID:   cid,
							WarehouseID: wid,
						}
					} else if deps.DeviceRepo != nil {
						var errFetch error
						device, errFetch = deps.DeviceRepo.GetByID(ctx, deviceID)
						if errFetch != nil {
							logger.Log.Error().Err(errFetch).Msg("Error fetching device")
						} else if device != nil {
							recordDeviceSnapshot(device.ID, device.CompanyID, device.WarehouseID)
						}
					}

					if device != nil {
						if device.CompanyID != claims.CompanyID {
							logger.Log.Warn().
								Str("user_id", claims.UserID.String()).
								Str("device_id", deviceID.String()).
								Str("user_company", claims.CompanyID.String()).
								Str("device_company", device.CompanyID.String()).
								Msg("Device company mismatch")
							apierror.Write(w, http.StatusForbidden, string(apierror.Forbidden), "Dispositivo no pertenece a la empresa", nil)
							return
						}

						if deps.UserWarehouseRepo != nil {
							skipWarehouseACL := claims.Role == models.RoleCompanyOwner || claims.Role == models.RoleWarehouseAdmin
							if !skipWarehouseACL {
								hasAccess, errAccess := deps.UserWarehouseRepo.HasWarehouseAccess(ctx, claims.UserID, device.WarehouseID)
								if errAccess != nil {
									logger.Log.Error().Err(errAccess).Msg("Error checking warehouse access")
								} else if !hasAccess {
									logger.Log.Warn().
										Str("user_id", claims.UserID.String()).
										Str("device_id", deviceID.String()).
										Str("warehouse_id", device.WarehouseID.String()).
										Msg("User does not have access to device warehouse")
									apierror.Write(w, http.StatusForbidden, string(apierror.Forbidden), "No tienes acceso a este depósito", nil)
									return
								}
							}
						}

						userClaims.DeviceID = deviceID.String()
						recordAuthPass(claims.UserID, deviceID, claims.CompanyID)
						logger.Log.Debug().
							Str("user_id", claims.UserID.String()).
							Str("device_id", deviceID.String()).
							Msg("device validated")
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
				apierror.Write(w, http.StatusUnauthorized, string(apierror.Unauthorized), "Unauthorized", nil)
				return
			}

			if userClaims.Role != role {
				apierror.Write(w, http.StatusForbidden, string(apierror.Forbidden), "Forbidden", nil)
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}

// RequireRoles allows any of the given JWT role strings (e.g. company_owner, warehouse_admin).
func RequireRoles(roles ...string) func(http.Handler) http.Handler {
	allowed := make(map[string]struct{}, len(roles))
	for _, role := range roles {
		allowed[role] = struct{}{}
	}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			userClaims, ok := r.Context().Value(UserContextKey).(UserClaims)
			if !ok {
				apierror.Write(w, http.StatusUnauthorized, string(apierror.Unauthorized), "Unauthorized", nil)
				return
			}
			if _, ok := allowed[userClaims.Role]; !ok {
				apierror.Write(w, http.StatusForbidden, string(apierror.Forbidden), "Forbidden", nil)
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
