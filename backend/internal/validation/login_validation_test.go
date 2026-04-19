package validation

import (
	"strings"
	"testing"

	"server/internal/models"
)

func TestNormalizeLoginRequest(t *testing.T) {
	req := models.LoginRequest{
		CompanyCode: " ab_cd-1 ",
		Username:    "  user@example.com ",
		Password:    "  secret  ",
		DeviceName:  " web ",
	}
	NormalizeLoginRequest(&req)
	if req.CompanyCode != "AB_CD-1" {
		t.Errorf("company code: got %q", req.CompanyCode)
	}
	if req.Username != "user@example.com" {
		t.Errorf("username: got %q", req.Username)
	}
	if req.Password != "  secret  " {
		t.Errorf("password must not be trimmed, got %q", req.Password)
	}
	if req.DeviceName != "web" {
		t.Errorf("device name: got %q", req.DeviceName)
	}
}

func TestLogin_StructValidAfterNormalize(t *testing.T) {
	req := models.LoginRequest{
		CompanyCode: " emp01 ",
		Username:    " owner@example.com ",
		Password:    "password123",
		DeviceName:  "",
	}
	NormalizeLoginRequest(&req)
	errs := Struct(req)
	if errs != nil {
		t.Fatalf("expected valid, got %v", errs)
	}
}

func TestLogin_WhitespaceOnlyUsernameFails(t *testing.T) {
	req := models.LoginRequest{
		CompanyCode: "EMP01",
		Username:    "   ",
		Password:    "password123",
	}
	NormalizeLoginRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
	found := false
	for _, e := range errs {
		if strings.Contains(e, "username") && strings.Contains(e, "requerido") {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("expected username required error, got %v", errs)
	}
}

func TestLogin_CompanyCodeInvalidCharacters(t *testing.T) {
	req := models.LoginRequest{
		CompanyCode: "BAD*CODE",
		Username:    "user@example.com",
		Password:    "password123",
	}
	NormalizeLoginRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
	found := false
	for _, e := range errs {
		if strings.Contains(e, "Usá solo letras") {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("expected company_code_chars error, got %v", errs)
	}
}

func TestLogin_CompanyCodeTooShort(t *testing.T) {
	req := models.LoginRequest{
		CompanyCode: "A",
		Username:    "user@example.com",
		Password:    "password123",
	}
	NormalizeLoginRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
}
