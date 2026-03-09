package repository

import (
	"testing"

	"github.com/google/uuid"
	"server/internal/models"
)

func ptr(s string) *string {
	return &s
}

type UserRepositoryMock struct {
	users map[string]*models.User
}

func NewUserRepositoryMock() *UserRepositoryMock {
	return &UserRepositoryMock{users: make(map[string]*models.User)}
}

func (m *UserRepositoryMock) GetByEmail(email string) (*models.User, error) {
	if user, ok := m.users[email]; ok {
		return user, nil
	}
	return nil, nil
}

func (m *UserRepositoryMock) Create(user *models.User) error {
	if user.Email != nil {
		m.users[*user.Email] = user
	}
	return nil
}

func TestUserRepositoryMock_GetByEmail_NotFound(t *testing.T) {
	repo := NewUserRepositoryMock()

	user, err := repo.GetByEmail("notfound@test.com")
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}
	if user != nil {
		t.Errorf("expected nil user, got %v", user)
	}
}

func TestUserRepositoryMock_GetByEmail_Found(t *testing.T) {
	repo := NewUserRepositoryMock()
	expectedUser := &models.User{
		ID:    uuid.New(),
		Email: ptr("test@test.com"),
		Role:  "admin",
	}
	repo.users["test@test.com"] = expectedUser

	user, err := repo.GetByEmail("test@test.com")
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}
	if user == nil {
		t.Fatal("expected user, got nil")
	}
	if user.Email != nil && *user.Email != *expectedUser.Email {
		t.Errorf("expected email %s, got %s", *expectedUser.Email, *user.Email)
	}
}

func TestUserRepositoryMock_Create(t *testing.T) {
	repo := NewUserRepositoryMock()
	newUser := &models.User{
		ID:    uuid.New(),
		Email: ptr("new@test.com"),
		Role:  "operator",
	}

	err := repo.Create(newUser)
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}

	user, _ := repo.GetByEmail("new@test.com")
	if user == nil {
		t.Fatal("expected user to be created")
	}
	if user.Role != "operator" {
		t.Errorf("expected role operator, got %s", user.Role)
	}
}

func TestUserRepositoryMock_Create_Duplicate(t *testing.T) {
	repo := NewUserRepositoryMock()
	existing := &models.User{
		ID:    uuid.New(),
		Email: ptr("existing@test.com"),
	}
	repo.users["existing@test.com"] = existing

	newUser := &models.User{
		ID:    uuid.New(),
		Email: ptr("existing@test.com"),
	}

	err := repo.Create(newUser)
	if err != nil {
		t.Errorf("expected no error (mock doesn't prevent duplicates), got %v", err)
	}
}
