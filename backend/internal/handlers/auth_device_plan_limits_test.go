package handlers

import "testing"

func TestBlockNewDeviceForWarehousePlan(t *testing.T) {
	t.Run("trial blocks when warehouse already has an active device", func(t *testing.T) {
		block, msg := blockNewDeviceForWarehousePlan("trial", 1)
		if !block || msg == "" {
			t.Fatalf("expected block, got block=%v msg=%q", block, msg)
		}
	})

	t.Run("pyme blocks when warehouse already has an active device", func(t *testing.T) {
		block, msg := blockNewDeviceForWarehousePlan("pyme", 1)
		if !block || msg == "" {
			t.Fatalf("expected block, got block=%v msg=%q", block, msg)
		}
	})

	t.Run("empresa allows multiple actives", func(t *testing.T) {
		block, _ := blockNewDeviceForWarehousePlan("empresa", 5)
		if block {
			t.Fatal("expected empresa not to block")
		}
	})

	t.Run("allows first active device", func(t *testing.T) {
		block, _ := blockNewDeviceForWarehousePlan("trial", 0)
		if block {
			t.Fatal("expected first device allowed")
		}
	})
}
