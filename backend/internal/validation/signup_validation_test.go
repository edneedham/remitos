package validation

import (
	"strings"
	"testing"

	"server/internal/models"
)

func TestNormalizeSignupRequest(t *testing.T) {
	req := models.SignupRequest{
		Email:       "  User@Example.COM ",
		CompanyName: "  Mi Empresa ",
		CompanyCode: "ab_cd-1",
		CompanyCUIT: " 30-12345678-1 ",
		Password:    "  secret  ",
	}
	NormalizeSignupRequest(&req)
	if req.Email != "user@example.com" {
		t.Errorf("email: got %q", req.Email)
	}
	if req.CompanyName != "Mi Empresa" {
		t.Errorf("company name: got %q", req.CompanyName)
	}
	if req.CompanyCode != "AB_CD-1" {
		t.Errorf("company code: got %q", req.CompanyCode)
	}
	if req.CompanyCUIT != "30123456781" {
		t.Errorf("company cuit digits: got %q", req.CompanyCUIT)
	}
	if req.Password != "  secret  " {
		t.Errorf("password must not be trimmed, got %q", req.Password)
	}
}

func TestSignup_StructValidAfterNormalize(t *testing.T) {
	req := models.SignupRequest{
		Email:       " owner@example.com ",
		Password:    "password123",
		CompanyName: " Empresa SA ",
		CompanyCode: " emp_01 ",
		CompanyCUIT: "30-12345678-1",
	}
	NormalizeSignupRequest(&req)
	errs := Struct(req)
	if errs != nil {
		t.Fatalf("expected valid, got %v", errs)
	}
}

func TestSignup_WhitespaceOnlyCompanyNameFails(t *testing.T) {
	req := models.SignupRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "     ",
		CompanyCode: "EMP01",
		CompanyCUIT: "30123456781",
	}
	NormalizeSignupRequest(&req)
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

func TestSignup_CompanyCodeInvalidCharacters(t *testing.T) {
	req := models.SignupRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "BAD*CODE",
		CompanyCUIT: "30123456781",
	}
	NormalizeSignupRequest(&req)
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

func TestSignup_CompanyCUITInvalidLengthFails(t *testing.T) {
	req := models.SignupRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "EMP01",
		CompanyCUIT: "123456789",
	}
	NormalizeSignupRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
	found := false
	for _, e := range errs {
		if strings.Contains(e, "no es válido") || strings.Contains(e, "verificador") {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("expected cuit_ar error, got %v", errs)
	}
}

func TestSignup_CompanyCUITInvalidChecksumFails(t *testing.T) {
	req := models.SignupRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "EMP01",
		CompanyCUIT: "30123456789",
	}
	NormalizeSignupRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
	found := false
	for _, e := range errs {
		if strings.Contains(e, "verificador") {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("expected checksum error, got %v", errs)
	}
}

func TestSignup_MissingCUITFails(t *testing.T) {
	req := models.SignupRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "EMP01",
		CompanyCUIT: "",
	}
	NormalizeSignupRequest(&req)
	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected validation errors")
	}
	found := false
	for _, e := range errs {
		if strings.Contains(e, "CUIT") && strings.Contains(e, "obligatorio") {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("expected company_cuit required error, got %v", errs)
	}
}

func TestSignup_CompanyCodeAllowsLettersDigitsUnderscoreHyphen(t *testing.T) {
	req := models.SignupRequest{
		Email:       "owner@example.com",
		Password:    "password123",
		CompanyName: "Empresa",
		CompanyCode: "Ab_9-zZ",
		CompanyCUIT: "30123456781",
	}
	NormalizeSignupRequest(&req)
	errs := Struct(req)
	if errs != nil {
		t.Fatalf("expected valid, got %v", errs)
	}
	if req.CompanyCode != "AB_9-ZZ" {
		t.Errorf("company code uppercasing: got %q", req.CompanyCode)
	}
}
