package afip

import (
	"context"
	"time"
)

// repoTicket is the row shape we expect any "RepoTicket"-style store to return.
type repoTicket struct {
	Service        string
	Token          string
	Sign           string
	GenerationTime time.Time
	ExpirationTime time.Time
}

// repoStore is the structural interface the wrapper requires from a SQL-backed ticket repo.
// Implementations live alongside their data source (e.g. repository.AfipTicketRepository).
//
// We deliberately keep this file private (lowercase) so callers go through the public
// adapter helpers (NewTAManagerWithFuncs / NewTAManager). New persistent stores can simply
// construct a TAManager with a custom TAStore.
type repoStore interface {
	Get(ctx context.Context, service string) (*repoTicket, error)
	Upsert(ctx context.Context, t repoTicket) error
}

// FuncTAStore lets callers wire a TA store from a pair of closures, avoiding interface
// adapters when the persistence layer can't (or won't) import the afip package.
//
// This is how main.go bridges *repository.AfipTicketRepository into *TAManager without
// adding an afip dep on the repository package.
type FuncTAStore struct {
	GetFn    func(ctx context.Context, service string) (*StoredTA, error)
	UpsertFn func(ctx context.Context, ta StoredTA) error
}

// Get implements TAStore.
func (f *FuncTAStore) Get(ctx context.Context, service string) (*StoredTA, error) {
	if f == nil || f.GetFn == nil {
		return nil, nil
	}
	return f.GetFn(ctx, service)
}

// Upsert implements TAStore.
func (f *FuncTAStore) Upsert(ctx context.Context, ta StoredTA) error {
	if f == nil || f.UpsertFn == nil {
		return nil
	}
	return f.UpsertFn(ctx, ta)
}
