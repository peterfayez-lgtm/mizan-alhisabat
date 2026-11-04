const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read transactions, budgets, or templates", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("templates").get());
});

test("Authenticated user: can create and read own valid transaction, budget, and template", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "سوبر ماركت",
      amount: 350.5,
      type: "expense",
      category: "طعام ومشتريات",
      priority: "essential",
      recurrence: "variable",
      wallet: "bank",
      txDateEpochMs: 1730000000000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b_1").set({
      userId: ALICE_UID,
      category: "طعام ومشتريات",
      monthlyLimit: 4000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("templates").doc("tpl_1").set({
      userId: ALICE_UID,
      title: "قهوة مختصة",
      amount: 65,
      category: "مطاعم وكافيهات",
      priority: "luxury",
      recurrence: "variable",
      wallet: "cash",
      iconKey: "coffee",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("templates").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's transactions", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "سوبر ماركت",
      amount: 350,
      type: "expense",
      category: "طعام ومشتريات",
      priority: "essential",
      recurrence: "variable",
      wallet: "bank",
      txDateEpochMs: 1730000000000,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_2").set({
      userId: BOB_UID,
      title: "اختراق",
      amount: 100,
      type: "expense",
      category: "طعام ومشتريات",
      priority: "essential",
      recurrence: "variable",
      wallet: "cash",
      txDateEpochMs: 1730000000000,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Validation rejects shadow fields, negative amounts, and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_shadow").set({
      userId: ALICE_UID,
      title: "قهوة",
      amount: 50,
      type: "expense",
      category: "مطاعم وكافيهات",
      priority: "luxury",
      recurrence: "variable",
      wallet: "cash",
      txDateEpochMs: 1730000000000,
      createdAt: now,
      updatedAt: now,
      isVerified: true,
    })
  );

  // Negative amount
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_neg").set({
      userId: ALICE_UID,
      title: "قهوة",
      amount: -50,
      type: "expense",
      category: "مطاعم وكافيهات",
      priority: "luxury",
      recurrence: "variable",
      wallet: "cash",
      txDateEpochMs: 1730000000000,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid priority enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_enum").set({
      userId: ALICE_UID,
      title: "قهوة",
      amount: 50,
      type: "expense",
      category: "مطاعم وكافيهات",
      priority: "invalid_priority",
      recurrence: "variable",
      wallet: "cash",
      txDateEpochMs: 1730000000000,
      createdAt: now,
      updatedAt: now,
    })
  );
});
