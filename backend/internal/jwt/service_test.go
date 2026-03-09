package jwt

import (
	"testing"
	"time"

	"github.com/google/uuid"
)

func TestGenerateAndValidateToken(t *testing.T) {
	svc := NewService("test-secret")
	userID := uuid.New()
	companyID := uuid.New()

	token, err := svc.GenerateToken(userID, companyID, "admin", time.Hour)
	if err != nil {
		t.Fatalf("failed to generate token: %v", err)
	}

	claims, err := svc.ValidateToken(token)
	if err != nil {
		t.Fatalf("failed to validate token: %v", err)
	}

	if claims.UserID != userID {
		t.Errorf("expected user_id %s, got %s", userID, claims.UserID)
	}

	if claims.CompanyID != companyID {
		t.Errorf("expected company_id %s, got %s", companyID, claims.CompanyID)
	}

	if claims.Role != "admin" {
		t.Errorf("expected role admin, got %s", claims.Role)
	}
}

func TestValidateToken_Invalid(t *testing.T) {
	svc := NewService("test-secret")

	_, err := svc.ValidateToken("invalid-token")
	if err != ErrInvalidToken {
		t.Errorf("expected ErrInvalidToken, got %v", err)
	}
}

func TestValidateToken_WrongSecret(t *testing.T) {
	svc1 := NewService("secret-1")
	svc2 := NewService("secret-2")

	token, _ := svc1.GenerateToken(uuid.New(), uuid.New(), "user", time.Hour)

	_, err := svc2.ValidateToken(token)
	if err != ErrInvalidToken {
		t.Errorf("expected ErrInvalidToken, got %v", err)
	}
}
