package wsaa

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

const sampleSoapResponse = `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <ns1:loginCmsResponse xmlns:ns1="http://wsaa.view.sua.dvadac.desein.afip.gov">
      <loginCmsReturn>&lt;?xml version="1.0"?&gt;
&lt;loginTicketResponse&gt;
  &lt;header&gt;
    &lt;source&gt;CN=wsaahomo,O=AFIP,C=AR,SERIALNUMBER=CUIT 33693450239&lt;/source&gt;
    &lt;destination&gt;CN=remitos,O=remitos,C=AR&lt;/destination&gt;
    &lt;uniqueId&gt;42&lt;/uniqueId&gt;
    &lt;generationTime&gt;2026-05-08T11:00:00-03:00&lt;/generationTime&gt;
    &lt;expirationTime&gt;2026-05-08T22:00:00-03:00&lt;/expirationTime&gt;
  &lt;/header&gt;
  &lt;credentials&gt;
    &lt;token&gt;PD94bWwgdmVyc2lvbj0iMS4wIj8&gt;_FAKE_TOKEN&lt;/token&gt;
    &lt;sign&gt;abc123sign==&lt;/sign&gt;
  &lt;/credentials&gt;
&lt;/loginTicketResponse&gt;</loginCmsReturn>
    </ns1:loginCmsResponse>
  </soapenv:Body>
</soapenv:Envelope>`

func TestParseLoginCmsResponseHappyPath(t *testing.T) {
	body, err := extractLoginCmsReturn([]byte(sampleSoapResponse))
	if err != nil {
		t.Fatalf("extractLoginCmsReturn: %v", err)
	}
	if !strings.Contains(body, "loginTicketResponse") {
		t.Fatalf("unwrapped body does not contain loginTicketResponse: %s", body)
	}
	ta, err := parseLoginCmsResponse(body, "wsfe")
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	if ta.Token == "" || ta.Sign == "" {
		t.Fatalf("missing token/sign: %+v", ta)
	}
	if ta.ExpirationTime.IsZero() {
		t.Fatalf("expiration not parsed: %+v", ta)
	}
	if ta.Service != "wsfe" {
		t.Fatalf("service got %q want %q", ta.Service, "wsfe")
	}
}

func TestExpired(t *testing.T) {
	now := time.Date(2026, 5, 8, 12, 0, 0, 0, time.UTC)
	ta := TA{ExpirationTime: now.Add(10 * time.Minute)}
	if ta.Expired(now) {
		t.Fatalf("not yet expired")
	}
	if !ta.Expired(now.Add(15 * time.Minute)) {
		t.Fatalf("should be expired past expiration")
	}
	// Within 2-minute safety margin counts as expired.
	if !ta.Expired(now.Add(9 * time.Minute)) {
		t.Fatalf("should be considered expired inside safety margin")
	}
}

func TestBuildLoginTicketRequestHasService(t *testing.T) {
	xml := buildLoginTicketRequest(7, time.Now(), time.Now().Add(time.Hour), "wsfe")
	if !strings.Contains(string(xml), "<service>wsfe</service>") {
		t.Fatalf("missing service element: %s", xml)
	}
	if !strings.Contains(string(xml), "<uniqueId>7</uniqueId>") {
		t.Fatalf("missing uniqueId element: %s", xml)
	}
}

// Smoke test that callLoginCms posts to the configured URL and decodes a real-shaped reply.
// Cert/sign path is exercised separately via integration.
func TestCallLoginCmsRoundTrip(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}
		if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "text/xml") {
			t.Errorf("expected text/xml, got %q", ct)
		}
		w.Header().Set("Content-Type", "text/xml; charset=utf-8")
		_, _ = w.Write([]byte(sampleSoapResponse))
	}))
	defer server.Close()

	c := &Client{URL: server.URL, HTTP: server.Client()}
	body, err := c.callLoginCms(context.Background(), "FAKE_CMS_BLOB")
	if err != nil {
		t.Fatalf("callLoginCms: %v", err)
	}
	if !strings.Contains(body, "loginTicketResponse") {
		t.Fatalf("unexpected body: %s", body)
	}
}
