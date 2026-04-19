package validation

import (
	"fmt"
	"reflect"
	"regexp"
	"strings"
	"sync"

	"github.com/go-playground/validator/v10"
)

// Matches website/src/app/lib/validations/signupTrial.ts (COMPANY_CODE_RE), after trim/uppercase.
var companyCodeCharsetRe = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)

var (
	validate   *validator.Validate
	initOnce   sync.Once
)

func companyCodeChars(fl validator.FieldLevel) bool {
	s := fl.Field().String()
	if s == "" {
		return true
	}
	return companyCodeCharsetRe.MatchString(s)
}

func InitValidate() {
	initOnce.Do(func() {
		validate = validator.New()
		validate.RegisterTagNameFunc(func(fld reflect.StructField) string {
			name := strings.SplitN(fld.Tag.Get("json"), ",", 2)[0]
			if name == "-" || name == "" {
				return fld.Name
			}
			return name
		})
		if err := validate.RegisterValidation("company_code_chars", companyCodeChars); err != nil {
			panic(fmt.Sprintf("validation: register company_code_chars: %v", err))
		}
	})
}

func Init() {
	InitValidate()
}

func messageForFieldError(fe validator.FieldError) string {
	field := fe.Field()
	tag := fe.Tag()

	switch tag {
	case "required":
		return fmt.Sprintf("%s es requerido", field)
	case "email":
		return fmt.Sprintf("%s debe ser un email válido", field)
	case "min":
		return fmt.Sprintf("%s debe tener al menos %s caracteres", field, fe.Param())
	case "max":
		return fmt.Sprintf("%s debe tener como máximo %s caracteres", field, fe.Param())
	case "oneof":
		return fmt.Sprintf("%s debe ser uno de: %s", field, fe.Param())
	case "company_code_chars":
		return "Usá solo letras, números, guiones o guiones bajos."
	default:
		return fmt.Sprintf("validación fallida para %s: %s", field, tag)
	}
}

func collectStructErrors(s interface{}) (fieldErrors map[string]string, messages []string) {
	InitValidate()

	err := validate.Struct(s)
	if err == nil {
		return nil, nil
	}

	fieldErrors = make(map[string]string)
	for _, err := range err.(validator.ValidationErrors) {
		msg := messageForFieldError(err)
		messages = append(messages, msg)
		key := err.Field()
		if _, exists := fieldErrors[key]; !exists {
			fieldErrors[key] = msg
		}
	}
	return fieldErrors, messages
}

// Struct validates s and returns human-readable messages (backward compatible).
func Struct(s interface{}) []string {
	_, messages := collectStructErrors(s)
	return messages
}

// StructFieldErrors validates s and returns errors keyed by JSON field name when available.
func StructFieldErrors(s interface{}) map[string]string {
	fields, _ := collectStructErrors(s)
	return fields
}
