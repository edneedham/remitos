package handlers

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"sync"
	"testing"

	notifymail "server/internal/notifications/email"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
	"server/internal/testutil/integration"
	"server/internal/logger"
)

func TestMain(m *testing.M) {
	logger.Init("info")
	os.Exit(m.Run())
}

type webhookRecordingSender struct {
	mu  sync.Mutex
	out []notifymail.Message
}

func (r *webhookRecordingSender) Send(ctx context.Context, m notifymail.Message) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.out = append(r.out, m)
	return nil
}

func (r *webhookRecordingSender) len() int {
	r.mu.Lock()
	defer r.mu.Unlock()
	return len(r.out)
}

func TestHandlePayment_FirstApprovedPayment_SendsReceipt_Integration(t *testing.T) {
	dsn := integration.PostgresDSN(t)
	ctx := context.Background()
	pool := integration.NewPool(t, dsn)
	companyID, _ := integration.CompanyAndOwner(ctx, t, pool)

	const mpID = "pay_integration_webhook_001"
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/payments/"+mpID {
			http.NotFound(w, r)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		payload := map[string]any{
			"id":                   mpID,
			"status":               "approved",
			"currency_id":          "ARS",
			"transaction_amount":   100.0,
			"metadata":             map[string]any{"company_id": companyID.String()},
			"external_reference":   "",
		}
		_ = json.NewEncoder(w).Encode(payload)
	}))
	t.Cleanup(srv.Close)

	mp := mercadopago.New("test-access-token")
	mp.APIBase = srv.URL

	invRepo := repository.NewInvoiceRepository(pool)
	companyRepo := repository.NewCompanyRepository(pool)
	userRepo := repository.NewUserRepository(pool)
	mail := &webhookRecordingSender{}
	h := NewMercadoPagoWebhookHandler(
		pool, invRepo, companyRepo, userRepo, mp, mail,
		"https://site.example", 0.07,
		"",
		nil,
		nil,
	)

	if err := h.handlePayment(ctx, mpID); err != nil {
		t.Fatal(err)
	}
	integration.SleepBrief()
	if mail.len() != 1 {
		t.Fatalf("expected 1 receipt email, got %d", mail.len())
	}
}

func TestHandlePayment_DuplicatePaymentID_NoSecondReceipt_Integration(t *testing.T) {
	dsn := integration.PostgresDSN(t)
	ctx := context.Background()
	pool := integration.NewPool(t, dsn)
	companyID, _ := integration.CompanyAndOwner(ctx, t, pool)

	const mpID = "pay_integration_webhook_dup"
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		payload := map[string]any{
			"id":                 mpID,
			"status":             "approved",
			"currency_id":        "ARS",
			"transaction_amount": 50.0,
			"metadata":           map[string]any{"company_id": companyID.String()},
		}
		_ = json.NewEncoder(w).Encode(payload)
	}))
	t.Cleanup(srv.Close)

	mp := mercadopago.New("test-access-token")
	mp.APIBase = srv.URL

	invRepo := repository.NewInvoiceRepository(pool)
	companyRepo := repository.NewCompanyRepository(pool)
	userRepo := repository.NewUserRepository(pool)
	mail := &webhookRecordingSender{}
	h := NewMercadoPagoWebhookHandler(
		pool, invRepo, companyRepo, userRepo, mp, mail,
		"https://site.example", 0.07,
		"",
		nil,
		nil,
	)

	if err := h.handlePayment(ctx, mpID); err != nil {
		t.Fatal(err)
	}
	integration.SleepBrief()
	if err := h.handlePayment(ctx, mpID); err != nil {
		t.Fatal(err)
	}
	integration.SleepBrief()
	if mail.len() != 1 {
		t.Fatalf("duplicate webhook should not send twice, got %d messages", mail.len())
	}
}
