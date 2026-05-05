package handlers

import "testing"

func TestReachedWarehouseCap(t *testing.T) {
	pyme := 2
	t.Run("nil max means unlimited", func(t *testing.T) {
		if reachedWarehouseCap(99, nil) {
			t.Fatal("expected unlimited when max is nil")
		}
	})
	t.Run("under cap", func(t *testing.T) {
		if reachedWarehouseCap(1, &pyme) {
			t.Fatal("expected not reached at count=1, max=2")
		}
	})
	t.Run("at cap", func(t *testing.T) {
		if !reachedWarehouseCap(2, &pyme) {
			t.Fatal("expected reached at count=2, max=2")
		}
	})
	t.Run("over cap", func(t *testing.T) {
		if !reachedWarehouseCap(5, &pyme) {
			t.Fatal("expected reached at count=5, max=2")
		}
	})
}

func TestReachedUserCap(t *testing.T) {
	pyme := 3
	if reachedUserCap(2, &pyme) {
		t.Fatal("under cap should pass")
	}
	if !reachedUserCap(3, &pyme) {
		t.Fatal("at cap should be blocked")
	}
	if !reachedUserCap(10, &pyme) {
		t.Fatal("over cap should be blocked")
	}
	if reachedUserCap(99, nil) {
		t.Fatal("nil cap should always pass")
	}
}

func TestCanArchiveWarehouse(t *testing.T) {
	t.Run("refuses last warehouse", func(t *testing.T) {
		ok, reason := canArchiveWarehouse(archiveGuard{totalActive: 1, activeDevices: 0})
		if ok || reason != "last_warehouse" {
			t.Fatalf("expected blocked last_warehouse, got ok=%v reason=%s", ok, reason)
		}
	})
	t.Run("refuses while devices active", func(t *testing.T) {
		ok, reason := canArchiveWarehouse(archiveGuard{totalActive: 3, activeDevices: 2})
		if ok || reason != "active_devices" {
			t.Fatalf("expected blocked active_devices, got ok=%v reason=%s", ok, reason)
		}
	})
	t.Run("allows when other warehouses exist and no active devices", func(t *testing.T) {
		ok, reason := canArchiveWarehouse(archiveGuard{totalActive: 3, activeDevices: 0})
		if !ok || reason != "" {
			t.Fatalf("expected allowed, got ok=%v reason=%s", ok, reason)
		}
	})
	t.Run("refuses last warehouse takes precedence over active devices", func(t *testing.T) {
		ok, reason := canArchiveWarehouse(archiveGuard{totalActive: 1, activeDevices: 2})
		if ok || reason != "last_warehouse" {
			t.Fatalf("expected last_warehouse precedence, got ok=%v reason=%s", ok, reason)
		}
	})
}
