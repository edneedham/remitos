// Package mep fetches the Argentine MEP-style "dólar bolsa" quote used for USD→ARS conversion.
// Source: https://api.argentinadatos.com (series cotizaciones/dolares/bolsa — ARS per 1 USD).
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

const defaultBolsaURL = "https://api.argentinadatos.com/v1/cotizaciones/dolares/bolsa"

type bolsaRow struct {
	Compra float64 `json:"compra"`
	Venta  float64 `json:"venta"`
	Fecha  string  `json:"fecha"`
}

// LatestQuote is the most recent bolsa (MEP) row from the API.
type LatestQuote struct {
	// Sell is ARS per 1 USD (precio venta / ask side), used for billing.
	Sell float64
	// Buy is ARS compra from the same publication (informational).
	Buy       float64
	QuoteDate time.Time
}

// FetchLatestBolsa returns the latest dólar bolsa row (MEP). venta is used as the billing rate (ARS per USD).
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
	if len(raw) == 0 || raw[0] != '[' {
		return LatestQuote{}, fmt.Errorf("mep: unexpected response shape")
	}
	var rows []bolsaRow
	if err := json.Unmarshal(raw, &rows); err != nil {
		return LatestQuote{}, fmt.Errorf("mep: decode: %w", err)
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
