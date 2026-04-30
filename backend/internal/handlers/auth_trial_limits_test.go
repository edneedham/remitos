package handlers

import "testing"

func TestShouldBlockTrialDeviceRegistration(t *testing.T) {
	t.Run("blocks when new device and warehouse already has one", func(t *testing.T) {
		if !shouldBlockTrialDeviceRegistration(false, 1) {
			t.Fatal("expected device registration to be blocked")
		}
	})

	t.Run("allows when device already exists", func(t *testing.T) {
		if shouldBlockTrialDeviceRegistration(true, 5) {
			t.Fatal("expected existing device update to be allowed")
		}
	})

	t.Run("allows when warehouse has no devices", func(t *testing.T) {
		if shouldBlockTrialDeviceRegistration(false, 0) {
			t.Fatal("expected first device in warehouse to be allowed")
		}
	})
}
