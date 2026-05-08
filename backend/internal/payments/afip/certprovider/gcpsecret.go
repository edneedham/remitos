package certprovider

import (
	"context"
	"fmt"
	"strings"
	"sync"
	"time"

	secretmanager "cloud.google.com/go/secretmanager/apiv1"
	"cloud.google.com/go/secretmanager/apiv1/secretmanagerpb"
)

// GCPSecretProvider lazily fetches PEM cert + key from GCP Secret Manager and caches the parsed
// Material in memory. Callers should construct one provider per process; Load() is safe for
// concurrent use.
//
// CertSecretName / KeySecretName accept the full resource name format:
//
//	projects/<project>/secrets/<secret>/versions/<version>
//
// Use "latest" as the version segment to always fetch the active version.
type GCPSecretProvider struct {
	CertSecretName string
	KeySecretName  string
	// CacheTTL bounds how long we hold parsed material in memory before re-fetching from
	// Secret Manager. Zero defaults to 6 hours.
	CacheTTL time.Duration

	mu       sync.Mutex
	cached   *Material
	loadedAt time.Time
}

func NewGCPSecret(certName, keyName string) *GCPSecretProvider {
	return &GCPSecretProvider{
		CertSecretName: strings.TrimSpace(certName),
		KeySecretName:  strings.TrimSpace(keyName),
	}
}

func (p *GCPSecretProvider) Load() (*Material, error) {
	if p.CertSecretName == "" || p.KeySecretName == "" {
		return nil, fmt.Errorf("gcp secret provider: cert and key secret names are required")
	}
	p.mu.Lock()
	defer p.mu.Unlock()
	ttl := p.CacheTTL
	if ttl <= 0 {
		ttl = 6 * time.Hour
	}
	if p.cached != nil && time.Since(p.loadedAt) < ttl {
		return p.cached, nil
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	client, err := secretmanager.NewClient(ctx)
	if err != nil {
		return nil, fmt.Errorf("gcp secret provider: new client: %w", err)
	}
	defer client.Close()

	certBytes, err := accessSecret(ctx, client, p.CertSecretName)
	if err != nil {
		return nil, fmt.Errorf("gcp secret provider: cert: %w", err)
	}
	keyBytes, err := accessSecret(ctx, client, p.KeySecretName)
	if err != nil {
		return nil, fmt.Errorf("gcp secret provider: key: %w", err)
	}
	mat, err := ParseMaterial(certBytes, keyBytes)
	if err != nil {
		return nil, err
	}
	p.cached = mat
	p.loadedAt = time.Now()
	return mat, nil
}

func accessSecret(ctx context.Context, c *secretmanager.Client, name string) ([]byte, error) {
	req := &secretmanagerpb.AccessSecretVersionRequest{Name: name}
	resp, err := c.AccessSecretVersion(ctx, req)
	if err != nil {
		return nil, err
	}
	if resp.Payload == nil {
		return nil, fmt.Errorf("empty payload")
	}
	return resp.Payload.Data, nil
}
