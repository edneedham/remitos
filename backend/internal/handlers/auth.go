package handlers

import (
	"time"

	"cloud.google.com/go/storage"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/billing"
	"server/internal/jwt"
	notifymail "server/internal/notifications/email"
	"server/internal/payments/afip"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

// AuthReleasesConfig enables GET /auth/downloads/android via GCS signed URLs (optional).
type AuthReleasesConfig struct {
	Storage *storage.Client
	Bucket  string
	Object  string
	Expiry  time.Duration
}

type AuthHandler struct {
	userRepo                *repository.UserRepository
	companyRepo             *repository.CompanyRepository
	warehouseRepo           *repository.WarehouseRepository
	syncRepo                *repository.SyncRepository
	invoiceRepo             *repository.InvoiceRepository
	deviceRepo              *repository.DeviceRepository
	userWarehouseRepo       *repository.UserWarehouseRepository
	refreshTokenRepo        *repository.RefreshTokenRepository
	transferRepo            *repository.WebSessionTransferRepository
	subscriptionRepo        *repository.SubscriptionRepository
	db                      *pgxpool.Pool
	jwtSvc                  *jwt.Service
	mp                      *mercadopago.Client
	signupAllowMock         bool
	releases                *AuthReleasesConfig
	mailer                  notifymail.Sender
	publicSiteURL           string
	billingRateQuoter       billing.USDARSQuoter
	billingFXBufferFraction float64
	passwordResetTokenRepo  *repository.PasswordResetTokenRepository
	facturaEmitter          *billing.FacturaEmitter
	afipClient              *afip.Client
	notificationRepo        *repository.UserNotificationRepository
	waitlistOnly            bool
}

type RegisterDeviceRequest struct {
	DeviceUUID  string  `json:"device_uuid" validate:"required"`
	Platform    string  `json:"platform" validate:"required"`
	WarehouseID string  `json:"warehouse_id" validate:"required"`
	Model       *string `json:"model,omitempty"`
	OSVersion   *string `json:"os_version,omitempty"`
	AppVersion  *string `json:"app_version,omitempty"`
	DeviceName  string  `json:"device_name" validate:"omitempty"`
	Username    string  `json:"username" validate:"omitempty"`
	Password    string  `json:"password" validate:"omitempty"`
}

type DeviceRegistrationResponse struct {
	DeviceID     string `json:"device_id"`
	AccessToken  string `json:"access_token,omitempty"`
	RefreshToken string `json:"refresh_token,omitempty"`
	ExpiresIn    int    `json:"expires_in,omitempty"`
}

type LoginResponse struct {
	Token        string `json:"token,omitempty"`
	RefreshToken string `json:"refresh_token,omitempty"`
	ExpiresIn    int    `json:"expires_in"`
	Role         string `json:"role,omitempty"`
	// Session is "cookie" when tokens were issued only as httpOnly cookies (browser clients).
	Session string `json:"session,omitempty"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token"`
}

type TransferStartRequest struct {
	RefreshToken string `json:"refresh_token"`
}

type TransferClaimRequest struct {
	Token string `json:"token" validate:"required"`
}

type TransferStartResponse struct {
	Token     string `json:"token"`
	ExpiresAt string `json:"expires_at"`
}
