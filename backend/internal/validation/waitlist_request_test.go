package validation

import (
	"strings"
	"testing"

	"server/internal/models"
)

func TestWaitlistRequest_Valid(t *testing.T) {
	t.Parallel()
	wc := 3
	req := models.WaitlistRequest{
		Email:                   "a@b.com",
		FullName:                "Ana",
		CompanyName:             "ACME SRL",
		DeliveryNotesPerDayBand: "lt50",
		ProcessingMode:          "manual",
		WarehouseCount:          &wc,
	}
	NormalizeWaitlistRequest(&req)
	if fields := StructFieldErrors(req); len(fields) > 0 {
		t.Fatalf("unexpected errors: %v", fields)
	}
}

func TestWaitlistRequest_MissingWarehouseCount(t *testing.T) {
	t.Parallel()
	req := models.WaitlistRequest{
		Email:                   "a@b.com",
		FullName:                "Ana",
		CompanyName:             "ACME SRL",
		DeliveryNotesPerDayBand: "lt10",
		ProcessingMode:          "digital",
		DigitalApplication:      "SAP",
		WarehouseCount:          nil,
	}
	NormalizeWaitlistRequest(&req)
	fields := StructFieldErrors(req)
	if fields["warehouse_count"] == "" {
		t.Fatalf("expected warehouse_count error, got %v", fields)
	}
}

func TestWaitlistRequest_InvalidBand(t *testing.T) {
	t.Parallel()
	wc := 1
	req := models.WaitlistRequest{
		Email:                   "a@b.com",
		FullName:                "Ana",
		CompanyName:             "ACME SRL",
		DeliveryNotesPerDayBand: "nope",
		ProcessingMode:          "manual",
		WarehouseCount:          &wc,
	}
	NormalizeWaitlistRequest(&req)
	fields := StructFieldErrors(req)
	if fields["delivery_notes_per_day_band"] == "" {
		t.Fatalf("expected band error, got %v", fields)
	}
}

func TestWaitlistRequest_MissingFullName(t *testing.T) {
	t.Parallel()
	wc := 1
	req := models.WaitlistRequest{
		Email:                   "a@b.com",
		CompanyName:             "ACME SRL",
		DeliveryNotesPerDayBand: "lt10",
		ProcessingMode:          "manual",
		WarehouseCount:          &wc,
	}
	NormalizeWaitlistRequest(&req)
	fields := StructFieldErrors(req)
	if fields["full_name"] == "" {
		t.Fatalf("expected full_name error, got %v", fields)
	}
}

func TestWaitlistRequest_MissingCompanyName(t *testing.T) {
	t.Parallel()
	wc := 1
	req := models.WaitlistRequest{
		Email:                   "a@b.com",
		FullName:                "Ana",
		DeliveryNotesPerDayBand: "lt10",
		ProcessingMode:          "manual",
		WarehouseCount:          &wc,
	}
	NormalizeWaitlistRequest(&req)
	fields := StructFieldErrors(req)
	if fields["company_name"] == "" {
		t.Fatalf("expected company_name error, got %v", fields)
	}
}

func TestWaitlistRequest_EmailTooLong(t *testing.T) {
	t.Parallel()
	wc := 1
	longLocal := strings.Repeat("a", 250)
	req := models.WaitlistRequest{
		Email:                   longLocal + "@b.co",
		FullName:                "Ana",
		CompanyName:             "ACME SRL",
		DeliveryNotesPerDayBand: "lt10",
		ProcessingMode:          "manual",
		WarehouseCount:          &wc,
	}
	NormalizeWaitlistRequest(&req)
	fields := StructFieldErrors(req)
	if fields["email"] == "" {
		t.Fatalf("expected email error, got %v", fields)
	}
}

func TestWaitlistRequest_LogisticsPainPointsTooLong(t *testing.T) {
	t.Parallel()
	wc := 1
	req := models.WaitlistRequest{
		Email:                   "a@b.com",
		FullName:                "Ana",
		CompanyName:             "ACME SRL",
		DeliveryNotesPerDayBand: "lt10",
		ProcessingMode:          "manual",
		LogisticsPainPoints:     strings.Repeat("x", 2001),
		WarehouseCount:          &wc,
	}
	NormalizeWaitlistRequest(&req)
	fields := StructFieldErrors(req)
	if fields["logistics_pain_points"] == "" {
		t.Fatalf("expected logistics_pain_points error, got %v", fields)
	}
}
