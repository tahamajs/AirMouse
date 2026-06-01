# Contributing to Air Mouse Go Server

Thank you for considering contributing! We welcome bug reports, feature requests, and pull requests.

## How to Contribute

### Reporting Bugs
- Use the [GitHub Issues](https://github.com/tahamajs/airmouse-go/issues) page.
- Describe the problem, steps to reproduce, and your environment (OS, Go version, etc.).

### Suggesting Enhancements
- Open an issue with the label `enhancement`.
- Explain the use case and how it would benefit users.

### Pull Requests
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Make your changes.
4. Run tests: `go test -v ./...`
5. Run lint: `golangci-lint run` (if you have it installed).
6. Commit with a clear message (`git commit -m 'Add amazing feature'`).
7. Push to your fork (`git push origin feature/amazing-feature`).
8. Open a Pull Request against the `main` branch.

## Development Setup

```bash
git clone https://github.com/tahamajs/airmouse-go.git
cd airmouse-go
go mod download
go build -o airmouse-server ./cmd/airmouse-server