package validation

import (
	"fmt"

	"github.com/go-playground/validator/v10"
)

var validate *validator.Validate

func Init() {
	validate = validator.New()
}

func InitValidate() {
	if validate == nil {
		validate = validator.New()
	}
}

func Struct(s interface{}) []string {
	InitValidate()

	err := validate.Struct(s)
	if err == nil {
		return nil
	}

	var errors []string
	for _, err := range err.(validator.ValidationErrors) {
		field := err.Field()
		tag := err.Tag()

		switch tag {
		case "required":
			errors = append(errors, fmt.Sprintf("%s es requerido", field))
		case "email":
			errors = append(errors, fmt.Sprintf("%s debe ser un email válido", field))
		case "min":
			errors = append(errors, fmt.Sprintf("%s debe tener al menos %s caracteres", field, err.Param()))
		case "max":
			errors = append(errors, fmt.Sprintf("%s debe tener como máximo %s caracteres", field, err.Param()))
		case "oneof":
			errors = append(errors, fmt.Sprintf("%s debe ser uno de: %s", field, err.Param()))
		default:
			errors = append(errors, fmt.Sprintf("validación fallida para %s: %s", field, tag))
		}
	}

	return errors
}
