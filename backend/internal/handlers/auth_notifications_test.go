package handlers

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/middleware"
)

func TestGetMeNotifications_Unauthorized(t *testing.T) {
	h := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me/notifications", nil)
	res := httptest.NewRecorder()

	h.GetMeNotifications(res, req)

	if res.Code != http.StatusUnauthorized {
		t.Fatalf("expected status %d, got %d", http.StatusUnauthorized, res.Code)
	}
}

func TestGetMeNotifications_OperatorForbidden(t *testing.T) {
	h := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me/notifications", nil)
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "eaa53d7b-1cac-42ca-a2f1-72349f72b516",
		CompanyID: "b78e7497-68ca-4ad4-8355-2ef7548acb7b",
		Role:      "operator",
	}))
	res := httptest.NewRecorder()

	h.GetMeNotifications(res, req)

	if res.Code != http.StatusForbidden {
		t.Fatalf("expected status %d, got %d", http.StatusForbidden, res.Code)
	}
}

func TestGetMeNotifications_NoRepo_ServiceUnavailable(t *testing.T) {
	h := &AuthHandler{}
	req := httptest.NewRequest(http.MethodGet, "/auth/me/notifications", nil)
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "eaa53d7b-1cac-42ca-a2f1-72349f72b516",
		CompanyID: "b78e7497-68ca-4ad4-8355-2ef7548acb7b",
		Role:      "company_owner",
	}))
	res := httptest.NewRecorder()

	h.GetMeNotifications(res, req)

	if res.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected status %d, got %d", http.StatusServiceUnavailable, res.Code)
	}
}

func TestPatchMeNotificationRead_NoRepo_ServiceUnavailable(t *testing.T) {
	h := &AuthHandler{}
	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("notificationID", uuid.New().String())
	req := httptest.NewRequest(http.MethodPatch, "/", nil)
	req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "eaa53d7b-1cac-42ca-a2f1-72349f72b516",
		CompanyID: "b78e7497-68ca-4ad4-8355-2ef7548acb7b",
		Role:      "company_owner",
	}))
	res := httptest.NewRecorder()

	h.PatchMeNotificationRead(res, req)

	if res.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected status %d, got %d", http.StatusServiceUnavailable, res.Code)
	}
}
