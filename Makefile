# Common commands. Run `make` (or `make help`) to list them.

PACKAGE := $(shell awk -F= '$$1=="PACKAGE_NAME" { print $$2 }' app.properties)
MOBILE_GATES := test detekt ktlintCheck :androidApp:lintDebug :composeApp:lintAndroidMain :shared:lintAndroidMain \
	:androidApp:assembleDebug :composeApp:compileKotlinDesktop \
	:composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:verifySqlDelightMigration \
	:composeApp:koverVerifyUnitTests :shared:koverVerifyUnitTests
BACKEND_GATES := test detekt ktlintCheck koverVerify

.DEFAULT_GOAL := help
.PHONY: help setup bootstrap doctor app-checklist setup-hooks ui ui-hot backend backend-emulated emulators \
	mobile apk apk-install apk-release baseline-profile ios ios-app ios-run feature \
	test test-backend test-backend-emulated test-rules test-perf test-bdd test-appium test-mobile test-mobile-fast test-watch lint lint-repo lint-backend lint-mobile format \
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

backend: ## Start the backend API on http://localhost:8085 in development mode
	@cd backend && APP_ENVIRONMENT=development ./gradlew run

backend-emulated: ## Start the backend against the Firebase emulators (make emulators first)
	@cd backend && APP_ENVIRONMENT=development FIREBASE_PROJECT_ID=$(FIREBASE_PROJECT) \
		FIRESTORE_EMULATOR_HOST=localhost:8080 FIREBASE_AUTH_EMULATOR_HOST=localhost:9099 ./gradlew run

# ---------------------------------------------------------------- Firebase
# The emulators use the real project id so the debug app's google-services.json matches; they
# never touch the real project's data. Without google-services.json the debug app uses
# FirebaseSettings.EMULATOR_PROJECT_ID, which must equal this default. Tests use an emulator-only
# demo- project instead.
FIREBASE_PROJECT ?= remit-payments

emulators: ## Start the Firebase Auth and Firestore emulators (UI on http://localhost:4000)
	@firebase emulators:start --only auth,firestore --project $(FIREBASE_PROJECT)

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

baseline-profile: ## Regenerate the release Baseline Profile on the connected emulator (API 33+)
	@cd mobile && ./gradlew :androidApp:generateReleaseBaselineProfile

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

test-backend-emulated: ## Backend tests plus the Firebase integration tests (starts the emulators)
	@firebase emulators:exec --only auth,firestore --project demo-remit-test "cd backend && ./gradlew test --rerun"

test-bdd: ## Cucumber scenarios for sending payments and history (shared module, JVM)
	@cd mobile && ./gradlew :shared:desktopTest --tests '*CucumberTest*'
	@echo "Report: mobile/shared/build/reports/cucumber/index.html"

test-appium: ## Appium UI tests on the connected Android device (emulators, backend, and app running)
	@./scripts/test-appium.sh

test-rules: ## Firestore security rules tests on the emulator
	@./scripts/test-rules.sh

test-perf: ## JMeter load test of POST /api/v1/payments (make emulators and make backend-emulated first)
	@./scripts/perf.sh

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
	@cd mobile && ./gradlew detekt ktlintCheck :androidApp:lintDebug :composeApp:lintAndroidMain :shared:lintAndroidMain
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
r: backend
m: mobile
v: version
