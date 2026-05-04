package billing

import (
	"context"
	"net/http"
	"time"

	"server/internal/billing/mep"
)

// USDARSQuote is one USD priced in ARS (seller side) for billing.
type USDARSQuote struct {
	// SellPerUSD is ARS per 1 USD (e.g. bolsa venta).
	SellPerUSD float64
	// EffectiveDate is the quote date from the source (billing date alignment).
	EffectiveDate time.Time
	// Source identifies how the rate was obtained (e.g. dolarapi_bolsa, env_fallback).
	Source string
}

// USDARSQuoter resolves ARS per 1 USD for catalog USD prices.
type USDARSQuoter interface {
	Quote(ctx context.Context) (USDARSQuote, error)
}

// MEPWithFallback tries dolarapi.com bolsa (default); on failure uses fallbackARSPerUSD if > 0.
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
			Source:        "dolarapi_bolsa",
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
