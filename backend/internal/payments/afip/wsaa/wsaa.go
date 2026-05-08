// Package wsaa implements AFIP/ARCA WSAA (Web Service de Autenticación y Autorización).
//
// WSAA is the auth layer that all other AFIP web services depend on. To obtain a
// service-specific TA (Token + Sign), the client signs a small XML "loginTicketRequest"
// payload with their X.509 client certificate using CMS (PKCS#7 SignedData), and posts the
// base64-encoded CMS blob to the LoginCms SOAP endpoint. The response contains the TA which
// is valid for 12 hours and must be cached and reused; AFIP rate-limits new TA requests
// per service.
package wsaa

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/xml"
	"fmt"
	"io"
	"math/big"
	"net/http"
	"strings"
	"time"

	"github.com/digitorus/pkcs7"

	"server/internal/payments/afip/certprovider"
)

// TA is the WSAA "ticket de acceso" returned for a given service. Token + Sign are passed
// in every subsequent SOAP request to that service's <Auth> header.
type TA struct {
	Service        string
	Token          string
	Sign           string
	GenerationTime time.Time
	ExpirationTime time.Time
}

// Expired reports whether the TA is past its ExpirationTime (with a small safety margin).
func (t TA) Expired(now time.Time) bool {
	if t.ExpirationTime.IsZero() {
		return true
	}
	return !now.Add(2 * time.Minute).Before(t.ExpirationTime)
}

// Client signs login_cms tickets and exchanges them at LoginCms for a TA.
type Client struct {
	URL   string
	HTTP  *http.Client
	Certs certprovider.CertProvider
}

// New constructs a WSAA Client.
func New(url string, hc *http.Client, certs certprovider.CertProvider) *Client {
	if hc == nil {
		hc = defaultHTTPClient()
	}
	return &Client{URL: url, HTTP: hc, Certs: certs}
}

func defaultHTTPClient() *http.Client {
	return &http.Client{Timeout: 60 * time.Second}
}

// LoginCMS signs a login_cms request for the given service ID (e.g. "wsfe", "ws_sr_padron_a5",
// "ws_sr_constancia_inscripcion") and exchanges it with WSAA for a fresh TA.
func (c *Client) LoginCMS(ctx context.Context, service string) (*TA, error) {
	service = strings.TrimSpace(service)
	if service == "" {
		return nil, fmt.Errorf("wsaa: service is required")
	}
	if c.Certs == nil {
		return nil, fmt.Errorf("wsaa: cert provider is nil")
	}
	mat, err := c.Certs.Load()
	if err != nil {
		return nil, fmt.Errorf("wsaa: load cert: %w", err)
	}

	now := time.Now().UTC()
	gen := now.Add(-2 * time.Minute) // small clock-skew tolerance
	exp := now.Add(11*time.Hour + 50*time.Minute)

	uniqueID, err := newUniqueID()
	if err != nil {
		return nil, err
	}
	tra := buildLoginTicketRequest(uniqueID, gen, exp, service)

	cms, err := signCMS(tra, mat)
	if err != nil {
		return nil, fmt.Errorf("wsaa: sign cms: %w", err)
	}
	cmsB64 := base64.StdEncoding.EncodeToString(cms)

	respXML, err := c.callLoginCms(ctx, cmsB64)
	if err != nil {
		return nil, err
	}

	ta, err := parseLoginCmsResponse(respXML, service)
	if err != nil {
		return nil, fmt.Errorf("wsaa: parse response: %w", err)
	}
	return ta, nil
}

func newUniqueID() (uint32, error) {
	n, err := rand.Int(rand.Reader, big.NewInt(0xFFFFFFFE))
	if err != nil {
		return 0, err
	}
	return uint32(n.Uint64()) + 1, nil
}

// buildLoginTicketRequest renders the loginTicketRequest XML expected by AFIP. The XML must
// be UTF-8 encoded; AFIP rejects payloads with extraneous whitespace inside elements but is
// forgiving about indentation between elements.
func buildLoginTicketRequest(uniqueID uint32, gen, exp time.Time, service string) []byte {
	const layout = "2006-01-02T15:04:05-07:00"
	body := fmt.Sprintf(`<?xml version="1.0" encoding="UTF-8"?>
<loginTicketRequest version="1.0">
  <header>
    <uniqueId>%d</uniqueId>
    <generationTime>%s</generationTime>
    <expirationTime>%s</expirationTime>
  </header>
  <service>%s</service>
</loginTicketRequest>`,
		uniqueID,
		gen.Local().Format(layout),
		exp.Local().Format(layout),
		service,
	)
	return []byte(body)
}

// signCMS produces a CMS (PKCS#7) SignedData blob over the given content using the
// caller's X.509 cert + private key. AFIP expects DER-encoded CMS, base64-armored at the
// transport layer.
func signCMS(content []byte, mat *certprovider.Material) ([]byte, error) {
	sd, err := pkcs7.NewSignedData(content)
	if err != nil {
		return nil, err
	}
	if err := sd.AddSigner(mat.Certificate, mat.PrivateKey, pkcs7.SignerInfoConfig{}); err != nil {
		return nil, err
	}
	der, err := sd.Finish()
	if err != nil {
		return nil, err
	}
	return der, nil
}

// callLoginCms posts the SOAP envelope to WSAA and returns the inner loginCmsReturn XML body.
func (c *Client) callLoginCms(ctx context.Context, cmsB64 string) (string, error) {
	envelope := fmt.Sprintf(`<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
  <soapenv:Header/>
  <soapenv:Body>
    <wsaa:loginCms>
      <wsaa:in0>%s</wsaa:in0>
    </wsaa:loginCms>
  </soapenv:Body>
</soapenv:Envelope>`, cmsB64)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.URL, strings.NewReader(envelope))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "text/xml; charset=utf-8")
	req.Header.Set("SOAPAction", "")

	resp, err := c.HTTP.Do(req)
	if err != nil {
		return "", fmt.Errorf("wsaa: post: %w", err)
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("wsaa: status %d: %s", resp.StatusCode, truncate(string(raw), 1024))
	}

	body, err := extractLoginCmsReturn(raw)
	if err != nil {
		return "", fmt.Errorf("wsaa: extract loginCmsReturn: %w (raw=%s)", err, truncate(string(raw), 512))
	}
	return body, nil
}

// extractLoginCmsReturn pulls the inner XML payload (loginTicketResponse) out of the SOAP
// envelope. The payload is double-encoded (XML escaped inside a string element), so we use
// xml.Decoder to walk the envelope and extract the CharData.
func extractLoginCmsReturn(soap []byte) (string, error) {
	dec := xml.NewDecoder(bytes.NewReader(soap))
	for {
		tok, err := dec.Token()
		if err == io.EOF {
			return "", fmt.Errorf("loginCmsReturn not found")
		}
		if err != nil {
			return "", err
		}
		if se, ok := tok.(xml.StartElement); ok && se.Name.Local == "loginCmsReturn" {
			var inner string
			if err := dec.DecodeElement(&inner, &se); err != nil {
				return "", err
			}
			return inner, nil
		}
	}
}

// loginTicketResponse mirrors the inner XML returned by AFIP after successful sign-in.
type loginTicketResponse struct {
	XMLName     xml.Name `xml:"loginTicketResponse"`
	Header      ltrHeader `xml:"header"`
	Credentials struct {
		Token string `xml:"token"`
		Sign  string `xml:"sign"`
	} `xml:"credentials"`
}

type ltrHeader struct {
	GenerationTime string `xml:"generationTime"`
	ExpirationTime string `xml:"expirationTime"`
}

func parseLoginCmsResponse(inner string, service string) (*TA, error) {
	var ltr loginTicketResponse
	if err := xml.Unmarshal([]byte(inner), &ltr); err != nil {
		return nil, err
	}
	if ltr.Credentials.Token == "" || ltr.Credentials.Sign == "" {
		return nil, fmt.Errorf("missing token/sign in loginTicketResponse")
	}
	gen, _ := time.Parse(time.RFC3339, ltr.Header.GenerationTime)
	exp, _ := time.Parse(time.RFC3339, ltr.Header.ExpirationTime)
	return &TA{
		Service:        service,
		Token:          ltr.Credentials.Token,
		Sign:           ltr.Credentials.Sign,
		GenerationTime: gen,
		ExpirationTime: exp,
	}, nil
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}
