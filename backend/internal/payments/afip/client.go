// Package afip is a direct integration with AFIP/ARCA web services for Argentine factura
// electrónica (WSFEv1) and CUIT padrón (constancia de inscripción).
//
// Sub-packages:
//
//	wsaa         — login_cms ticket issuance with CMS PKCS#7 signing
//	wsfev1       — invoice authorization (CAE) and parameter queries
//	padron       — CUIT lookup (razón social, condición IVA, domicilio fiscal)
//	certprovider — pluggable X.509 cert + key sources (env, GCP Secret Manager)
//
// The top-level Client wires emisor identity (CUIT, punto de venta, condición IVA) and the
// active environment (homo / prod) so handlers can construct service clients without
// re-reading config.
package afip

import (
	"errors"
	"fmt"
	"net/http"
	"strings"
	"time"

	"server/internal/payments/afip/certprovider"
)

// Env names the active AFIP environment. "homo" maps to homologación (testing) URLs; "prod"
// hits production. Empty / unknown values fall back to "homo" to avoid accidental prod calls.
type Env string

const (
	EnvHomo Env = "homo"
	EnvProd Env = "prod"
)

// Default SOAP endpoints (homologación + producción). Overridable via Client.URLOverrides for tests.
const (
	WSAAHomoURL   = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms"
	WSAAProdURL   = "https://wsaa.afip.gov.ar/ws/services/LoginCms"
	WSFEv1HomoURL = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx"
	WSFEv1ProdURL = "https://servicios1.afip.gov.ar/wsfev1/service.asmx"
	PadronHomoURL = "https://awshomo.afip.gov.ar/sr-padron/webservices/personaServiceA5"
	PadronProdURL = "https://aws.afip.gov.ar/sr-padron/webservices/personaServiceA5"
)

// IssuerCondicion represents Remitos' tax category for factura tipo selection.
type IssuerCondicion string

const (
	IssuerRI          IssuerCondicion = "RESPONSABLE_INSCRIPTO"
	IssuerMonotributo IssuerCondicion = "MONOTRIBUTO"
	IssuerExento      IssuerCondicion = "EXENTO"
)

// URLOverrides lets tests replace SOAP endpoints with httptest servers.
type URLOverrides struct {
	WSAA   string
	WSFEv1 string
	Padron string
}

// Client is the top-level AFIP integration handle. Hold one per process.
type Client struct {
	Env             Env
	CUIT            string // emisor CUIT, digits only
	PuntoVenta      int
	IssuerCondicion IssuerCondicion
	Concepto        int
	DefaultAlicuota float64

	Certs certprovider.CertProvider
	HTTP  *http.Client

	URLs URLOverrides
}

// Config wraps the inputs needed to construct a Client. All fields except URL overrides are required.
type Config struct {
	Env             string
	CUIT            string
	PuntoVenta      int
	IssuerCondicion string
	Concepto        int
	DefaultAlicuota float64
	Certs           certprovider.CertProvider

	WSAAURL   string
	WSFEv1URL string
	PadronURL string
}

// New constructs a Client. Returns an error if required fields are missing; URL overrides
// remain optional and default to the AFIP homologación/producción endpoints.
func New(cfg Config) (*Client, error) {
	env := normalizeEnv(cfg.Env)
	cuit := digitsOnly(cfg.CUIT)
	if cuit == "" {
		return nil, errors.New("afip: CUIT is required")
	}
	if cfg.PuntoVenta <= 0 {
		return nil, errors.New("afip: punto de venta is required")
	}
	if cfg.Certs == nil {
		return nil, errors.New("afip: cert provider is required")
	}
	concepto := cfg.Concepto
	if concepto <= 0 {
		concepto = 2
	}
	alic := cfg.DefaultAlicuota
	if alic <= 0 {
		alic = 21.0
	}
	cond := strings.ToUpper(strings.TrimSpace(cfg.IssuerCondicion))
	if cond == "" {
		cond = string(IssuerRI)
	}
	return &Client{
		Env:             env,
		CUIT:            cuit,
		PuntoVenta:      cfg.PuntoVenta,
		IssuerCondicion: IssuerCondicion(cond),
		Concepto:        concepto,
		DefaultAlicuota: alic,
		Certs:           cfg.Certs,
		HTTP: &http.Client{
			Timeout: 60 * time.Second,
		},
		URLs: URLOverrides{
			WSAA:   strings.TrimSpace(cfg.WSAAURL),
			WSFEv1: strings.TrimSpace(cfg.WSFEv1URL),
			Padron: strings.TrimSpace(cfg.PadronURL),
		},
	}, nil
}

// WSAAURL returns the active WSAA endpoint after applying overrides + env defaults.
func (c *Client) WSAAURL() string {
	if s := strings.TrimSpace(c.URLs.WSAA); s != "" {
		return s
	}
	if c.Env == EnvProd {
		return WSAAProdURL
	}
	return WSAAHomoURL
}

func (c *Client) WSFEv1URL() string {
	if s := strings.TrimSpace(c.URLs.WSFEv1); s != "" {
		return s
	}
	if c.Env == EnvProd {
		return WSFEv1ProdURL
	}
	return WSFEv1HomoURL
}

func (c *Client) PadronURL() string {
	if s := strings.TrimSpace(c.URLs.Padron); s != "" {
		return s
	}
	if c.Env == EnvProd {
		return PadronProdURL
	}
	return PadronHomoURL
}

func normalizeEnv(s string) Env {
	switch strings.ToLower(strings.TrimSpace(s)) {
	case string(EnvProd), "production", "produccion":
		return EnvProd
	default:
		return EnvHomo
	}
}

func digitsOnly(s string) string {
	var b strings.Builder
	for _, r := range s {
		if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	return b.String()
}

// FormatNumeroComprobante renders a "0003-00000123" style label for ptoVta + numero.
func FormatNumeroComprobante(ptoVta int, numero int64) string {
	return fmt.Sprintf("%04d-%08d", ptoVta, numero)
}
