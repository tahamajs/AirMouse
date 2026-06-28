package syscmd

var execute func(string) error

// ExecuteSystemCommand runs a system command by name.
// It dispatches to the platform‑specific implementation set during init.
func ExecuteSystemCommand(command string) error {
	if execute == nil {
		return nil
	}
	return execute(command)
}
