package repository

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"server/internal/models"
)

type DeviceRepositoryMock struct {
	devices map[string]*models.Device
}

func NewDeviceRepositoryMock() *DeviceRepositoryMock {
	return &DeviceRepositoryMock{devices: make(map[string]*models.Device)}
}

func (m *DeviceRepositoryMock) GetByUUID(companyID uuid.UUID, deviceUUID string) (*models.Device, error) {
	if device, ok := m.devices[deviceUUID]; ok {
		return device, nil
	}
	return nil, nil
}

func (m *DeviceRepositoryMock) Create(device *models.Device) error {
	m.devices[device.DeviceUUID] = device
	return nil
}

func (m *DeviceRepositoryMock) UpdateLastSeen(deviceID uuid.UUID) error {
	for _, device := range m.devices {
		if device.ID == deviceID {
			now := time.Now()
			device.LastSeenAt = &now
			return nil
		}
	}
	return nil
}

func TestDeviceRepositoryMock_GetByUUID_NotFound(t *testing.T) {
	repo := NewDeviceRepositoryMock()

	device, err := repo.GetByUUID(uuid.New(), "nonexistent")
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}
	if device != nil {
		t.Errorf("expected nil device, got %v", device)
	}
}

func TestDeviceRepositoryMock_GetByUUID_Found(t *testing.T) {
	repo := NewDeviceRepositoryMock()
	deviceID := uuid.New()
	warehouseID := uuid.New()
	companyID := uuid.New()
	now := time.Now()
	expectedDevice := &models.Device{
		ID:           deviceID,
		CompanyID:    companyID,
		WarehouseID:  warehouseID,
		DeviceUUID:   "abc-123",
		Platform:     "android",
		Status:       "active",
		RegisteredAt: now,
	}
	repo.devices["abc-123"] = expectedDevice

	device, err := repo.GetByUUID(companyID, "abc-123")
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}
	if device == nil {
		t.Fatal("expected device, got nil")
	}
	if device.DeviceUUID != "abc-123" {
		t.Errorf("expected device_uuid abc-123, got %s", device.DeviceUUID)
	}
}

func TestDeviceRepositoryMock_Create(t *testing.T) {
	repo := NewDeviceRepositoryMock()
	newDeviceID := uuid.New()
	warehouseID := uuid.New()
	companyID := uuid.New()
	now := time.Now()
	newDevice := &models.Device{
		ID:           newDeviceID,
		CompanyID:    companyID,
		WarehouseID:  warehouseID,
		DeviceUUID:   "new-device-uuid",
		Platform:     "android",
		Status:       "pending",
		RegisteredAt: now,
	}

	err := repo.Create(newDevice)
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}

	device, _ := repo.GetByUUID(companyID, "new-device-uuid")
	if device == nil {
		t.Fatal("expected device to be created")
	}
	if device.ID != newDevice.ID {
		t.Errorf("expected device id %s, got %s", newDevice.ID, device.ID)
	}
}

func TestDeviceRepositoryMock_UpdateLastSeen(t *testing.T) {
	repo := NewDeviceRepositoryMock()
	deviceID := uuid.New()
	warehouseID := uuid.New()
	companyID := uuid.New()
	oldTime := time.Now().Add(-time.Hour)
	device := &models.Device{
		ID:           deviceID,
		CompanyID:    companyID,
		WarehouseID:  warehouseID,
		DeviceUUID:   "test-device-uuid",
		Platform:     "android",
		Status:       "active",
		RegisteredAt: oldTime,
		LastSeenAt:   &oldTime,
	}
	repo.devices["test-device-uuid"] = device

	err := repo.UpdateLastSeen(deviceID)
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}

	updated, _ := repo.GetByUUID(companyID, "test-device-uuid")
	if updated.LastSeenAt.Before(oldTime) {
		t.Error("expected last_seen_at to be updated to a newer time")
	}
}

func TestDeviceRepositoryMock_UpdateLastSeen_NotFound(t *testing.T) {
	repo := NewDeviceRepositoryMock()

	err := repo.UpdateLastSeen(uuid.New())
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}
}
