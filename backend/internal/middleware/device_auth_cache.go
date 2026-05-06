package middleware

import (
	"sync"
	"time"

	"github.com/google/uuid"
)

// Short TTL caches avoid repeated device / warehouse lookups on hot mobile paths.
// Stale reads are bounded by TTL if roles or device assignments change mid-session.
const (
	deviceAuthPassTTL   = 30 * time.Second
	deviceSnapshotTTL   = 45 * time.Second
	cachePruneThreshold = 2048
)

var (
	authPassMu sync.Mutex
	authPassAt = make(map[string]time.Time)

	snapMu  sync.Mutex
	devSnap = make(map[uuid.UUID]deviceSnapEntry)
)

type deviceSnapEntry struct {
	companyID   uuid.UUID
	warehouseID uuid.UUID
	until       time.Time
}

func authPassKey(userID, deviceID, companyID uuid.UUID) string {
	return userID.String() + "|" + deviceID.String() + "|" + companyID.String()
}

func tryAuthPass(userID, deviceID, companyID uuid.UUID) bool {
	key := authPassKey(userID, deviceID, companyID)
	now := time.Now()
	authPassMu.Lock()
	defer authPassMu.Unlock()
	exp, ok := authPassAt[key]
	if !ok || now.After(exp) {
		if ok {
			delete(authPassAt, key)
		}
		return false
	}
	return true
}

func recordAuthPass(userID, deviceID, companyID uuid.UUID) {
	key := authPassKey(userID, deviceID, companyID)
	authPassMu.Lock()
	defer authPassMu.Unlock()
	if len(authPassAt) > cachePruneThreshold {
		now := time.Now()
		for k, exp := range authPassAt {
			if now.After(exp) {
				delete(authPassAt, k)
			}
		}
	}
	authPassAt[key] = time.Now().Add(deviceAuthPassTTL)
}

func tryDeviceSnapshot(deviceID uuid.UUID) (companyID, warehouseID uuid.UUID, ok bool) {
	snapMu.Lock()
	defer snapMu.Unlock()
	e, hit := devSnap[deviceID]
	if !hit || time.Now().After(e.until) {
		if hit {
			delete(devSnap, deviceID)
		}
		return uuid.UUID{}, uuid.UUID{}, false
	}
	return e.companyID, e.warehouseID, true
}

func recordDeviceSnapshot(deviceID, companyID, warehouseID uuid.UUID) {
	snapMu.Lock()
	defer snapMu.Unlock()
	if len(devSnap) > cachePruneThreshold {
		now := time.Now()
		for id, e := range devSnap {
			if now.After(e.until) {
				delete(devSnap, id)
			}
		}
	}
	devSnap[deviceID] = deviceSnapEntry{
		companyID:   companyID,
		warehouseID: warehouseID,
		until:       time.Now().Add(deviceSnapshotTTL),
	}
}

// resetDeviceAuthCaches clears process-local caches (for tests).
func resetDeviceAuthCaches() {
	authPassMu.Lock()
	authPassAt = make(map[string]time.Time)
	authPassMu.Unlock()

	snapMu.Lock()
	devSnap = make(map[uuid.UUID]deviceSnapEntry)
	snapMu.Unlock()
}
