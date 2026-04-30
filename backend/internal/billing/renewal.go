package billing

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

// RenewalService ties invoice issuance, payment collection, and subscription extension together.
type RenewalService struct {
	Pool           *pgxpool.Pool
	Companies      *repository.CompanyRepository
	Invoices       *repository.InvoiceRepository
	Users          *repository.UserRepository
	MP             *mercadopago.Client
	StubAutoCharge bool
	RateQuoter     USDARSQuoter // used when AmountMinor <= 0 in Run()
}

type RenewalRunInput struct {
	CompanyID    uuid.UUID
	AmountMinor  int64
	Currency     string
	Description  string
	ExtendMonths int
}

type RenewalRunResult struct {
	InvoiceID             uuid.UUID
	Charged               bool
	PaymentRef            string
	SubscriptionExpiresAt *string
}

// NewRenewalService returns a RenewalService with the given dependencies.
func NewRenewalService(
	pool *pgxpool.Pool,
	companies *repository.CompanyRepository,
	invoices *repository.InvoiceRepository,
	users *repository.UserRepository,
	mp *mercadopago.Client,
	stubAutoCharge bool,
	quoter USDARSQuoter,
) *RenewalService {
	return &RenewalService{
		Pool:           pool,
		Companies:      companies,
		Invoices:       invoices,
		Users:          users,
		MP:             mp,
		StubAutoCharge: stubAutoCharge,
		RateQuoter:     quoter,
	}
}

// Run creates a pending invoice, attempts an automatic charge, and on approval marks the invoice paid
// and extends subscription_expires_at. AmountMinor is ARS centavos (1/100 ARS) per billing_invoices convention.
// If in.AmountMinor <= 0, the amount is derived from companies.subscription_plan × MEP (bolsa) quote (or env fallback).
func (s *RenewalService) Run(ctx context.Context, in RenewalRunInput) (*RenewalRunResult, error) {
	if in.ExtendMonths <= 0 {
		return nil, fmt.Errorf("extend_months must be positive")
	}
	currency := strings.TrimSpace(in.Currency)
	if currency == "" {
		currency = "ARS"
	}

	company, err := s.Companies.GetByIDForBilling(ctx, in.CompanyID)
	if err != nil {
		return nil, err
	}
	if company == nil {
		return nil, fmt.Errorf("company not found")
	}
	if err := validateCompanyForRenewal(company); err != nil {
		return nil, err
	}

	amountMinor := in.AmountMinor
	if amountMinor <= 0 {
		if s.RateQuoter == nil {
			return nil, fmt.Errorf("billing rate quoter is not configured")
		}
		q, qerr := s.RateQuoter.Quote(ctx)
		if qerr != nil {
			return nil, fmt.Errorf("billing fx: %w", qerr)
		}
		computed, perr := PlanMonthlyAmountMinorARS(company.SubscriptionPlan, q.SellPerUSD)
		if perr != nil {
			return nil, fmt.Errorf("billing amount: %w", perr)
		}
		amountMinor = computed
	}

	payerEmail, err := s.Users.GetCompanyOwnerPrimaryEmail(ctx, in.CompanyID)
	if err != nil {
		return nil, err
	}
	if payerEmail == "" {
		return nil, fmt.Errorf("no company_owner email available for payer metadata")
	}

	tx1, err := s.Pool.Begin(ctx)
	if err != nil {
		return nil, err
	}
	invoiceID, err := s.Invoices.InsertPending(ctx, tx1, in.CompanyID, amountMinor, currency, in.Description)
	if err != nil {
		_ = tx1.Rollback(ctx)
		return nil, err
	}
	if err := tx1.Commit(ctx); err != nil {
		return nil, err
	}

	custID := ""
	cardID := ""
	if company.MpCustomerID != nil {
		custID = strings.TrimSpace(*company.MpCustomerID)
	}
	if company.MpCardID != nil {
		cardID = strings.TrimSpace(*company.MpCardID)
	}

	amountARS := float64(amountMinor) / 100.0

	chargeOut, err := s.MP.ChargeRenewal(ctx, mercadopago.RenewalChargeInput{
		PayerEmail:        payerEmail,
		CustomerID:        custID,
		CardID:            cardID,
		AmountARS:         amountARS,
		Description:       in.Description,
		ExternalReference: invoiceID.String(),
	}, s.StubAutoCharge)
	if err != nil {
		return &RenewalRunResult{
			InvoiceID:  invoiceID,
			Charged:    false,
			PaymentRef: "",
		}, err
	}
	if !chargeOut.Approved || chargeOut.PaymentID == "" {
		return &RenewalRunResult{
			InvoiceID:  invoiceID,
			Charged:    false,
			PaymentRef: "",
		}, errors.New("payment not approved")
	}

	tx2, err := s.Pool.Begin(ctx)
	if err != nil {
		return nil, err
	}
	if err := s.Invoices.MarkPaid(ctx, tx2, invoiceID, in.CompanyID, chargeOut.PaymentID); err != nil {
		_ = tx2.Rollback(ctx)
		return nil, err
	}
	newEnds, err := s.Companies.ExtendPaidSubscriptionPeriod(ctx, tx2, in.CompanyID, in.ExtendMonths)
	if err != nil {
		_ = tx2.Rollback(ctx)
		return nil, err
	}
	if err := tx2.Commit(ctx); err != nil {
		return nil, err
	}

	expiresRFC := newEnds.UTC().Format(time.RFC3339)

	return &RenewalRunResult{
		InvoiceID:             invoiceID,
		Charged:               true,
		PaymentRef:            chargeOut.PaymentID,
		SubscriptionExpiresAt: &expiresRFC,
	}, nil
}

func validateCompanyForRenewal(c *models.Company) error {
	if c.ArchivedAt != nil {
		return fmt.Errorf("company is archived")
	}
	if c.Status != "" && c.Status != "active" {
		return fmt.Errorf("company status is not active")
	}
	if !IsPaidPlan(c.SubscriptionPlan) {
		return fmt.Errorf("company plan is not billable as paid subscription")
	}
	return nil
}
