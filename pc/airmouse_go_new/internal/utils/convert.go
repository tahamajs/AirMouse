package utils

import "strconv"

// AtoiSafe converts a string to int, returning default on error.
func AtoiSafe(s string, defaultValue int) int {
    val, err := strconv.Atoi(s)
    if err != nil {
        return defaultValue
    }
    return val
}

// Itoa is a wrapper for strconv.Itoa.
func Itoa(i int) string {
    return strconv.Itoa(i)
}