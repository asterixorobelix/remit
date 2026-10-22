# Demo script

A five-minute walkthrough of Remit on an Android emulator, against the local Firebase emulators.
The recording of this script is [images/remit-demo.mp4](images/remit-demo.mp4).

## Setup (once)

```bash
make emulators                                                     # terminal 1
make backend-emulated                                              # terminal 2
echo 'FIREBASE_EMULATOR_HOST=10.0.2.2' >> mobile/local.properties
make mobile                                                        # installs and launches the app
```

Keep the Emulator Suite UI open at <http://localhost:4000/firestore> to watch documents appear.

## Walkthrough

1. **First launch.** Onboarding appears; tap **Skip**. Behind the scenes the app signs in
   anonymously (Auth emulator: one new user).
2. **Validation on the device.** Tap **Send payment** with the form empty: each field shows its
   own error. Enter `bob@example.com`, then type `1,000` as the amount: "Use at most two decimal
   places". The app never guesses that this meant one thousand, or one dollar.
3. **Send.** Change the amount to `10.50` and press the keyboard's Done key or **Send payment**.
   The button shows progress, then "Sent $10.50 to bob@example.com". In the Emulator Suite UI, a
   `payments` document (1050 cents, your uid) and an `idempotencyKeys` document appear.
4. **History.** Tap **View** or the **History** tab: the payment is listed with recipient,
   amount, currency, local time, and status.
5. **Live updates.** With History open, add a document to `payments` in the Emulator Suite UI
   with your `senderUid` (copy it from the existing document), or send from another device
   signed in as the same user. It appears at the top within a second, with no refresh.
6. **EUR and a comma decimal.** Back on **Send**, pick **EUR** and send `7,5` to
   `dave@example.com`: "Sent €7.50 to dave@example.com".
7. **Failure and a safe retry.** Stop the backend (Ctrl+C in terminal 2) and send another
   payment: "Couldn't reach the server, so the payment wasn't sent", with **Retry**. Start the
   backend again and tap **Send payment**: it goes through, and Firestore holds exactly one
   document for it, because the retry reused the idempotency key.
8. **An interrupted payment.** Stop the backend, send a payment so it fails, then swipe the app
   away (or `adb shell am force-stop com.asterixorobelix.remit.debug`). Start the backend and
   reopen the app: "Unfinished payment" shows it with the form filled in. Send it again: one
   payment, because the key was saved before the first attempt went out.
9. **The API directly.** Show idempotency with curl (an ID token from the Auth emulator):

   ```bash
   TOKEN=$(curl -s -X POST 'http://localhost:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key' \
     -H 'Content-Type: application/json' -d '{"returnSecureToken":true}' | jq -r .idToken)
   KEY=$(uuidgen)
   for i in 1 2; do
     curl -s -i -X POST localhost:8085/api/v1/payments -H "Authorization: Bearer $TOKEN" \
       -H "Idempotency-Key: $KEY" -H 'Content-Type: application/json' \
       -d '{"recipientEmail":"bob@example.com","amount":10.50,"currency":"USD"}' | grep -iE '^HTTP|replayed|"id"'
   done
   ```

   Both answers carry the same payment id; the second has `Idempotent-Replayed: true`. Changing
   the amount with the same key returns 409 `IDEMPOTENCY_KEY_REUSED`.

## Tests to show

```bash
make test-bdd        # Cucumber scenarios; report in mobile/shared/build/reports/cucumber/index.html
make test-rules      # Firestore security rules
make test-appium     # UI tests on the running emulator
make test-perf       # JMeter, 5 concurrent users; dashboard in build/perf/report/index.html
```
