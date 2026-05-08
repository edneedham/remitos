package afip

import (
	"context"
	"fmt"
	"sync"
	"time"

	"server/internal/payments/afip/wsaa"
)

// TAStore is the persistence-side of the WSAA ticket cache. *repository.AfipTicketRepository
// implements it, but tests can swap an in-memory implementation.
type TAStore interface {
	Get(ctx context.Context, service string) (*StoredTA, error)
	Upsert(ctx context.Context, ta StoredTA) error
}

// StoredTA mirrors repository.AfipTicket but lives in the afip package so wsaa+manager are
// callable without importing the repository package (avoids cyclic deps in tests).
type StoredTA struct {
	Service        string
	Token          string
	Sign           string
	GenerationTime time.Time
	ExpirationTime time.Time
}

// TAManager fetches and caches WSAA TAs per service. Safe for concurrent use; in-process
// concurrency is serialized per service so we don't race AFIP with parallel login_cms calls.
type TAManager struct {
	WSAA  *wsaa.Client
	Store TAStore
	Now   func() time.Time

	mu     sync.Mutex
	locks  map[string]*sync.Mutex
	memTAs map[string]wsaa.TA
}

// NewTAManager returns a TAManager wired with the given WSAA client + persistent store.
func NewTAManager(client *wsaa.Client, store TAStore) *TAManager {
	return &TAManager{
		WSAA:   client,
		Store:  store,
		Now:    time.Now,
		locks:  make(map[string]*sync.Mutex),
		memTAs: make(map[string]wsaa.TA),
	}
}

// Get returns a fresh TA for service, refreshing via WSAA when the cached entry is expired
// or missing. Concurrent callers for the same service are serialized.
func (m *TAManager) Get(ctx context.Context, service string) (*wsaa.TA, error) {
	if service == "" {
		return nil, fmt.Errorf("ta manager: service required")
	}
	if m.WSAA == nil {
		return nil, fmt.Errorf("ta manager: wsaa client not configured")
	}

	now := m.now()
	if cached, ok := m.memCacheGet(service); ok && !cached.Expired(now) {
		return &cached, nil
	}

	mu := m.serviceLock(service)
	mu.Lock()
	defer mu.Unlock()

	if cached, ok := m.memCacheGet(service); ok && !cached.Expired(now) {
		return &cached, nil
	}

	if m.Store != nil {
		row, err := m.Store.Get(ctx, service)
		if err == nil && row != nil {
			ta := wsaa.TA{
				Service:        row.Service,
				Token:          row.Token,
				Sign:           row.Sign,
				GenerationTime: row.GenerationTime,
				ExpirationTime: row.ExpirationTime,
			}
			if !ta.Expired(now) {
				m.memCacheSet(service, ta)
				return &ta, nil
			}
		}
	}

	fresh, err := m.WSAA.LoginCMS(ctx, service)
	if err != nil {
		return nil, err
	}
	if m.Store != nil {
		_ = m.Store.Upsert(ctx, StoredTA{
			Service:        fresh.Service,
			Token:          fresh.Token,
			Sign:           fresh.Sign,
			GenerationTime: fresh.GenerationTime,
			ExpirationTime: fresh.ExpirationTime,
		})
	}
	m.memCacheSet(service, *fresh)
	return fresh, nil
}

func (m *TAManager) serviceLock(service string) *sync.Mutex {
	m.mu.Lock()
	defer m.mu.Unlock()
	if l, ok := m.locks[service]; ok {
		return l
	}
	l := &sync.Mutex{}
	m.locks[service] = l
	return l
}

func (m *TAManager) memCacheGet(service string) (wsaa.TA, bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	ta, ok := m.memTAs[service]
	return ta, ok
}

func (m *TAManager) memCacheSet(service string, ta wsaa.TA) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.memTAs[service] = ta
}

func (m *TAManager) now() time.Time {
	if m.Now != nil {
		return m.Now()
	}
	return time.Now()
}
