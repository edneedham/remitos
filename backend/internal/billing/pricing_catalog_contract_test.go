package billing

import (
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"testing"
)

var webCatalogPriceBlockPattern = regexp.MustCompile(`(?s)export const PLAN_MONTHLY_LIST_PRICE_USD:[^=]*=\s*\{(.*?)\};`)
var webCatalogPriceEntryPattern = regexp.MustCompile(`([a-z]+)\s*:\s*([0-9]+(?:\.[0-9]+)?)`)

// TestWebAndBackendPlanCatalogUSDPricesMatch is a contract test that prevents
// silent drift between backend billing prices and web plan catalog prices.
func TestWebAndBackendPlanCatalogUSDPricesMatch(t *testing.T) {
	webCatalogPath := filepath.Join("..", "..", "..", "website", "src", "app", "lib", "planCatalog.ts")
	raw, err := os.ReadFile(webCatalogPath)
	if err != nil {
		t.Fatalf("read website plan catalog: %v", err)
	}

	matches := webCatalogPriceBlockPattern.FindStringSubmatch(string(raw))
	if len(matches) < 2 {
		t.Fatalf("could not parse PLAN_MONTHLY_LIST_PRICE_USD from %s", webCatalogPath)
	}

	webPrices := make(map[string]float64)
	for _, entry := range webCatalogPriceEntryPattern.FindAllStringSubmatch(matches[1], -1) {
		planID := strings.TrimSpace(strings.ToLower(entry[1]))
		price, parseErr := strconv.ParseFloat(strings.TrimSpace(entry[2]), 64)
		if parseErr != nil {
			t.Fatalf("parse web catalog price for %s: %v", planID, parseErr)
		}
		webPrices[planID] = price
	}

	if len(webPrices) == 0 {
		t.Fatalf("no price entries found in website PLAN_MONTHLY_LIST_PRICE_USD")
	}

	if len(webPrices) != len(MonthlyListPriceUSD) {
		t.Fatalf("catalog size mismatch: website has %d plans, backend has %d plans", len(webPrices), len(MonthlyListPriceUSD))
	}

	for planID, backendPrice := range MonthlyListPriceUSD {
		webPrice, ok := webPrices[planID]
		if !ok {
			t.Fatalf("plan %q exists in backend but not website catalog", planID)
		}
		if webPrice != backendPrice {
			t.Fatalf("price mismatch for %q: website=%v backend=%v", planID, webPrice, backendPrice)
		}
	}

	for planID := range webPrices {
		if _, ok := MonthlyListPriceUSD[planID]; !ok {
			t.Fatalf("plan %q exists in website catalog but not backend", planID)
		}
	}
}
