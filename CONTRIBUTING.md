# Contributing to AirMouse

Thank you for your interest in contributing to **AirMouse**! We welcome bug reports, feature enhancements, documentation improvements, and architectural optimizations from the community.

Please take a few moments to review these guidelines before submitting a pull request or filing an issue.

---

## 📜 Code of Conduct

We are committed to providing a friendly, safe, and welcoming environment for all contributors. Please treat everyone with respect, constructive feedback, and professionalism.

---

## 🛠️ Development Setup & Workflow

### 1. Fork & Clone
```bash
git clone https://github.com/tahamajs/AirMouse.git
cd AirMouse
```

### 2. Environment Prerequisites
- **Go**: 1.22+ or 1.23+ (`go version`)
- **Python**: 3.10+ (`python3 --version`)
- **Java / JDK**: OpenJDK 17 (`java -version`)
- **Android SDK**: API 34+ build-tools

### 3. Install Git Pre-Commit Guardrails
AirMouse enforces strict file size policies to prevent large binaries from bloating the repository history. Run:
```bash
chmod +x scripts/install_hooks.sh
./scripts/install_hooks.sh
```
The pre-commit hook automatically rejects any commit containing individual files exceeding **5 MB**.

---

## 📐 Architecture & Coding Standards

1. **Strict Protocol V2 Adherence**:
   - Every network message must conform to the canonical `Protocol V2` schema (`v: 2`, `seq`, `ts`, `id`, `payload`).
   - Run `./scripts/check_docs.sh` to ensure documentation and message catalogs stay synchronized with the codebase.

2. **Mathematical Precision & Signal Processing**:
   - Any modifications to the **One Euro Filter**, **Kalman Filter**, or **ZUPT** (Zero Velocity Update) algorithms must account for:
     - Non-positive or zero delta time (`dt <= 0`).
     - Division by zero and NaN / Inf float propagation.
     - Velocity cutoff frequency bounding.

3. **Concurrency & Thread Safety**:
   - In Python: Decouple asyncio network loops from OS actuator drivers via `queue.Queue`.
   - In Go: Protect mutable shared state with `sync.RWMutex` or channel messaging. Prevent lock inversion or recursive mutex acquisition.
   - In Kotlin/Android: Tie asynchronous operations to lifecycle-aware CoroutineScopes (`viewModelScope`).

4. **Zero-Placeholder Policy**:
   - Never commit placeholder code (`TODO`, `...`, `pass`, empty stubs). All implementations must be fully realized and documented.

---

## 🧪 Testing Requirements

Every Pull Request must pass all relevant test suites before review:

```bash
# 1. Run Python test suite
cd pc/airmouse_py
pytest

# 2. Run Go server test suite
cd ../airmouse_go_new
go test -v ./...

# 3. Run Android unit tests
cd ../../android
./gradlew testDebugUnitTest

# 4. Check Protocol documentation parity
cd ..
./scripts/check_docs.sh
```

---

## 🔀 Pull Request Process

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Follow [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat(...)`: New feature or capability
   - `fix(...)`: Bug fix
   - `docs(...)`: Documentation changes
   - `test(...)`: Adding or updating test cases
   - `refactor(...)`: Code change that neither fixes a bug nor adds a feature
3. Rebase onto latest `main` before submitting:
   ```bash
   git fetch origin
   git rebase origin/main
   ```
4. Push your branch and open a Pull Request against `main`. Provide a descriptive summary, reproduction steps (for bug fixes), or architectural justification (for major features).

---

## 💬 Getting Help
If you have questions, reach out by opening a Discussion or an Issue in the GitHub repository.
