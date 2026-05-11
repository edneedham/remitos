package handlers

import (
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/billing"
	"server/internal/jwt"
	notifymail "server/internal/notifications/email"
	"server/internal/payments/afip"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

// AuthHandlerConfig carries dependencies for AuthHandler (replaces a long constructor argument list).
type AuthHandlerConfig struct {
	UserRepo                *repository.UserRepository
	CompanyRepo             *repository.CompanyRepository
	WarehouseRepo           *repository.WarehouseRepository
	SyncRepo                *repository.SyncRepository
	InvoiceRepo             *repository.InvoiceRepository
	DeviceRepo              *repository.DeviceRepository
	UserWarehouseRepo       *repository.UserWarehouseRepository
	RefreshTokenRepo        *repository.RefreshTokenRepository
	PasswordResetTokenRepo  *repository.PasswordResetTokenRepository
	TransferRepo            *repository.WebSessionTransferRepository
	SubscriptionRepo        *repository.SubscriptionRepository
	NotificationRepo        *repository.UserNotificationRepository
	DB                      *pgxpool.Pool
	JwtSvc                  *jwt.Service
	MercadoPago             *mercadopago.Client
	SignupAllowMockPayment  bool
	Releases                *AuthReleasesConfig
	Mailer                  notifymail.Sender
	PublicSiteURL           string
	BillingRateQuoter       billing.USDARSQuoter
	BillingFXBufferFraction float64
	FacturaEmitter          *billing.FacturaEmitter
	AfipClient              *afip.Client
}

// NewAuthHandlerFromConfig builds an AuthHandler from AuthHandlerConfig.
func NewAuthHandlerFromConfig(c AuthHandlerConfig) *AuthHandler {
	return &AuthHandler{
		userRepo:                c.UserRepo,
		companyRepo:             c.CompanyRepo,
		warehouseRepo:           c.WarehouseRepo,
		syncRepo:                c.SyncRepo,
		invoiceRepo:             c.InvoiceRepo,
		deviceRepo:              c.DeviceRepo,
		userWarehouseRepo:       c.UserWarehouseRepo,
		refreshTokenRepo:        c.RefreshTokenRepo,
		passwordResetTokenRepo:  c.PasswordResetTokenRepo,
		transferRepo:            c.TransferRepo,
		subscriptionRepo:        c.SubscriptionRepo,
		db:                      c.DB,
		jwtSvc:                  c.JwtSvc,
		mp:                      c.MercadoPago,
		signupAllowMock:         c.SignupAllowMockPayment,
		releases:                c.Releases,
		mailer:                  c.Mailer,
		publicSiteURL:           c.PublicSiteURL,
		billingRateQuoter:       c.BillingRateQuoter,
		billingFXBufferFraction: c.BillingFXBufferFraction,
		facturaEmitter:          c.FacturaEmitter,
		afipClient:              c.AfipClient,
		notificationRepo:        c.NotificationRepo,
	}
}
