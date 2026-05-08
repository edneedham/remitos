package afip

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"crypto/x509/pkix"
	"math/big"
	"testing"
	"time"

	"server/internal/payments/afip/certprovider"
)

// newDevCertProvider mints a fresh self-signed RSA cert + key for use in WSAA / TA manager
// tests. The cert is not trusted by AFIP (or anything); it's purely a vehicle for exercising
// the CMS signing path against fake httptest servers.
func newDevCertProvider(t *testing.T) certprovider.CertProvider {
	t.Helper()
	priv, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("gen key: %v", err)
	}
	tmpl := &x509.Certificate{
		SerialNumber: big.NewInt(1),
		Subject:      pkix.Name{CommonName: "afip-test"},
		NotBefore:    time.Now().Add(-time.Hour),
		NotAfter:     time.Now().Add(24 * time.Hour),
		KeyUsage:     x509.KeyUsageDigitalSignature | x509.KeyUsageKeyEncipherment,
		ExtKeyUsage:  []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth},
	}
	der, err := x509.CreateCertificate(rand.Reader, tmpl, tmpl, &priv.PublicKey, priv)
	if err != nil {
		t.Fatalf("create cert: %v", err)
	}
	cert, err := x509.ParseCertificate(der)
	if err != nil {
		t.Fatalf("parse cert: %v", err)
	}
	return &certprovider.StaticProvider{
		M: &certprovider.Material{
			Certificate: cert,
			PrivateKey:  priv,
			CertDER:     der,
		},
	}
}
