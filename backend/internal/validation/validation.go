package validation

import (
	"fmt"
	"reflect"
	"regexp"
	"strings"
	"sync"

	"github.com/go-playground/validator/v10"
	"server/internal/cuit"
)

// Matches website/src/app/lib/validations/signup.ts (COMPANY_CODE_RE), after trim/uppercase.
var companyCodeCharsetRe = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)

var (
	validate *validator.Validate
	initOnce sync.Once
)

func companyCodeChars(fl validator.FieldLevel) bool {
	s := fl.Field().String()
	if s == "" {
		return true
	}
	return companyCodeCharsetRe.MatchString(s)
}

// cuit_ar: exactly 11 digits with valid Argentine CUIT/CUIL verifier digit.
func cuitAR(fl validator.FieldLevel) bool {
	s := fl.Field().String()
	if s == "" {
		return true
	}
	return cuit.ValidChecksum(s)
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
		if err := validate.RegisterValidation("cuit_ar", cuitAR); err != nil {
			panic(fmt.Sprintf("validation: register cuit_ar: %v", err))
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
		if field == "company_cuit" {
			return "El CUIT de la empresa es obligatorio."
		}
		switch field {
		case "delivery_notes_per_day_band":
			return "Elegí cuántos remitos procesás por día en promedio."
		case "processing_mode":
			return "Elegí cómo procesás los remitos hoy."
		case "warehouse_count":
			return "Indicá cuántos depósitos tenés."
		}
		return fmt.Sprintf("%s es requerido", field)
	case "email":
		return fmt.Sprintf("%s debe ser un email válido", field)
	case "min":
		return fmt.Sprintf("%s debe tener al menos %s caracteres", field, fe.Param())
	case "max":
		switch field {
		case "logistics_pain_points":
			return "El texto sobre problemas de logística es demasiado largo (máximo " + fe.Param() + " caracteres)."
		case "email":
			return "El correo electrónico es demasiado largo."
		}
		return fmt.Sprintf("%s debe tener como máximo %s caracteres", field, fe.Param())
	case "oneof":
		switch field {
		case "delivery_notes_per_day_band":
			return "Elegí una opción válida para remitos por día."
		case "processing_mode":
			return "Elegí una opción válida para cómo procesás los remitos."
		}
		return fmt.Sprintf("%s debe ser uno de: %s", field, fe.Param())
	case "company_code_chars":
		return "Usá solo letras, números, guiones o guiones bajos."
	case "cuit_ar":
		return "El CUIT no es válido (revisá los 11 dígitos y el dígito verificador)."
	case "gte", "lte":
		switch field {
		case "warehouse_count":
			if tag == "gte" {
				return "La cantidad de depósitos no puede ser negativa."
			}
			return "La cantidad de depósitos es demasiado alta."
		default:
			return fmt.Sprintf("%s no cumple el rango permitido", field)
		}
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
