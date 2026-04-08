package main

import (
	"fmt"
	"golang.org/x/crypto/bcrypt"
)

func main() {
	password := "demo1234"
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Password: %s\n", password)
	fmt.Printf("Bcrypt Hash: %s\n", string(hash))

	// Verify it works
	err = bcrypt.CompareHashAndPassword(hash, []byte(password))
	if err != nil {
		fmt.Println("Verification FAILED!")
	} else {
		fmt.Println("Verification OK!")
	}
}
