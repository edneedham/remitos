package billing

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"server/internal/models"
)

func TestPadronTaxSnapshotFresh(t *testing.T) {
	t.Parallel()
	id := uuid.MustParse("11111111-1111-1111-1111-111111111111")
	t165 := time.Date(2026, 6, 1, 12, 0, 0, 0, time.UTC)
	ttl := 7 * 24 * time.Hour

	t.Run("fresh", func(t *testing.T) {
		t.Parallel()
		synced := t165.Add(-24 * time.Hour)
		co := &models.Company{
			ID:             id,
			CondicionIVA:   "RESPONSABLE_INSCRIPTO",
			PadronSyncedAt: &synced,
		}
		if !padronTaxSnapshotFresh(co, ttl, t165) {
			t.Fatal("expected fresh within TTL")
		}
	})

	t.Run("stale", func(t *testing.T) {
		t.Parallel()
		synced := t165.Add(-200 * time.Hour)
		co := &models.Company{
			ID:             id,
			CondicionIVA:   "RESPONSABLE_INSCRIPTO",
			PadronSyncedAt: &synced,
		}
		if padronTaxSnapshotFresh(co, ttl, t165) {
			t.Fatal("expected stale past TTL")
		}
	})

	t.Run("zero_ttl_always_miss", func(t *testing.T) {
		t.Parallel()
		synced := t165.Add(-1 * time.Hour)
		co := &models.Company{
			ID:             id,
			CondicionIVA:   "RESPONSABLE_INSCRIPTO",
			PadronSyncedAt: &synced,
		}
		if padronTaxSnapshotFresh(co, 0, t165) {
			t.Fatal("expected miss when ttl=0")
		}
	})

	t.Run("missing_condicion_miss", func(t *testing.T) {
		t.Parallel()
		synced := t165.Add(-1 * time.Hour)
		co := &models.Company{
			ID:             id,
			CondicionIVA:   "",
			PadronSyncedAt: &synced,
		}
		if padronTaxSnapshotFresh(co, ttl, t165) {
			t.Fatal("expected miss without condicion IVA")
		}
	})

	t.Run("nil_sync_miss", func(t *testing.T) {
		t.Parallel()
		co := &models.Company{ID: id, CondicionIVA: "MONOTRIBUTO"}
		if padronTaxSnapshotFresh(co, ttl, t165) {
			t.Fatal("expected miss without padron_synced_at")
		}
	})
}
