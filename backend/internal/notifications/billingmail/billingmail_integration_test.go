package billingmail

import (
	"context"
	"os"
	"sync"
	"testing"

	notifymail "server/internal/notifications/email"
	"server/internal/repository"
	"server/internal/testutil/integration"
	"server/internal/logger"
)

func TestMain(m *testing.M) {
	logger.Init("info")
	os.Exit(m.Run())
}

type recordingSender struct {
	mu  sync.Mutex
	out []notifymail.Message
}

func (r *recordingSender) Send(ctx context.Context, m notifymail.Message) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.out = append(r.out, m)
	return nil
}

func (r *recordingSender) len() int {
	r.mu.Lock()
	defer r.mu.Unlock()
	return len(r.out)
}

func TestSendPaymentReceipt_Idempotent_Integration(t *testing.T) {
	dsn := integration.PostgresDSN(t)
	ctx := context.Background()
	pool := integration.NewPool(t, dsn)
	integration.TruncateTenantData(t, pool)

	_, invID, _ := integration.CompanyOwnerAndPaidInvoice(ctx, t, pool)

	invRepo := repository.NewInvoiceRepository(pool)
	users := repository.NewUserRepository(pool)
	companies := repository.NewCompanyRepository(pool)
	mail := &recordingSender{}
	legal := "Nota legal de integración."

	sendPaymentReceipt(ctx, invRepo, users, companies, mail, "https://example.com", legal, invID)
	integration.SleepBrief()
	if mail.len() != 1 {
		t.Fatalf("first send: want 1 message, got %d", mail.len())
	}

	inv, err := invRepo.GetByID(ctx, invID)
	if err != nil || inv == nil || !inv.ReceiptEmailSentAt.Valid {
		t.Fatalf("expected receipt_email_sent_at set, inv=%v err=%v", inv, err)
	}

	sendPaymentReceipt(ctx, invRepo, users, companies, mail, "https://example.com", legal, invID)
	integration.SleepBrief()
	if mail.len() != 1 {
		t.Fatalf("second send: want still 1 message (idempotent), got %d", mail.len())
	}
}

func TestSetReceiptEmailSentIfUnset_Integration(t *testing.T) {
	dsn := integration.PostgresDSN(t)
	ctx := context.Background()
	pool := integration.NewPool(t, dsn)
	integration.TruncateTenantData(t, pool)

	_, invID, _ := integration.CompanyOwnerAndPaidInvoice(ctx, t, pool)
	invRepo := repository.NewInvoiceRepository(pool)

	ok, err := invRepo.SetReceiptEmailSentIfUnset(ctx, invID)
	if err != nil || !ok {
		t.Fatalf("first set: ok=%v err=%v", ok, err)
	}
	ok2, err := invRepo.SetReceiptEmailSentIfUnset(ctx, invID)
	if err != nil || ok2 {
		t.Fatalf("second set: want ok=false got=%v err=%v", ok2, err)
	}
}

func TestSendRenewalChargeFailure_OncePerInvoice_Integration(t *testing.T) {
	dsn := integration.PostgresDSN(t)
	ctx := context.Background()
	pool := integration.NewPool(t, dsn)

	companyID, invID, _ := integration.PendingRenewalInvoice(ctx, t, pool)

	invRepo := repository.NewInvoiceRepository(pool)
	users := repository.NewUserRepository(pool)
	companies := repository.NewCompanyRepository(pool)
	mail := &recordingSender{}

	sendRenewalChargeFailure(ctx, invRepo, users, companies, mail, "https://example.com",
		companyID, invID, 1500000, "ARS", "test rejection")
	integration.SleepBrief()
	if mail.len() != 1 {
		t.Fatalf("first failure mail: want 1 got %d", mail.len())
	}

	sendRenewalChargeFailure(ctx, invRepo, users, companies, mail, "https://example.com",
		companyID, invID, 1500000, "ARS", "test rejection")
	integration.SleepBrief()
	if mail.len() != 1 {
		t.Fatalf("second failure mail: want 1 (idempotent) got %d", mail.len())
	}
}

func TestSendRenewalChargeFailure_SkipsWhenInvoicePaid_Integration(t *testing.T) {
	dsn := integration.PostgresDSN(t)
	ctx := context.Background()
	pool := integration.NewPool(t, dsn)

	_, invID, _ := integration.CompanyOwnerAndPaidInvoice(ctx, t, pool)
	invRepo := repository.NewInvoiceRepository(pool)
	users := repository.NewUserRepository(pool)
	companies := repository.NewCompanyRepository(pool)
	mail := &recordingSender{}

	inv, err := invRepo.GetByID(ctx, invID)
	if err != nil || inv == nil {
		t.Fatal(err)
	}
	sendRenewalChargeFailure(ctx, invRepo, users, companies, mail, "https://example.com",
		inv.CompanyID, invID, 100, "ARS", "x")
	integration.SleepBrief()
	if mail.len() != 0 {
		t.Fatalf("paid invoice should not get failure email, got %d", mail.len())
	}
}
