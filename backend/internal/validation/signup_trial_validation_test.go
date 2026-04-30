package validation

import (
	"strings"
	"testing"

	"server/internal/models"
)

func TestNormalizeSignupTrialRequest(t *testing.T) {
	req := models.SignupTrialRequest{
		Email:       "  User@Example.COM ",
		CompanyName: "  Mi Empresa ",
		CompanyCode: "ab_cd-1",
		Password:    "  secret  ",
	}
	NormalizeSignupTrialRequest(&req)
	if req.Email != "user@example.com" {
		t.Errorf("email: got %q", req.Email)
	}
	if req.CompanyName != "Mi Empresa" {
		t.Errorf("company name: got %q", req.CompanyName)
	}
	if req.CompanyCode != "AB_CD-1" {
		t.Errorf("company code: got %q", req.CompanyCode)
	}
	if req.Password != "  secret  " {
		t.Errorf("password must not be trimmed, got %q", req.Password)
	}
}

func TestSignupTrial_StructValidAfterNormalize(t *testing.T) {
	req := models.SignupTrialRequest{
		Email:       " owner@example.com ",
		Password:    "password123",
		CompanyName: " Empresa SA ",
		CompanyCode: " emp_01 ",
	}
	NormalizeSignupTrialRequest(&req)
	errs := Struct(req)
	if errs != nil {
		t.Fatalf("expected valid, got %v", errs)
	}
}

func TestSignupTrial_WhitespaceOnlyCompanyNameFails(t *testing.T) {
	req := models.SignupTrialRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "     ",
		CompanyCode: "EMP01",
	}
	NormalizeSignupTrialRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
	found := false
	for _, e := range errs {
		if strings.Contains(e, "company_name") && strings.Contains(e, "requerido") {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("expected company_name required error, got %v", errs)
	}
}

func TestSignupTrial_CompanyCodeInvalidCharacters(t *testing.T) {
	req := models.SignupTrialRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "BAD*CODE",
	}
	NormalizeSignupTrialRequest(&req)
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

func TestSignupTrial_CompanyCodeAllowsLettersDigitsUnderscoreHyphen(t *testing.T) {
	req := models.SignupTrialRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "Ab_9-zZ",
	}
	NormalizeSignupTrialRequest(&req)
	errs := Struct(req)
	if errs != nil {
		t.Fatalf("expected valid, got %v", errs)
	}
	if req.CompanyCode != "AB_9-ZZ" {
		t.Errorf("company code uppercasing: got %q", req.CompanyCode)
	}
}
