// Package mep fetches the Argentine "dólar bolsa" (MEP-style) quote for USD→ARS billing.
// Default source: https://dolarapi.com/v1/dolares/bolsa (single JSON object, venta = ARS per USD).
// Override BILLING_MEP_BOLSA_URL to use another endpoint that returns the same object shape,
// or legacy ArgentinaDatos array JSON for backward compatibility.
package mep

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

const defaultBolsaURL = "https://dolarapi.com/v1/dolares/bolsa"

type bolsaSeriesRow struct {
	Compra float64 `json:"compra"`
	Venta  float64 `json:"venta"`
	Fecha  string  `json:"fecha"`
}

// LatestQuote is the current bolsa (MEP) quote.
type LatestQuote struct {
	// Sell is ARS per 1 USD (venta), used for billing.
	Sell float64
	// Buy is ARS compra from the same publication (informational).
	Buy       float64
	QuoteDate time.Time
}

// FetchLatestBolsa returns the dólar bolsa rate. By default it uses dolarapi.com; set url to override.
func FetchLatestBolsa(ctx context.Context, client *http.Client, url string) (LatestQuote, error) {
	if client == nil {
		client = http.DefaultClient
	}
	if strings.TrimSpace(url) == "" {
		url = defaultBolsaURL
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return LatestQuote{}, err
	}
	resp, err := client.Do(req)
	if err != nil {
		return LatestQuote{}, err
	}
	defer resp.Body.Close()
	raw, err := io.ReadAll(resp.Body)
	if err != nil {
		return LatestQuote{}, err
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return LatestQuote{}, fmt.Errorf("mep: http %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}
	raw = bytes.TrimSpace(raw)
	if len(raw) == 0 {
		return LatestQuote{}, fmt.Errorf("mep: empty response")
	}
	switch raw[0] {
	case '{':
		return parseBolsaObject(raw)
	case '[':
		return parseBolsaSeries(raw)
	default:
		return LatestQuote{}, fmt.Errorf("mep: unexpected response shape")
	}
}

func parseBolsaObject(raw []byte) (LatestQuote, error) {
	var row struct {
		Compra             float64 `json:"compra"`
		Venta              float64 `json:"venta"`
		FechaActualizacion string  `json:"fechaActualizacion"`
	}
	if err := json.Unmarshal(raw, &row); err != nil {
		return LatestQuote{}, fmt.Errorf("mep: decode object: %w", err)
	}
	if row.Venta <= 0 {
		return LatestQuote{}, fmt.Errorf("mep: invalid venta %v", row.Venta)
	}
	d := time.Now().UTC()
	if ts := strings.TrimSpace(row.FechaActualizacion); ts != "" {
		if parsed, err := time.Parse(time.RFC3339, ts); err == nil {
			d = parsed.UTC()
		}
	}
	return LatestQuote{
		Sell:      row.Venta,
		Buy:       row.Compra,
		QuoteDate: d,
	}, nil
}

func parseBolsaSeries(raw []byte) (LatestQuote, error) {
	var rows []bolsaSeriesRow
	if err := json.Unmarshal(raw, &rows); err != nil {
		return LatestQuote{}, fmt.Errorf("mep: decode series: %w", err)
	}
	if len(rows) == 0 {
		return LatestQuote{}, fmt.Errorf("mep: empty series")
	}
	last := rows[len(rows)-1]
	if last.Venta <= 0 {
		return LatestQuote{}, fmt.Errorf("mep: invalid venta %v", last.Venta)
	}
	d, err := time.ParseInLocation("2006-01-02", strings.TrimSpace(last.Fecha), time.FixedZone("ART", -3*3600))
	if err != nil {
		d = time.Now().In(time.FixedZone("ART", -3*3600))
	}
	return LatestQuote{
		Sell:      last.Venta,
		Buy:       last.Compra,
		QuoteDate: d,
	}, nil
}
