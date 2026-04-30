package mep

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestFetchLatestBolsa(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`[
			{"casa":"bolsa","compra":100,"venta":101,"fecha":"2026-04-28"},
			{"casa":"bolsa","compra":1450,"venta":1460,"fecha":"2026-04-30"}
		]`))
	}))
	defer srv.Close()

	q, err := FetchLatestBolsa(context.Background(), srv.Client(), srv.URL)
	if err != nil {
		t.Fatal(err)
	}
	if q.Sell != 1460 {
		t.Fatalf("Sell = %v", q.Sell)
	}
	if q.Buy != 1450 {
		t.Fatalf("Buy = %v", q.Buy)
	}
}
