package apierror

// Code is the JSON "error" field value (same wire contract as handlers historically used).
type Code string

const (
	InvalidRequest    Code = "INVALID_REQUEST"
	Unauthorized      Code = "UNAUTHORIZED"
	Forbidden         Code = "FORBIDDEN"
	Conflict          Code = "CONFLICT"
	NotFound          Code = "NOT_FOUND"
	InternalError     Code = "INTERNAL_ERROR"
	PaymentRequired   Code = "PAYMENT_REQUIRED"
	TooManyRequests   Code = "TOO_MANY_REQUESTS"
	MethodNotAllowed  Code = "METHOD_NOT_ALLOWED"
)
