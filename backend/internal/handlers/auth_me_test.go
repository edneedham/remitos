package handlers

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"server/internal/middleware"
)

func TestGetMe_MissingClaims_Unauthorized(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me", nil)
	res := httptest.NewRecorder()

	handler.GetMe(res, req)

	if res.Code != http.StatusUnauthorized {
		t.Fatalf("expected status %d, got %d", http.StatusUnauthorized, res.Code)
	}
}

func TestGetMe_InvalidUserID_BadRequest(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me", nil)
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "not-a-uuid",
		CompanyID: "b78e7497-68ca-4ad4-8355-2ef7548acb7b",
		Role:      "company_owner",
	}))
	res := httptest.NewRecorder()

	handler.GetMe(res, req)

	if res.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, res.Code)
	}
}

func TestGetMe_InvalidCompanyID_BadRequest(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me", nil)
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "eaa53d7b-1cac-42ca-a2f1-72349f72b516",
		CompanyID: "not-a-uuid",
		Role:      "company_owner",
	}))
	res := httptest.NewRecorder()

	handler.GetMe(res, req)

	if res.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, res.Code)
	}
}

func TestGetMe_OperatorRole_Forbidden(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me", nil)
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "eaa53d7b-1cac-42ca-a2f1-72349f72b516",
		CompanyID: "b78e7497-68ca-4ad4-8355-2ef7548acb7b",
		Role:      "operator",
	}))
	res := httptest.NewRecorder()

	handler.GetMe(res, req)

	if res.Code != http.StatusForbidden {
		t.Fatalf("expected status %d, got %d", http.StatusForbidden, res.Code)
	}
}
