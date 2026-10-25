# Reviewer's guide

A map of the challenge for reviewers: where each line of the brief is implemented and tested,
the order to read the code in, and the parts of the repository you can skip. The
[README](../README.md) has the full explanation; this page is for finding things fast.

## In five minutes

- **Watch it:** the [demo video](images/remit-demo.mp4) (82 s), or the
  [screenshots](images/remit-screens.png).
- **Run it:** three terminals, no Firebase account needed.

  ```bash
  make emulators            # Firebase Auth + Firestore emulators (UI on http://localhost:4000)
  make backend-emulated     # the API on http://localhost:8085 (Swagger UI at /swagger)
  make mobile               # build, install, and launch the Android debug app
  ```

- **Read it:** the twelve files under [Follow one payment](#follow-one-payment-through-the-code)
  carry the whole feature. Everything else supports them or comes from the template.

## The brief, line by line

"Met, differently" rows are deliberate; the README explains each one under
[Where this differs from the brief](../README.md#where-this-differs-from-the-brief).

| The brief asks for | Status | Implemented in | Proved by |
|---|---|---|---|
| Form: recipient email, numeric amount, currency dropdown (USD, EUR) | Met | `SendPaymentContent` and `CurrencyPicker` in [`SendPaymentScreen.kt`][send-screen] | [`SendPaymentViewModelTest`][send-vm-test], Appium |
| Validate in the shared KMP module (valid email, amount > 0, supported currency) | Met | [`PaymentValidator.kt`][validator] (shared), amounts parsed exactly by [`DecimalAmount`][decimal] into [`Money`][money] | [`PaymentValidatorTest`][validator-test], 65 cases in [`payment-validation-cases.json`][cases] that the app and the backend both run |
| Call the API, then save the transaction to Firestore | Met, differently: the server saves it | [`SendPaymentUseCase`][use-cases] → [`KtorPaymentsApi`][api] → [`paymentRoutes`][routes] → [`PaymentService`][service] → [`FirestorePaymentStore`][store] (one transaction) | Cucumber "A valid payment is processed and saved to Firestore", [`FirebaseEmulatorTest`][emulator-test] |
| Success and error messages | Met | Snackbars in `SendPaymentScreen`; wording in [`PaymentMessages.kt`][messages] | [`SendPaymentViewModelTest`][send-vm-test] |
| History: scrollable list with recipient, amount, currency, timestamp | Met | `PaymentHistoryContent` in [`PaymentHistoryScreen.kt`][history-screen] (a `LazyColumn` keyed by payment id) | [`PaymentHistoryViewModelTest`][history-vm-test], Appium |
| Real-time updates with a snapshot listener | Met | [`FirestorePaymentHistorySource`][history-source] (`callbackFlow`, listener removed in `awaitClose`), collected while visible by [`PaymentHistoryViewModel`][history-vm] | Cucumber "History updates in real time"; demo step 5 |
| `POST /payments` taking `{recipientEmail, amount, currency}`, returning the payment or an error | Met, differently: `POST /api/v1/payments`, auth and an `Idempotency-Key` required, amount read as an exact decimal | [`PaymentRoutes.kt`][routes], [`PaymentDto.kt`][dto], [OpenAPI spec][openapi] | [`PaymentRoutesTest`][routes-test], [`OpenApiContractTest`][contract-test] |
| Host the API locally, mocked, or on a free service | Met: Ktor, locally | [`backend/`](../backend) (`make backend-emulated`) | |
| `shared` module: models, API client, Firebase logic | Met | [`mobile/shared`](../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared) | |
| `androidApp` module: UI and Firebase setup | Met, differently: the Compose Multiplatform UI is in `composeApp`; `androidApp` holds the entry point and Firebase setup | [`mobile/composeApp`](../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit), [`mobile/androidApp`](../mobile/androidApp) | |
| expect/actual for the Firebase SDK | Met | `expect fun firebaseBackendModule` in [`FirebaseBackend.kt`][expect]; actuals for [Android][actual-android] (Firebase SDK), [iOS][actual-ios] and desktop ("not set up") | [`UnsupportedPlatformTest`][unsupported-test] |
| Ktor in the shared module | Met | [`KtorPaymentsApi`][api] | [`KtorPaymentsApiTest`][api-test] (status and error mapping), [`PaymentDtosTest`][dtos-test] (parsing) |
| Koin | Met | `paymentsModule()` in [`SharedModule.kt`][shared-module], app bindings in [`PaymentsModule.kt`][app-module] | [`PaymentsModuleTest`][module-test] |
| Jetpack Compose UI | Met | Compose Multiplatform, Material 3, light and dark | |
| BDD: 2-3 Cucumber scenarios | Met: 13 scenarios | [`send_payment.feature`][feature-send], [`transaction_history.feature`][feature-history]; steps in [`PaymentSteps.kt`][steps] | `make test-bdd` |
| Unit tests: validation and API response parsing in `shared` | Met | [`mobile/shared/src/commonTest`](../mobile/shared/src/commonTest/kotlin/com/asterixorobelix/remit/shared) | `make test-mobile` |
| Appium: send a payment and see it in history | Met | `sentPaymentAppearsInTransactionHistory` in [`SendPaymentUiTest.kt`][appium] | `make test-appium` |
| JMeter: 5 concurrent users on `/payments`, response times | Met | [`perf/payments.jmx`](../perf/payments.jmx) (create and replay samplers, latency assertions) | `make test-perf`; results in the README |
| README: setup, architecture, running the app and tests | Met | [README](../README.md) | |
| Demo script or video | Met | [Demo script](DEMO.md), [video](images/remit-demo.mp4) | |
| KMP architecture and cross-platform potential | Met | README, [Kotlin Multiplatform and its cross-platform potential](../README.md#kotlin-multiplatform-and-its-cross-platform-potential) | |

## Follow one payment through the code

Read these in order; each step calls the next.

1. [`SendPaymentScreen.kt`][send-screen]: the form. `SendPaymentContent` is stateless (state in,
   events out) and has previews; `SendPaymentScreen` wires it to the ViewModel and shows snackbars.
2. [`SendPaymentViewModel.kt`][send-vm]: keeps what the user typed in `SavedStateHandle` and calls
   the use case from `submit()`. No business logic.
3. [`PaymentUseCases.kt`][use-cases] (shared): `SendPaymentUseCase` validates, then sends with one
   idempotency key per payment ([`IdempotencyKeys.kt`][keys]), saved to SQLDelight before the
   request so a crash cannot pay twice.
4. [`PaymentValidator.kt`][validator] (shared): every rule, one error per field.
5. [`PaymentsApi.kt`][api] (shared): `KtorPaymentsApi` posts to `/api/v1/payments` and maps each
   status to a result. [`PaymentRepositoryImpl.kt`][repository] adds the ID token and refreshes
   it once on a 401.
6. [`PaymentRoutes.kt`][routes] (backend): the order of checks (configured, signed in, key present,
   body valid), then the service.
7. [`PaymentService.kt`][service] and [`FirestorePaymentStore.kt`][store]: validate again, then write
   the payment and its idempotency record in one Firestore transaction.
8. [`firestore.rules`][rules]: users read only their own payments; no client writes.
9. [`FirestorePaymentHistorySource.kt`][history-source] (shared, Android): the snapshot listener.
10. [`PaymentHistoryViewModel.kt`][history-vm]: `stateIn(WhileSubscribed(5 s))`, so the listener
    stops when History is not on screen.
11. [`PaymentHistoryScreen.kt`][history-screen]: the list.
12. [`FirebaseBackend.kt`][expect] and its [Android actual][actual-android]: the expect/actual seam
    that keeps the Firebase SDK out of common code.

## What you can skip

Remit is an instance of my app template ([docs/TEMPLATE.md](TEMPLATE.md)). The template parts
work and are tested, but none of them is needed to judge the brief.

| Path | What it is |
|---|---|
| `mobile/fastlane`, `mobile/iosApp`, `mobile/store_assets`, `.github/workflows` | Store delivery, the iOS shell, and CI from the template |
| `setup.sh`, `bump-version.sh`, `app.properties`, `APP_CHECKLIST.md`, `RELEASE_CHECKLIST.md` | Creating and releasing an app from the template |
| `presentation/feedback`, `presentation/developer`, `presentation/webview`, `presentation/forceupdate` | Template screens reached from Settings (feedback, developer options, legal pages) or remote config |
| `ui/components`, `gallery/` | The template's component kit and its desktop gallery |
| `AGENTS.md`, `CLAUDE.md`, `.claude/`, `.codex/`, `scripts/agent-hook.sh` | Guardrails for AI coding agents |
| `scripts/test-initializer.sh`, `scripts/create-feature.sh`, `scripts/test-feature-generator.sh` | Template tooling and its tests |

To see only the challenge work, start after the commits that set up the template:

```bash
git log --oneline 53b7cfa..main                 # every challenge commit
git diff --stat 53b7cfa..main -- mobile/shared backend/src firebase e2e perf testdata
```

## Checking a claim

| Claim | Fastest check |
|---|---|
| A retried payment is stored once | Demo step 9 (curl twice with one key), or `make test-backend-emulated` (ten concurrent requests, one payment) |
| The app and the server validate alike | `make test-mobile test-backend`: both run `testdata/payment-validation-cases.json` |
| Users cannot read others' payments or write any | `make test-rules` |
| History is live | Demo step 5: add a document in the Emulator Suite UI with History open |
| A crash mid-send does not pay twice | Demo step 8, or the Cucumber scenario "A payment interrupted by the app closing" |
| Coverage floors | `make coverage` (shared 90%, app 80%, backend 80%) |
| Every CI gate | `make check` |

[send-screen]: ../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit/presentation/payments/send/SendPaymentScreen.kt
[send-vm]: ../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit/presentation/payments/send/SendPaymentViewModel.kt
[send-vm-test]: ../mobile/composeApp/src/commonTest/kotlin/com/asterixorobelix/remit/presentation/payments/SendPaymentViewModelTest.kt
[messages]: ../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit/presentation/payments/PaymentMessages.kt
[history-screen]: ../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit/presentation/payments/history/PaymentHistoryScreen.kt
[history-vm]: ../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit/presentation/payments/history/PaymentHistoryViewModel.kt
[history-vm-test]: ../mobile/composeApp/src/commonTest/kotlin/com/asterixorobelix/remit/presentation/payments/PaymentHistoryViewModelTest.kt
[app-module]: ../mobile/composeApp/src/commonMain/kotlin/com/asterixorobelix/remit/di/PaymentsModule.kt
[module-test]: ../mobile/composeApp/src/commonTest/kotlin/com/asterixorobelix/remit/di/PaymentsModuleTest.kt
[validator]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/domain/validation/PaymentValidator.kt
[validator-test]: ../mobile/shared/src/commonTest/kotlin/com/asterixorobelix/remit/shared/domain/validation/PaymentValidatorTest.kt
[decimal]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/domain/model/DecimalAmount.kt
[money]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/domain/model/Money.kt
[use-cases]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/domain/usecase/PaymentUseCases.kt
[keys]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/domain/usecase/IdempotencyKeys.kt
[api]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/data/remote/PaymentsApi.kt
[api-test]: ../mobile/shared/src/commonTest/kotlin/com/asterixorobelix/remit/shared/data/remote/KtorPaymentsApiTest.kt
[dtos-test]: ../mobile/shared/src/commonTest/kotlin/com/asterixorobelix/remit/shared/data/remote/PaymentDtosTest.kt
[repository]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/data/PaymentRepositoryImpl.kt
[shared-module]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/di/SharedModule.kt
[expect]: ../mobile/shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared/data/FirebaseBackend.kt
[actual-android]: ../mobile/shared/src/androidMain/kotlin/com/asterixorobelix/remit/shared/data/FirebaseBackend.android.kt
[actual-ios]: ../mobile/shared/src/iosMain/kotlin/com/asterixorobelix/remit/shared/data/FirebaseBackend.ios.kt
[unsupported-test]: ../mobile/shared/src/commonTest/kotlin/com/asterixorobelix/remit/shared/data/UnsupportedPlatformTest.kt
[history-source]: ../mobile/shared/src/androidMain/kotlin/com/asterixorobelix/remit/shared/data/history/FirestorePaymentHistorySource.kt
[feature-send]: ../mobile/shared/src/desktopTest/resources/features/send_payment.feature
[feature-history]: ../mobile/shared/src/desktopTest/resources/features/transaction_history.feature
[steps]: ../mobile/shared/src/desktopTest/kotlin/com/asterixorobelix/remit/shared/bdd/PaymentSteps.kt
[cases]: ../testdata/payment-validation-cases.json
[routes]: ../backend/src/main/kotlin/com/asterixorobelix/remit/routes/PaymentRoutes.kt
[routes-test]: ../backend/src/test/kotlin/com/asterixorobelix/remit/routes/PaymentRoutesTest.kt
[contract-test]: ../backend/src/test/kotlin/com/asterixorobelix/remit/OpenApiContractTest.kt
[dto]: ../backend/src/main/kotlin/com/asterixorobelix/remit/dto/PaymentDto.kt
[openapi]: ../backend/src/main/resources/openapi/documentation.yaml
[service]: ../backend/src/main/kotlin/com/asterixorobelix/remit/service/PaymentService.kt
[store]: ../backend/src/main/kotlin/com/asterixorobelix/remit/service/FirestorePaymentStore.kt
[emulator-test]: ../backend/src/test/kotlin/com/asterixorobelix/remit/service/FirebaseEmulatorTest.kt
[rules]: ../firebase/firestore.rules
[appium]: ../e2e/src/test/kotlin/com/asterixorobelix/remit/e2e/SendPaymentUiTest.kt
