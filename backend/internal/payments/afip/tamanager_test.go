package afip

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"server/internal/payments/afip/wsaa"
)

type memStore struct {
	mu  sync.Mutex
	tas map[string]StoredTA
}

func newMemStore() *memStore { return &memStore{tas: map[string]StoredTA{}} }

func (m *memStore) Get(_ context.Context, service string) (*StoredTA, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	ta, ok := m.tas[service]
	if !ok {
		return nil, nil
	}
	out := ta
	return &out, nil
}

func (m *memStore) Upsert(_ context.Context, ta StoredTA) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.tas[ta.Service] = ta
	return nil
}

const fakeWSAAResponse = `<?xml version="1.0"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <loginCmsResponse>
      <loginCmsReturn>&lt;loginTicketResponse&gt;&lt;header&gt;&lt;generationTime&gt;2026-05-08T11:00:00Z&lt;/generationTime&gt;&lt;expirationTime&gt;%s&lt;/expirationTime&gt;&lt;/header&gt;&lt;credentials&gt;&lt;token&gt;TOK&lt;/token&gt;&lt;sign&gt;SGN&lt;/sign&gt;&lt;/credentials&gt;&lt;/loginTicketResponse&gt;</loginCmsReturn>
    </loginCmsResponse>
  </soapenv:Body>
</soapenv:Envelope>`

// fakeCertProvider supplies certprovider.Material via direct setter; its Load() is never
// reached because we wire wsaa.Client to a fake HTTP server that doesn't call back to the
// signer.
type fakeCertProvider struct{}

func (fakeCertProvider) load() error { return nil }

// We don't need real signing for TAManager tests because we replace LoginCMS via a custom
// wsaa.Client whose Certs is never Load()ed (it short-circuits on a fake HTTP). To do that
// we pass a wsaa.Client whose Certs is a special provider that returns a precomputed sig
// path-free Material. Easier: bypass Certs by injecting a wrapper.
//
// For brevity, we directly call afip.TAManager.Get against a wsaa.Client whose .HTTP hits
// the fake server BUT we also ensure Certs is a stub material so signCMS doesn't blow up.

func TestTAManagerCachesAcrossCalls(t *testing.T) {
	calls := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		w.Header().Set("Content-Type", "text/xml; charset=utf-8")
		_, _ = w.Write([]byte(strings.Replace(fakeWSAAResponse, "%s", time.Now().Add(2*time.Hour).UTC().Format(time.RFC3339), 1)))
	}))
	defer server.Close()

	matProv := newDevCertProvider(t)
	wc := wsaa.New(server.URL, server.Client(), matProv)
	store := newMemStore()
	mgr := NewTAManager(wc, store)

	ctx := context.Background()
	if _, err := mgr.Get(ctx, "wsfe"); err != nil {
		t.Fatalf("first get: %v", err)
	}
	if _, err := mgr.Get(ctx, "wsfe"); err != nil {
		t.Fatalf("second get: %v", err)
	}
	if calls != 1 {
		t.Fatalf("expected 1 wsaa call (cached), got %d", calls)
	}
}

func TestTAManagerRefreshesAfterExpiration(t *testing.T) {
	calls := 0
	expFmt := time.Now().Add(-1 * time.Hour).UTC().Format(time.RFC3339) // already expired
	freshFmt := time.Now().Add(2 * time.Hour).UTC().Format(time.RFC3339)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		w.Header().Set("Content-Type", "text/xml; charset=utf-8")
		exp := expFmt
		if calls > 1 {
			exp = freshFmt
		}
		_, _ = w.Write([]byte(strings.Replace(fakeWSAAResponse, "%s", exp, 1)))
	}))
	defer server.Close()

	matProv := newDevCertProvider(t)
	wc := wsaa.New(server.URL, server.Client(), matProv)
	mgr := NewTAManager(wc, newMemStore())

	ctx := context.Background()
	if _, err := mgr.Get(ctx, "wsfe"); err != nil {
		t.Fatalf("first get: %v", err)
	}
	// Second call: cached TA is already expired (per server response), so Get must refresh.
	if _, err := mgr.Get(ctx, "wsfe"); err != nil {
		t.Fatalf("second get: %v", err)
	}
	if calls != 2 {
		t.Fatalf("expected 2 wsaa calls (cache miss after expiry), got %d", calls)
	}
}
