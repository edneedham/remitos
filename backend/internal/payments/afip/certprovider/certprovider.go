// Package certprovider abstracts how the AFIP/ARCA X.509 client certificate and private key
// are loaded. EnvProvider reads PEM from env / disk; GCPSecretProvider lazily fetches and
// caches from GCP Secret Manager. New providers can be added by implementing CertProvider.
package certprovider

import (
	"crypto"
	"crypto/ecdsa"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"
	"os"
	"strings"
	"sync"
	"time"
)

// Material is the certificate + private key pair used to sign WSAA login_cms tickets.
// Both fields are always present after a successful Load(); the cert chain is single-leaf
// (AFIP signs the leaf directly, no intermediate is required for the CMS payload).
type Material struct {
	Certificate *x509.Certificate
	PrivateKey  crypto.PrivateKey
	// Raw DER for re-encoding into the CMS SignedData.
	CertDER []byte
}

// CertProvider supplies the AFIP signing material. Implementations may cache.
type CertProvider interface {
	Load() (*Material, error)
}

// EnvProvider reads PEM-encoded cert + key from env vars (PEM body) or filesystem paths.
// PEM body wins over path. Empty inputs return an error from Load().
type EnvProvider struct {
	CertPEM  string
	CertPath string
	KeyPEM   string
	KeyPath  string

	once sync.Once
	mat  *Material
	err  error
}

func NewEnv(certPEM, certPath, keyPEM, keyPath string) *EnvProvider {
	return &EnvProvider{CertPEM: certPEM, CertPath: certPath, KeyPEM: keyPEM, KeyPath: keyPath}
}

func (p *EnvProvider) Load() (*Material, error) {
	p.once.Do(func() {
		certBytes, err := readPEMSource(p.CertPEM, p.CertPath, "AFIP_CERT")
		if err != nil {
			p.err = err
			return
		}
		keyBytes, err := readPEMSource(p.KeyPEM, p.KeyPath, "AFIP_KEY")
		if err != nil {
			p.err = err
			return
		}
		mat, err := ParseMaterial(certBytes, keyBytes)
		if err != nil {
			p.err = err
			return
		}
		p.mat = mat
	})
	return p.mat, p.err
}

func readPEMSource(pemBody, path, label string) ([]byte, error) {
	if strings.TrimSpace(pemBody) != "" {
		return []byte(pemBody), nil
	}
	if strings.TrimSpace(path) != "" {
		raw, err := os.ReadFile(path)
		if err != nil {
			return nil, fmt.Errorf("%s: read file %s: %w", label, path, err)
		}
		return raw, nil
	}
	return nil, fmt.Errorf("%s not configured (provide PEM body or file path)", label)
}

// ParseMaterial decodes PEM cert + key (RSA or ECDSA, PKCS#1 / PKCS#8 / EC) into a Material struct.
func ParseMaterial(certPEM, keyPEM []byte) (*Material, error) {
	cert, der, err := decodeCertificate(certPEM)
	if err != nil {
		return nil, fmt.Errorf("decode certificate: %w", err)
	}
	key, err := decodePrivateKey(keyPEM)
	if err != nil {
		return nil, fmt.Errorf("decode private key: %w", err)
	}
	if !time.Now().Before(cert.NotAfter) {
		return nil, fmt.Errorf("certificate expired on %s", cert.NotAfter.Format(time.RFC3339))
	}
	return &Material{
		Certificate: cert,
		PrivateKey:  key,
		CertDER:     der,
	}, nil
}

func decodeCertificate(raw []byte) (*x509.Certificate, []byte, error) {
	for {
		block, rest := pem.Decode(raw)
		if block == nil {
			return nil, nil, errors.New("no CERTIFICATE PEM block found")
		}
		if block.Type == "CERTIFICATE" {
			cert, err := x509.ParseCertificate(block.Bytes)
			if err != nil {
				return nil, nil, err
			}
			return cert, block.Bytes, nil
		}
		raw = rest
	}
}

func decodePrivateKey(raw []byte) (crypto.PrivateKey, error) {
	for {
		block, rest := pem.Decode(raw)
		if block == nil {
			return nil, errors.New("no PRIVATE KEY PEM block found")
		}
		switch block.Type {
		case "RSA PRIVATE KEY":
			return x509.ParsePKCS1PrivateKey(block.Bytes)
		case "EC PRIVATE KEY":
			return x509.ParseECPrivateKey(block.Bytes)
		case "PRIVATE KEY":
			k, err := x509.ParsePKCS8PrivateKey(block.Bytes)
			if err != nil {
				return nil, err
			}
			switch k.(type) {
			case *rsa.PrivateKey, *ecdsa.PrivateKey:
				return k, nil
			default:
				return nil, fmt.Errorf("unsupported private key type %T", k)
			}
		}
		raw = rest
	}
}

// StaticProvider returns pre-parsed material (used by tests).
type StaticProvider struct {
	M *Material
}

func (p *StaticProvider) Load() (*Material, error) {
	if p.M == nil {
		return nil, errors.New("static provider: nil material")
	}
	return p.M, nil
}
