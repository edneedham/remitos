package validation

import (
	"testing"
)

type TestRequest struct {
	Email    string `json:"email" validate:"required,email"`
	Password string `json:"password" validate:"required,min=8"`
}

func TestStruct_Valid(t *testing.T) {
	req := TestRequest{
		Email:    "test@example.com",
		Password: "password123",
	}

	errs := Struct(req)
	if errs != nil {
		t.Errorf("expected no errors, got %v", errs)
	}
}

func TestStruct_InvalidEmail(t *testing.T) {
	req := TestRequest{
		Email:    "not-an-email",
		Password: "password123",
	}

	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected errors, got nil")
	}

	found := false
	for _, err := range errs {
		if len(err) > 0 {
			found = true
		}
	}
	if !found {
		t.Error("expected email validation error")
	}
}

func TestStruct_MissingRequired(t *testing.T) {
	req := TestRequest{
		Email:    "",
		Password: "",
	}

	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected errors, got nil")
	}

	if len(errs) < 2 {
		t.Errorf("expected at least 2 errors, got %d", len(errs))
	}
}

func TestStruct_PasswordTooShort(t *testing.T) {
	req := TestRequest{
		Email:    "test@example.com",
		Password: "123",
	}

	errs := Struct(req)
	if errs == nil {
		t.Fatal("expected errors, got nil")
	}
}

func TestStructFieldErrors_JSONKeys(t *testing.T) {
	req := TestRequest{
		Email:    "",
		Password: "",
	}
	fields := StructFieldErrors(req)
	if _, ok := fields["email"]; !ok {
		t.Fatalf("expected email key, got %v", fields)
	}
	if _, ok := fields["password"]; !ok {
		t.Fatalf("expected password key, got %v", fields)
	}
	if fields["email"] == "" || fields["password"] == "" {
		t.Fatal("expected non-empty messages")
	}
}
