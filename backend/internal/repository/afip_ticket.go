package repository

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// AfipTicketRepository persists WSAA TA (Token + Sign) per service so we don't request a
// fresh ticket on every call (AFIP rate-limits this aggressively).
type AfipTicketRepository struct {
	pool *pgxpool.Pool
}

func NewAfipTicketRepository(pool *pgxpool.Pool) *AfipTicketRepository {
	return &AfipTicketRepository{pool: pool}
}

// AfipTicket is one cached WSAA TA row.
type AfipTicket struct {
	Service        string
	Token          string
	Sign           string
	GenerationTime time.Time
	ExpirationTime time.Time
	UpdatedAt      time.Time
}

// Get returns the cached TA for a service or (nil, nil) if absent.
func (r *AfipTicketRepository) Get(ctx context.Context, service string) (*AfipTicket, error) {
	var t AfipTicket
	err := r.pool.QueryRow(ctx, `
		SELECT service, token, sign, generation_time, expiration_time, updated_at
		FROM afip_tickets WHERE service = $1
	`, service).Scan(&t.Service, &t.Token, &t.Sign, &t.GenerationTime, &t.ExpirationTime, &t.UpdatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &t, nil
}

// Upsert stores or replaces the TA for a service.
func (r *AfipTicketRepository) Upsert(ctx context.Context, t AfipTicket) error {
	_, err := r.pool.Exec(ctx, `
		INSERT INTO afip_tickets (service, token, sign, generation_time, expiration_time, updated_at)
		VALUES ($1, $2, $3, $4, $5, NOW())
		ON CONFLICT (service) DO UPDATE SET
			token = EXCLUDED.token,
			sign = EXCLUDED.sign,
			generation_time = EXCLUDED.generation_time,
			expiration_time = EXCLUDED.expiration_time,
			updated_at = NOW()
	`, t.Service, t.Token, t.Sign, t.GenerationTime, t.ExpirationTime)
	return err
}

