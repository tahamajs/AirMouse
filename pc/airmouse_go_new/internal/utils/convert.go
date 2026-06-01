package utils

import "strconv"

func AtoiSafe(s string, defaultValue int) int {
	val, err := strconv.Atoi(s)
	if err != nil {
		return defaultValue
	}
	return val
}

func Itoa(i int) string {
	return strconv.Itoa(i)
}
