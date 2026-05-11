// Package padron is a SOAP client for AFIP/ARCA's CUIT/CUIL lookup web services.
//
// Default service: ws_sr_padron_a5 (personaServiceA5) — returns the most data points used
// by billing flows (razón social, condición IVA, domicilio fiscal, estado de la clave). The
// Client.LookupCUIT method returns a normalized Persona struct so callers do not need to
// know the underlying XML shape.
package padron

import (
	"bytes"
	"context"
	"encoding/xml"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// Client is the padrón SOAP client. ws_sr_padron_a5 expects auth (token+sign+cuit) like
// other AFIP services; the consuming CUIT (cuitRepresentada) is the issuer's CUIT.
type Client struct {
	URL  string
	HTTP *http.Client
}

func New(url string, hc *http.Client) *Client {
	if hc == nil {
		hc = &http.Client{Timeout: 30 * time.Second}
	}
	return &Client{URL: url, HTTP: hc}
}

// Auth carries the WSAA TA + emisor CUIT.
type Auth struct {
	Token string
	Sign  string
	Cuit  string
}

// CondicionIVA is the canonical form used inside Remitos for tax category. Matches the
// internal companies.condicion_iva enum-like column.
type CondicionIVA string

const (
	CondicionRI              CondicionIVA = "RESPONSABLE_INSCRIPTO"
	CondicionMonotributo     CondicionIVA = "MONOTRIBUTO"
	CondicionExento          CondicionIVA = "EXENTO"
	CondicionConsumidorFinal CondicionIVA = "CONSUMIDOR_FINAL"
	CondicionNoCategorizado  CondicionIVA = "NO_CATEGORIZADO"
	CondicionDesconocido     CondicionIVA = "DESCONOCIDO"
)

// Persona is the trimmed-down padrón record we persist on companies and use at emission time.
type Persona struct {
	CUIT            string
	TipoPersona     string // FISICA | JURIDICA
	EstadoClave     string // ACTIVO | INACTIVO
	RazonSocial     string
	Nombre          string
	Apellido        string
	CondicionIVA    CondicionIVA
	DomicilioFiscal string
	// FetchedAt records when this snapshot was returned by AFIP (UTC).
	FetchedAt time.Time
	// RawXML is the inner getPersonaReturn body for audit/debug.
	RawXML string
}

// LookupCUIT calls getPersona(cuitRepresentada, sign, token, idPersona) and decodes the result.
func (c *Client) LookupCUIT(ctx context.Context, auth Auth, cuit string) (*Persona, error) {
	cuit = digitsOnly(cuit)
	if len(cuit) != 11 {
		return nil, fmt.Errorf("padron: CUIT must be 11 digits, got %q", cuit)
	}
	body := fmt.Sprintf(`<a5:getPersona xmlns:a5="http://a5.soap.ws.server.puc.sr/">
  <token>%s</token>
  <sign>%s</sign>
  <cuitRepresentada>%s</cuitRepresentada>
  <idPersona>%s</idPersona>
</a5:getPersona>`, escape(auth.Token), escape(auth.Sign), escape(auth.Cuit), cuit)

	raw, err := c.callSOAP(ctx, "", body)
	if err != nil {
		return nil, err
	}
	persona, err := parseGetPersonaResult(raw)
	if err != nil {
		return nil, fmt.Errorf("padron: parse: %w (raw=%s)", err, truncate(string(raw), 512))
	}
	if persona.CUIT == "" {
		persona.CUIT = cuit
	}
	persona.FetchedAt = time.Now().UTC()
	persona.RawXML = string(raw)
	return persona, nil
}

func (c *Client) callSOAP(ctx context.Context, soapAction, innerBody string) ([]byte, error) {
	envelope := fmt.Sprintf(`<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Header/>
  <soapenv:Body>
    %s
  </soapenv:Body>
</soapenv:Envelope>`, innerBody)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.URL, strings.NewReader(envelope))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "text/xml; charset=utf-8")
	req.Header.Set("SOAPAction", soapAction)
	resp, err := c.HTTP.Do(req)
	if err != nil {
		return nil, fmt.Errorf("padron: post: %w", err)
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, fmt.Errorf("padron: status %d: %s", resp.StatusCode, truncate(string(raw), 1024))
	}
	return raw, nil
}

// padronReturn captures the structurally relevant fields. ws_sr_padron_a5 returns a
// "personaReturn" element with nested "persona" or "datosGenerales"/"datosMonotributo" etc.
type padronReturn struct {
	XMLName xml.Name `xml:"personaReturn"`
	Persona struct {
		Cuit        string `xml:"idPersona"`
		TipoPersona string `xml:"tipoPersona"`
		EstadoClave string `xml:"estadoClave"`
		Nombre      string `xml:"nombre"`
		Apellido    string `xml:"apellido"`
		RazonSocial string `xml:"razonSocial"`
		Domicilio   []struct {
			Direccion string `xml:"direccion"`
			Localidad string `xml:"localidad"`
			Provincia string `xml:"descripcionProvincia"`
			CodPostal string `xml:"codPostal"`
			TipoDomic string `xml:"tipoDomicilio"`
		} `xml:"domicilio"`
		DatosMonotributo *struct {
			Categoria string `xml:"categoriaMonotributo"`
		} `xml:"datosMonotributo"`
		ImpuestosActivos struct {
			Impuestos []struct {
				IDImpuesto     int    `xml:"idImpuesto"`
				Descripcion    string `xml:"descripcionImpuesto"`
				EstadoImpuesto string `xml:"estadoImpuesto"`
				Periodo        string `xml:"periodo"`
			} `xml:"impuesto"`
		} `xml:"impuestosActivos"`
		CategoriasMonotributo struct {
			Categorias []struct {
				IDImpuesto int    `xml:"idImpuesto"`
				Categoria  string `xml:"descripcionCategoria"`
			} `xml:"categoria"`
		} `xml:"categoriasMonotributo"`
	} `xml:"persona"`
}

func parseGetPersonaResult(soap []byte) (*Persona, error) {
	// Walk to <personaReturn> (the SOAP body wraps it inside <getPersonaResponse>).
	dec := xml.NewDecoder(bytes.NewReader(soap))
	for {
		tok, err := dec.Token()
		if err == io.EOF {
			return nil, fmt.Errorf("personaReturn not found")
		}
		if err != nil {
			return nil, err
		}
		if se, ok := tok.(xml.StartElement); ok && se.Name.Local == "personaReturn" {
			var pr padronReturn
			if err := dec.DecodeElement(&pr, &se); err != nil {
				return nil, err
			}
			return personaFromReturn(pr), nil
		}
	}
}

func personaFromReturn(pr padronReturn) *Persona {
	out := &Persona{
		CUIT:        strings.TrimSpace(pr.Persona.Cuit),
		TipoPersona: strings.ToUpper(strings.TrimSpace(pr.Persona.TipoPersona)),
		EstadoClave: strings.ToUpper(strings.TrimSpace(pr.Persona.EstadoClave)),
		RazonSocial: strings.TrimSpace(pr.Persona.RazonSocial),
		Nombre:      strings.TrimSpace(pr.Persona.Nombre),
		Apellido:    strings.TrimSpace(pr.Persona.Apellido),
	}
	out.CondicionIVA = inferCondicionIVA(pr)
	if len(pr.Persona.Domicilio) > 0 {
		d := pr.Persona.Domicilio[0]
		parts := []string{d.Direccion, d.Localidad, d.Provincia, d.CodPostal}
		clean := make([]string, 0, len(parts))
		for _, p := range parts {
			if s := strings.TrimSpace(p); s != "" {
				clean = append(clean, s)
			}
		}
		out.DomicilioFiscal = strings.Join(clean, ", ")
	}
	if out.RazonSocial == "" {
		full := strings.TrimSpace(out.Apellido + " " + out.Nombre)
		if full != "" {
			out.RazonSocial = full
		}
	}
	return out
}

func inferCondicionIVA(pr padronReturn) CondicionIVA {
	hasMonotributo := pr.Persona.DatosMonotributo != nil ||
		len(pr.Persona.CategoriasMonotributo.Categorias) > 0
	hasIVA := false
	hasExento := false
	for _, imp := range pr.Persona.ImpuestosActivos.Impuestos {
		switch imp.IDImpuesto {
		case 30: // IVA
			if strings.EqualFold(imp.EstadoImpuesto, "ACTIVO") || imp.EstadoImpuesto == "" {
				hasIVA = true
			}
		case 32: // IVA Exento
			hasExento = true
		case 20: // Monotributo
			hasMonotributo = true
		}
	}
	switch {
	case hasIVA:
		return CondicionRI
	case hasMonotributo:
		return CondicionMonotributo
	case hasExento:
		return CondicionExento
	default:
		// Padrón does not always populate enough to distinguish CF from non-categorizado.
		return CondicionNoCategorizado
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

func escape(s string) string {
	r := strings.NewReplacer("&", "&amp;", "<", "&lt;", ">", "&gt;")
	return r.Replace(s)
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}
