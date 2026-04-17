package config

import (
	"os"
	"strconv"
	"strings"
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
