package models

// LoginRequest is used by POST /auth/login (web and apps).
type LoginRequest struct {
	CompanyCode string `json:"company_code" validate:"required,min=2,max=32,company_code_chars"`
	Username    string `json:"username" validate:"required"`
	Password    string `json:"password" validate:"required"`
	DeviceName  string `json:"device_name" validate:"omitempty"`
}
