package billing

import (
	"context"
	"net/http"
	"time"

	"server/internal/billing/mep"
)

// USDARSQuote is one USD priced in ARS (seller side) for billing.
type USDARSQuote struct {
	// SellPerUSD is ARS per 1 USD (e.g. MEP venta).
	SellPerUSD float64
	// EffectiveDate is the quote date from the source (billing date alignment).
	EffectiveDate time.Time
	// Source identifies how the rate was obtained (e.g. argentinadatos_bolsa_mep, env_fallback).
	Source string
}

// USDARSQuoter resolves ARS per 1 USD for catalog USD prices.
type USDARSQuoter interface {
	Quote(ctx context.Context) (USDARSQuote, error)
}

// MEPWithFallback tries ArgentinaDatos dólar bolsa (MEP); on failure uses fallbackARSPerUSD if > 0.
type MEPWithFallback struct {
	HTTP              *http.Client
	BolsaURL          string
	FallbackARSPerUSD float64
}

// Quote implements USDARSQuoter.
func (m *MEPWithFallback) Quote(ctx context.Context) (USDARSQuote, error) {
	client := m.HTTP
	if client == nil {
		client = http.DefaultClient
	}
	q, err := mep.FetchLatestBolsa(ctx, client, m.BolsaURL)
	if err == nil {
		return USDARSQuote{
			SellPerUSD:    q.Sell,
			EffectiveDate: q.QuoteDate,
			Source:        "argentinadatos_bolsa_mep",
		}, nil
	}
	if m.FallbackARSPerUSD > 0 {
		return USDARSQuote{
			SellPerUSD:    m.FallbackARSPerUSD,
			EffectiveDate: time.Now(),
			Source:        "env_fallback",
		}, nil
	}
	return USDARSQuote{}, err
}

// LegalNoticeContractUSDARSMEPTemplate is shown next to ARS amounts (Argentine Spanish).
// Technical implementation uses the dólar bolsa series (ArgentinaDatos) as the MEP reference for each billing date.
const LegalNoticeContractUSDARSMEPTemplate = "Los precios están denominados en dólares estadounidenses (USD). Los pagos en pesos argentinos (ARS) se calcularán utilizando el tipo de cambio del dólar MEP vigente en la fecha de facturación."
