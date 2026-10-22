# Project Setup Template

Full-stack Kotlin app template: Compose Multiplatform/KMP client plus Ktor backend.

For current functionality and source locations, read [AGENTS.md](AGENTS.md) and
[docs/AI_AGENT_GUIDE.md](docs/AI_AGENT_GUIDE.md). Those documents are authoritative;
do not assume authentication, offline storage, or release automation are placeholders.

## Start and iterate

```bash
./setup.sh --name Focus --package dev.example.focus \
  --features auth,offline,backend --dry-run
make doctor
make check
make ui
```

A project made from this template is not ready to ship until `setup.sh` has run (doctor
warns otherwise) and [APP_CHECKLIST.md](APP_CHECKLIST.md) is worked through: per-app
features such as Share app, endpoints, legal links, GitHub secrets, and store setup. Those
items are reminders, not implemented features. When the user asks what is left before
launch, start from that checklist and `make doctor` (which lists missing release secrets).

`app.properties` is the single identity/version/endpoint/capability/product source.
Use `setup.sh` for package renaming, `bump-version.sh` for versions, and
`scripts/sync-app-config.sh` after editing `app.properties` (it mirrors the iOS xcconfig).

## Structure

| Path | Purpose |
|---|---|
| `mobile/` | Android, iOS, desktop gallery, shared domain/data/UI |
| `backend/` | Ktor, JWT, Exposed, Flyway, PostgreSQL/H2 |
| `scripts/` | Doctor, config sync, resource checks, initializer and feature generator tests |
| `.github/workflows/` | CI, device smoke, CodeQL, transactional Mobile CD |
| `docs/` | Workflow, releasing, Firebase, billing, and agent guides |

Read `mobile/AGENTS.md` or `backend/AGENTS.md` before editing that module.

## Quality gates

`make quick` is the inner loop (fast checks for modules changed on the branch). Before
declaring work done, run the full gates:

```bash
make check-backend   # cd backend && ./gradlew test detekt ktlintCheck koverVerify
make check-mobile    # cd mobile && ./gradlew test detekt ktlintCheck :androidApp:lintDebug \
                     #   :composeApp:lintDebug :androidApp:assembleDebug :composeApp:compileKotlinDesktop \
                     #   :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:verifySqlDelightMigration \
                     #   :composeApp:koverVerifyUnitTests
make check-template  # initializer/generator contracts, xcconfig sync, resources, architecture
make lint-repo       # actionlint, ShellCheck, typos, hadolint, SwiftLint, architecture contracts
make format          # ktlint and SwiftLint auto-fixes
make coverage        # Kover HTML reports and totals for both builds
```

`make bootstrap` installs the local tools (Brewfile) and the Git hooks. Edits are checked as you
make them: `scripts/agent-hook.sh` (wired in `.claude/settings.json` and `.codex/hooks.json`) blocks
edits to generated files, credentials, lint baselines, and migrations main already has, formats and
lints each edited file (ktlint, SwiftLint, ShellCheck, actionlint, typos), and blocks `--no-verify`,
plain force pushes, and admin merges. Fix what it reports; do not work around it.

Pull requests (including AI-agent ones) follow "Working through pull requests" in
[AGENTS.md](AGENTS.md): CI and the AI review must pass, changed lines need 80% test coverage,
every review thread is resolved, and `.github/CODEOWNERS` paths need the owner's approval.
Never disable a check, rule, or test, or add a lint baseline entry, to make a change pass.

Use conventional commits (they become the release notes). Never add co-author trailers.
Never commit secrets, signing files, `.env`, or production credentials.
