package models

// ForgotPasswordRequest is POST /auth/forgot-password (same username resolution as login).
type ForgotPasswordRequest struct {
	CompanyCode string `json:"company_code" validate:"required,min=2,max=32,company_code_chars"`
	Username    string `json:"username" validate:"required"`
}

// ResetPasswordRequest completes a reset using the token from email.
type ResetPasswordRequest struct {
	Token       string `json:"token" validate:"required"`
	NewPassword string `json:"new_password" validate:"required,min=8,max=72"`
}

// ChangePasswordRequest is POST /auth/change-password (authenticated).
type ChangePasswordRequest struct {
	CurrentPassword string `json:"current_password" validate:"required"`
	NewPassword     string `json:"new_password" validate:"required,min=8,max=72"`
}
