# Common commands. Run `make` (or `make help`) to list them.

DB_NAME := remit
PACKAGE := $(shell awk -F= '$$1=="PACKAGE_NAME" { print $$2 }' app.properties)
DB_ENV := DATABASE_URL=jdbc:postgresql://localhost:5432/$(DB_NAME) DATABASE_USER=dev DATABASE_PASSWORD=dev
MOBILE_GATES := test detekt ktlintCheck :androidApp:lintDebug :composeApp:lintDebug \
	:androidApp:assembleDebug :composeApp:compileKotlinDesktop \
	:composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:verifySqlDelightMigration \
	:composeApp:koverVerifyUnitTests
BACKEND_GATES := test detekt ktlintCheck koverVerify

.DEFAULT_GOAL := help
.PHONY: help setup bootstrap doctor app-checklist setup-hooks ui ui-hot run backend backend-h2 \
	db db-start db-stop db-logs db-shell db-reset \
	mobile apk apk-install apk-release ios ios-app ios-run feature \
	test test-backend test-mobile test-mobile-fast test-watch lint lint-repo lint-backend lint-mobile format \
	quick quick-all check-template check check-backend check-mobile coverage \
	build build-backend build-mobile jar clean loc \
	version release-patch release-minor release-major release-notes protect-main

help: ## Show this help
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ---------------------------------------------------------------- Setup
setup: ## Show initializer usage (./setup.sh --help)
	@./setup.sh --help

bootstrap: ## Install local lint/hook tools (Brewfile), Git hooks, then run doctor
	@if command -v brew >/dev/null; then brew bundle --file=Brewfile; \
		else echo 'Homebrew not found: install the tools in Brewfile, or on Linux run scripts/ci/install-tools.sh DIR'; fi
	@./setup-hooks.sh
	@./scripts/doctor.sh

doctor: ## Check local toolchains and template configuration
	@./scripts/doctor.sh

app-checklist: ## Show app-specific work to finish before launch
	@cat APP_CHECKLIST.md

setup-hooks: ## Install Git pre-commit hooks
	@./setup-hooks.sh

# ---------------------------------------------------------------- Run
ui: ## Run the desktop component gallery (shared Compose UI)
	@cd mobile && ./gradlew :composeApp:run

ui-hot: ## Run the desktop gallery with Compose Hot Reload
	@cd mobile && ./gradlew :composeApp:hotRunDesktop --mainClass com.asterixorobelix.remit.gallery.ComponentGalleryKt

run: db-start backend ## Start PostgreSQL and the backend against it

backend: ## Start the backend against the local PostgreSQL (make db first)
	@cd backend && $(DB_ENV) APP_ENVIRONMENT=development ./gradlew run

backend-h2: ## Start the backend with an in-memory H2 database (no Docker)
	@cd backend && APP_ENVIRONMENT=development ./gradlew run

# ---------------------------------------------------------------- Database
db: db-start ## Start PostgreSQL (Docker)

db-start:
	@docker compose up -d --wait db

db-stop: ## Stop Docker services
	@docker compose down

db-logs: ## Follow PostgreSQL logs
	@docker compose logs -f db

db-shell: ## Open a psql shell
	@docker compose exec db psql -U dev -d $(DB_NAME)

db-reset: ## Delete all local database data and restart PostgreSQL
	@docker compose down -v
	@docker compose up -d --wait db

# ---------------------------------------------------------------- Mobile
mobile: apk-install ## Build, install, and launch the Android debug app
	@adb shell monkey -p $(PACKAGE).debug -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1

apk: ## Build the Android debug APK
	@cd mobile && ./gradlew :androidApp:assembleDebug
	@ls -lh mobile/androidApp/build/outputs/apk/debug/*.apk 2>/dev/null || true

apk-install:
	@cd mobile && ./gradlew :androidApp:installDebug

apk-release: ## Build the Android release APK (R8; signed if keystore.properties exists)
	@cd mobile && ./gradlew :androidApp:assembleRelease

ios: ## Link the iOS simulator framework
	@cd mobile && ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

ios-run: ## Build, install, and launch on the booted (or first) iOS simulator
	@./scripts/ios-run.sh

ios-app: ## Build the full iOS simulator app with Xcode (unsigned)
	@xcodebuild -project mobile/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
		-configuration Debug CODE_SIGNING_ALLOWED=NO build

feature: ## Scaffold and wire a mobile feature: make feature NAME=Bookmarks
	@test -n "$(NAME)" || { echo 'Usage: make feature NAME=PascalCaseName'; exit 2; }
	@./scripts/create-feature.sh $(NAME)

# ---------------------------------------------------------------- Quality
test: test-backend test-mobile ## Run backend and mobile unit tests

test-backend:
	@cd backend && ./gradlew test

test-mobile:
	@cd mobile && ./gradlew test

test-mobile-fast: ## Shared + desktop JVM tests only (no Android/iOS test variants)
	@cd mobile && ./gradlew :composeApp:desktopTest

test-watch: ## Re-run shared JVM tests on every save
	@cd mobile && ./gradlew :composeApp:desktopTest --continuous

lint: lint-repo lint-backend lint-mobile ## Run every linter (repo, backend, mobile)

lint-repo: ## Lint workflows, scripts, spelling, Dockerfile, Swift, and architecture contracts
	@./scripts/lint-repo.sh

format: ## Auto-format Kotlin (ktlint) and Swift (SwiftLint)
	@cd backend && ./gradlew ktlintFormat --quiet
	@cd mobile && ./gradlew ktlintFormat --quiet
	@if command -v swiftlint >/dev/null; then cd mobile/iosApp && swiftlint --fix --quiet; \
		else echo 'swiftlint not installed (brew install swiftlint); Swift not formatted'; fi

lint-backend:
	@cd backend && ./gradlew detekt ktlintCheck

lint-mobile:
	@cd mobile && ./gradlew detekt ktlintCheck :androidApp:lintDebug :composeApp:lintDebug
	@./scripts/check-resources.sh

quick: ## Fast checks for modules changed on this branch (inner loop)
	@./scripts/quick-check.sh

quick-all: ## Fast checks for every module
	@./scripts/quick-check.sh --all

check-template: ## Fast initializer, generator, config-sync, resource, and architecture checks
	@./scripts/test-initializer.sh
	@./scripts/test-feature-generator.sh
	@./scripts/sync-app-config.sh --check
	@./scripts/check-resources.sh
	@./scripts/check-architecture.sh

check-backend: ## Backend CI gate: tests, detekt, ktlint, coverage (Kover)
	@cd backend && ./gradlew $(BACKEND_GATES)

check-mobile: ## Mobile CI gate: tests, detekt, ktlint, Android, desktop, iOS, migrations, coverage
	@cd mobile && ./gradlew $(MOBILE_GATES)

check: check-template check-backend check-mobile ## Run every CI gate locally
	@echo "All checks passed."

coverage: ## Kover HTML coverage reports and totals for backend and mobile
	@echo '==> backend'; cd backend && ./gradlew koverHtmlReport koverLog --quiet
	@echo '==> mobile (logic only; @Composable and the UI kit are covered by screenshots)'
	@cd mobile && ./gradlew :composeApp:koverHtmlReportUnitTests :composeApp:koverLogUnitTests --quiet
	@echo 'HTML: backend/build/reports/kover/html/index.html'
	@echo '      mobile/composeApp/build/reports/kover/htmlUnitTests/index.html'

# ---------------------------------------------------------------- Build
build: build-backend build-mobile ## Build backend and mobile

build-backend:
	@cd backend && ./gradlew build

build-mobile:
	@cd mobile && ./gradlew build

jar: ## Build the runnable backend fat jar
	@cd backend && ./gradlew buildFatJar
	@ls -lh backend/build/libs/*.jar 2>/dev/null || true

clean: ## Remove build outputs
	@cd backend && ./gradlew clean
	@cd mobile && ./gradlew clean

loc: ## Count Kotlin lines of code
	@printf 'Backend: '; find backend/src -name '*.kt' -print0 | xargs -0 cat | wc -l
	@printf 'Mobile:  '; find mobile -path '*/build' -prune -o -name '*.kt' -print0 | xargs -0 cat | wc -l

# ---------------------------------------------------------------- Versioning
version: ## Show the current version
	@./bump-version.sh

release-patch: ## Bump patch version (no commit or tag)
	@./bump-version.sh patch

release-minor: ## Bump minor version (no commit or tag)
	@./bump-version.sh minor

release-major: ## Bump major version (no commit or tag)
	@./bump-version.sh major

release-notes: ## Show release notes since the last release tag
	@./scripts/release-notes.sh

protect-main: ## Apply branch protection and auto-merge to main (requires gh)
	@./scripts/protect-main.sh

# Shortcuts
q: quick
t: test
l: lint
c: check
r: run
m: mobile
v: version
