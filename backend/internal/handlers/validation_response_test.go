package handlers

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestRespondWithValidationError_JSONShape(t *testing.T) {
	w := httptest.NewRecorder()
	fields := map[string]string{
		"email": "email debe ser un email válido",
	}
	RespondWithValidationError(w, "Revisá los datos.", fields, http.StatusBadRequest)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("status: got %d", w.Code)
	}
	var body struct {
		Error   string            `json:"error"`
		Message string            `json:"message"`
		Fields  map[string]string `json:"fields"`
	}
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if body.Error != string(ErrCodeInvalidRequest) {
		t.Errorf("error code: got %q", body.Error)
	}
	if body.Message != "Revisá los datos." {
		t.Errorf("message: got %q", body.Message)
	}
	if body.Fields == nil || body.Fields["email"] == "" {
		t.Errorf("fields: got %+v", body.Fields)
	}
}
