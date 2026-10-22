// Firestore security rules: users read only their own payments; clients never write.
// Runs against the Firestore emulator (make test-rules).
import { after, before, beforeEach, describe, it } from "node:test";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  Timestamp,
  updateDoc,
  where,
} from "firebase/firestore";

let env;

const payment = (senderUid) => ({
  senderUid,
  recipientEmail: "bob@example.com",
  amountMinor: 1050,
  currency: "USD",
  status: "COMPLETED",
  createdAt: Timestamp.now(),
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-remit-rules",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

beforeEach(async () => {
  await env.clearFirestore();
  // Seed as the backend would: the Admin SDK bypasses rules.
  await env.withSecurityRulesDisabled(async (admin) => {
    const db = admin.firestore();
    await setDoc(doc(db, "payments/alice-1"), payment("alice"));
    await setDoc(doc(db, "payments/bob-1"), payment("bob"));
    await setDoc(doc(db, "idempotencyKeys/alice_key-1"), { paymentId: "alice-1" });
  });
});

after(async () => {
  await env.cleanup();
});

describe("payments", () => {
  it("lets a user read their own payment", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertSucceeds(getDoc(doc(db, "payments/alice-1")));
  });

  it("hides other users' payments", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(getDoc(doc(db, "payments/bob-1")));
  });

  it("allows the history query filtered to the caller", async () => {
    const db = env.authenticatedContext("alice").firestore();
    const own = query(collection(db, "payments"), where("senderUid", "==", "alice"));
    await assertSucceeds(getDocs(own));
  });

  it("rejects an unfiltered or someone else's history query", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(getDocs(collection(db, "payments")));
    await assertFails(getDocs(query(collection(db, "payments"), where("senderUid", "==", "bob"))));
  });

  it("rejects signed-out readers", async () => {
    const db = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "payments/alice-1")));
  });

  it("never lets clients create, change, or delete payments", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(db, "payments/forged"), payment("alice")));
    await assertFails(updateDoc(doc(db, "payments/alice-1"), { amountMinor: 1 }));
    await assertFails(deleteDoc(doc(db, "payments/alice-1")));
  });
});

describe("server-only data", () => {
  it("keeps idempotency records away from clients", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(getDoc(doc(db, "idempotencyKeys/alice_key-1")));
    await assertFails(setDoc(doc(db, "idempotencyKeys/alice_key-2"), { paymentId: "x" }));
  });
});
