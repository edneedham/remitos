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
	// DBPoolMaxConns caps PostgreSQL pool size (pgx). Zero means derive from CPU count (4–32).
	DBPoolMaxConns int
	JWTSecret      string

	// Mercado Pago (server-side). Public key is only for the website (NEXT_PUBLIC_*).
	MercadoPagoAccessToken string
	// Webhook signing secret from Mercado Pago "Your integrations" (validates x-signature on POST notifications).
	MercadoPagoWebhookSecret string
	// Comma-separated origins for browser signup (e.g. http://localhost:3000).
	CorsAllowedOrigins []string
	// If true, signup accepts trial without a real card token (development only).
	SignupAllowMockPayment bool

	// Shared secret for POST /internal/billing/trigger-renewal (header X-Billing-Secret). Empty disables the route.
	BillingRenewalSecret string
	// If true, periodically runs subscription renewals for companies past subscription_expires_at (uses RenewalService; keep false until charges are verified).
	BillingAutomaticRenewalEnabled bool
	// Interval for automatic renewal sweep (default 60 minutes).
	BillingRenewalPollMinutes int
	// If true, renewal charges succeed without calling Mercado Pago (local/dev only).
	BillingStubAutoCharge bool
	// Fallback ARS per 1 USD when the MEP (bolsa) quote cannot be fetched. Primary rate is live MEP.
	BillingUSDToARSRate float64
	// Fraction added on top of the MEP reference (e.g. 0.07 = +7%) before USD→ARS conversion. Default 0.07.
	BillingFXBufferFraction float64
	// Optional override URL for bolsa JSON (default: dolarapi.com/v1/dolares/bolsa).
	BillingMEPBolsaURL string
	// If true, inserts deterministic local dev users/companies from db/seed/local_dev_users.sql
	// after migrations (development / local testing only; keep false in production).
	SeedLocalDevUsers bool

	// Optional: GCS bucket and object path for signed Android APK downloads (same credentials as images).
	GCSReleasesBucket    string
	AndroidReleaseObject string
	// Signed URL TTL for APK GET links (browser initiates download shortly after request).
	ReleasesSignedURLExpiry time.Duration

	// LogLevel is zerolog global level: trace, debug, info, warn, error, fatal, panic, disabled (default: info).
	LogLevel string

	// Transactional email (Resend).
	EmailEnabled  bool
	ResendAPIKey  string
	EmailFrom     string
	EmailReplyTo  string
	PublicSiteURL string // optional; used for links in welcome emails (no trailing slash)

	// AFIP / ARCA direct integration. Empty/disabled by default until cert + emisor data are provisioned.
	// Env: "homo" (default) or "prod". Selects WSAA / WSFEv1 / Padron endpoints.
	AfipEnv string
	// Master switch for emitting factura electrónica after subscription payments.
	AfipBillingEnabled bool
	// Master switch for resolving CUIT padrón data (razón social / condición IVA / domicilio).
	AfipPadronEnabled bool
	// Issuer (Remitos) CUIT, no dashes (e.g. "30715975111").
	AfipCUIT string
	// Punto de venta registered for webservices (FECAESolicitar).
	AfipPuntoVenta int
	// Issuer condición IVA. One of: RESPONSABLE_INSCRIPTO, MONOTRIBUTO, EXENTO. Drives factura tipo selection.
	AfipIssuerCondicionIVA string
	// FECAE Concepto: 1=Productos, 2=Servicios (default), 3=Productos y Servicios.
	AfipConcepto int
	// FECAE alícuota IVA percent for SaaS items (default 21).
	AfipDefaultAlicuotaIVA float64
	// X.509 client certificate / private key sources. PEM env wins over file path; both fall back to GCP Secret Manager refs in prod.
	AfipCertPEM        string
	AfipCertPath       string
	AfipKeyPEM         string
	AfipKeyPath        string
	AfipCertSecretName string // e.g. "projects/<id>/secrets/afip-cert/versions/latest"
	AfipKeySecretName  string
	// Optional override for SOAP endpoints (testing).
	AfipWSAAURL   string
	AfipWSFEv1URL string
	AfipPadronURL string
	// AfipPadronCacheHours: reuse companies.* padron snapshot for WSFE emission without calling
	// getPersona while PadronSyncedAt is newer than this many hours. 0 = always refresh padron.
	AfipPadronCacheHours int
}

func Load() *Config {
	return &Config{
		DBHost:         getEnv("DB_HOST", "localhost"),
		DBPort:         getEnvAsInt("DB_PORT", 5432),
		DBUser:         getEnv("DB_USER", "postgres"),
		DBPassword:     getEnv("DB_PASSWORD", "postgres"),
		DBName:         getEnv("DB_NAME", "server"),
		DBSSLMode:      getEnv("DB_SSLMODE", "disable"),
		DBPoolMaxConns: getEnvAsInt("DB_POOL_MAX_CONNS", 0),
		JWTSecret:      strings.TrimSpace(getEnv("JWT_SECRET", "")),

		MercadoPagoAccessToken:         getEnv("MERCADOPAGO_ACCESS_TOKEN", ""),
		MercadoPagoWebhookSecret:       strings.TrimSpace(getEnv("MERCADOPAGO_WEBHOOK_SECRET", "")),
		CorsAllowedOrigins:             splitCommaTrim(getEnv("CORS_ALLOWED_ORIGINS", "")),
		SignupAllowMockPayment:         getEnv("SIGNUP_ALLOW_MOCK_PAYMENT", "") == "true",
		BillingRenewalSecret:           strings.TrimSpace(getEnv("BILLING_RENEWAL_SECRET", "")),
		BillingAutomaticRenewalEnabled: getEnv("BILLING_AUTOMATIC_RENEWAL_ENABLED", "") == "true",
		BillingRenewalPollMinutes:      getEnvAsInt("BILLING_RENEWAL_POLL_MINUTES", 60),
		BillingStubAutoCharge:          getEnv("BILLING_STUB_AUTO_CHARGE", "") == "true",
		BillingUSDToARSRate:            getEnvAsFloat64("BILLING_USD_ARS_RATE", 0),
		BillingFXBufferFraction:        getEnvAsFloat64("BILLING_FX_BUFFER_FRACTION", 0.07),
		BillingMEPBolsaURL:             strings.TrimSpace(getEnv("BILLING_MEP_BOLSA_URL", "")),
		SeedLocalDevUsers:              getEnv("SEED_LOCAL_DEV_USERS", "") == "true",

		GCSReleasesBucket:       getEnv("GCS_RELEASES_BUCKET", ""),
		AndroidReleaseObject:    getEnv("ANDROID_RELEASE_OBJECT", ""),
		ReleasesSignedURLExpiry: time.Duration(getEnvAsInt("GCS_RELEASES_SIGNED_URL_MINUTES", 15)) * time.Minute,

		LogLevel:      strings.ToLower(strings.TrimSpace(getEnv("LOG_LEVEL", "info"))),
		EmailEnabled:  getEnv("EMAIL_ENABLED", "") == "true",
		ResendAPIKey:  getEnv("RESEND_API_KEY", ""),
		EmailFrom:     getEnv("EMAIL_FROM", ""),
		EmailReplyTo:  getEnv("EMAIL_REPLY_TO", ""),
		PublicSiteURL: strings.TrimRight(strings.TrimSpace(getEnv("PUBLIC_SITE_URL", "")), "/"),

		AfipEnv:                strings.ToLower(strings.TrimSpace(getEnv("AFIP_ENV", "homo"))),
		AfipBillingEnabled:     getEnv("AFIP_BILLING_ENABLED", "") == "true",
		AfipPadronEnabled:      getEnv("AFIP_PADRON_ENABLED", "") == "true",
		AfipCUIT:               digitsOnly(getEnv("AFIP_CUIT", "")),
		AfipPuntoVenta:         getEnvAsInt("AFIP_PUNTO_VENTA", 0),
		AfipIssuerCondicionIVA: strings.ToUpper(strings.TrimSpace(getEnv("AFIP_ISSUER_CONDICION_IVA", "RESPONSABLE_INSCRIPTO"))),
		AfipConcepto:           getEnvAsInt("AFIP_CONCEPTO", 2),
		AfipDefaultAlicuotaIVA: getEnvAsFloat64("AFIP_DEFAULT_ALICUOTA_IVA", 21.0),
		AfipCertPEM:            getEnv("AFIP_CERT_PEM", ""),
		AfipCertPath:           strings.TrimSpace(getEnv("AFIP_CERT_PATH", "")),
		AfipKeyPEM:             getEnv("AFIP_KEY_PEM", ""),
		AfipKeyPath:            strings.TrimSpace(getEnv("AFIP_KEY_PATH", "")),
		AfipCertSecretName:     strings.TrimSpace(getEnv("AFIP_CERT_SECRET_NAME", "")),
		AfipKeySecretName:      strings.TrimSpace(getEnv("AFIP_KEY_SECRET_NAME", "")),
		AfipWSAAURL:            strings.TrimSpace(getEnv("AFIP_WSAA_URL", "")),
		AfipWSFEv1URL:          strings.TrimSpace(getEnv("AFIP_WSFEV1_URL", "")),
		AfipPadronURL:          strings.TrimSpace(getEnv("AFIP_PADRON_URL", "")),
		AfipPadronCacheHours:   getEnvAsInt("AFIP_PADRON_CACHE_HOURS", 168),
	}
}

// digitsOnly strips non-digit characters from a CUIT/CUIL string (handles "30-71597511-1" → "30715975111").
func digitsOnly(s string) string {
	var b strings.Builder
	for _, r := range s {
		if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	return b.String()
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
