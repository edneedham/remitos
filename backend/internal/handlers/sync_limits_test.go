package handlers

import "testing"

func TestProjectedNewInboundNotes(t *testing.T) {
	t.Run("counts only new cloud ids plus notes without cloud id", func(t *testing.T) {
		got := projectedNewInboundNotes(4, 2, 1)
		if got != 3 {
			t.Fatalf("expected 3, got %d", got)
		}
	})

	t.Run("never returns negative values", func(t *testing.T) {
		got := projectedNewInboundNotes(1, 5, 0)
		if got != 0 {
			t.Fatalf("expected 0, got %d", got)
		}
	})
}
