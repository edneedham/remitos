package config

import (
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	DBHost     string
	DBPort     int
	DBUser     string
	DBPassword string
	DBName     string
	DBSSLMode  string
	JWTSecret  string

	// Mercado Pago (server-side). Public key is only for the website (NEXT_PUBLIC_*).
	MercadoPagoAccessToken string
	// Comma-separated origins for browser signup (e.g. http://localhost:3000).
	CorsAllowedOrigins []string
	// If true, signup accepts trial without a real card token (development only).
	SignupAllowMockPayment bool

	// Shared secret for POST /internal/billing/trigger-renewal (header X-Billing-Secret). Empty disables the route.
	BillingRenewalSecret string
	// If true, renewal charges succeed without calling Mercado Pago (local/dev only).
	BillingStubAutoCharge bool
	// Fallback ARS per 1 USD when the MEP (bolsa) quote cannot be fetched. Primary rate is live MEP.
	BillingUSDToARSRate float64
	// Optional override URL for ArgentinaDatos bolsa JSON (default: api.argentinadatos.com .../dolares/bolsa).
	BillingMEPBolsaURL string
	// If true, inserts deterministic local dev users/companies from db/seed/local_dev_users.sql
	// after migrations (development / local testing only; keep false in production).
	SeedLocalDevUsers bool

	// Optional: GCS bucket and object path for signed Android APK downloads (same credentials as images).
	GCSReleasesBucket    string
	AndroidReleaseObject string
	// Signed URL TTL for APK GET links (browser initiates download shortly after request).
	ReleasesSignedURLExpiry time.Duration

	// Transactional email (Resend).
	EmailEnabled  bool
	ResendAPIKey  string
	EmailFrom     string
	EmailReplyTo  string
	PublicSiteURL string // optional; used for links in welcome emails (no trailing slash)
}

func Load() *Config {
	return &Config{
		DBHost:     getEnv("DB_HOST", "localhost"),
		DBPort:     getEnvAsInt("DB_PORT", 5432),
		DBUser:     getEnv("DB_USER", "postgres"),
		DBPassword: getEnv("DB_PASSWORD", "postgres"),
		DBName:     getEnv("DB_NAME", "server"),
		DBSSLMode:  getEnv("DB_SSLMODE", "disable"),
		JWTSecret:  getEnv("JWT_SECRET", "change-me-in-production"),

		MercadoPagoAccessToken: getEnv("MERCADOPAGO_ACCESS_TOKEN", ""),
		CorsAllowedOrigins:     splitCommaTrim(getEnv("CORS_ALLOWED_ORIGINS", "")),
		SignupAllowMockPayment: getEnv("SIGNUP_ALLOW_MOCK_PAYMENT", "") == "true",
		BillingRenewalSecret:   strings.TrimSpace(getEnv("BILLING_RENEWAL_SECRET", "")),
		BillingStubAutoCharge:  getEnv("BILLING_STUB_AUTO_CHARGE", "") == "true",
		BillingUSDToARSRate:    getEnvAsFloat64("BILLING_USD_ARS_RATE", 0),
		BillingMEPBolsaURL:     strings.TrimSpace(getEnv("BILLING_MEP_BOLSA_URL", "")),
		SeedLocalDevUsers:      getEnv("SEED_LOCAL_DEV_USERS", "") == "true",

		GCSReleasesBucket:       getEnv("GCS_RELEASES_BUCKET", ""),
		AndroidReleaseObject:    getEnv("ANDROID_RELEASE_OBJECT", ""),
		ReleasesSignedURLExpiry: time.Duration(getEnvAsInt("GCS_RELEASES_SIGNED_URL_MINUTES", 15)) * time.Minute,

		EmailEnabled:  getEnv("EMAIL_ENABLED", "") == "true",
		ResendAPIKey:  getEnv("RESEND_API_KEY", ""),
		EmailFrom:     getEnv("EMAIL_FROM", ""),
		EmailReplyTo:  getEnv("EMAIL_REPLY_TO", ""),
		PublicSiteURL: strings.TrimRight(strings.TrimSpace(getEnv("PUBLIC_SITE_URL", "")), "/"),
	}
}

func splitCommaTrim(s string) []string {
	parts := strings.Split(s, ",")
	var out []string
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}

func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}

func getEnvAsInt(key string, defaultValue int) int {
	if value, exists := os.LookupEnv(key); exists {
		if intValue, err := strconv.Atoi(value); err == nil {
			return intValue
		}
	}
	return defaultValue
}

func getEnvAsFloat64(key string, defaultValue float64) float64 {
	if value, exists := os.LookupEnv(key); exists {
		if f, err := strconv.ParseFloat(strings.TrimSpace(value), 64); err == nil {
			return f
		}
	}
	return defaultValue
}
