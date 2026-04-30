package onboarding

import (
	"database/sql"
	"testing"
	"time"
)

func TestPickNudgeStage(t *testing.T) {
	activation := time.Date(2026, 4, 1, 12, 0, 0, 0, time.UTC)

	t.Run("empty when age under 10m", func(t *testing.T) {
		now := activation.Add(5 * time.Minute)
		got := PickNudgeStage(now, activation, sql.NullTime{}, sql.NullTime{}, sql.NullTime{})
		if got != "" {
			t.Fatalf("got %q want empty", got)
		}
	})

	t.Run("setup after 10m", func(t *testing.T) {
		now := activation.Add(11 * time.Minute)
		got := PickNudgeStage(now, activation, sql.NullTime{}, sql.NullTime{}, sql.NullTime{})
		if got != "setup" {
			t.Fatalf("got %q want setup", got)
		}
	})

	t.Run("day1 after 24h when setup already sent", func(t *testing.T) {
		now := activation.Add(25 * time.Hour)
		setup := sql.NullTime{Valid: true, Time: activation.Add(15 * time.Minute)}
		got := PickNudgeStage(now, activation, setup, sql.NullTime{}, sql.NullTime{})
		if got != "day1" {
			t.Fatalf("got %q want day1", got)
		}
	})

	t.Run("day3 wins when overdue and nothing sent", func(t *testing.T) {
		now := activation.Add(73 * time.Hour)
		got := PickNudgeStage(now, activation, sql.NullTime{}, sql.NullTime{}, sql.NullTime{})
		if got != "day3" {
			t.Fatalf("got %q want day3", got)
		}
	})

	t.Run("nothing when all sent", func(t *testing.T) {
		now := activation.Add(100 * time.Hour)
		ts := func(tt time.Time) sql.NullTime {
			return sql.NullTime{Valid: true, Time: tt}
		}
		got := PickNudgeStage(now, activation,
			ts(activation.Add(time.Minute)),
			ts(activation.Add(24*time.Hour)),
			ts(activation.Add(72*time.Hour)),
		)
		if got != "" {
			t.Fatalf("got %q want empty", got)
		}
	})
}
