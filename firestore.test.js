const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_pos_123";
const BOB_UID = "bob_pos_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
}

function validSettings(uid) {
  return {
    userId: uid,
    storeName: "Toko Berkah",
    ownerName: "Rafline",
    phone: "08123456789",
    address: "Jl. Merdeka No. 1",
    photoData: "",
    darkTheme: false,
    language: "id",
    defaultTaxPercent: 11,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validProduct(uid) {
  return {
    userId: uid,
    name: "Beras Premium",
    sku: "BRS-001",
    category: "Sembako",
    unit: "Kg",
    costPrice: 12000,
    sellPrice: 15000,
    stock: 50,
    minStock: 5,
    description: "Beras pulen",
    photoData: "",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validTransaction(uid) {
  return {
    userId: uid,
    transactionNumber: "AK-20261113-0001",
    customerName: "Budi",
    items: [
      {
        productId: "prod_1",
        productName: "Beras Premium",
        unit: "Kg",
        unitPrice: 15000,
        costPrice: 12000,
        quantity: 0.5,
        subtotal: 7500,
      },
    ],
    subtotal: 7500,
    discountType: "NONE",
    discountValue: 0,
    discountAmount: 0,
    taxType: "NONE",
    taxPercent: 0,
    taxAmount: 0,
    totalAmount: 7500,
    paymentMethod: "CASH",
    amountPaid: 10000,
    changeAmount: 2500,
    status: "COMPLETED",
    statusReason: "",
    stockDeducted: true,
    stockRestored: false,
    transactionMillis: Date.now(),
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

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

test("Unauthenticated user: cannot read or write products or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/products/prod_1`).set(validProduct(ALICE_UID)));
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/transactions`).get());
});

test("Cross-user isolation: Bob cannot read or write Alice's store data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/products/prod_1`).set(validProduct(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/products/prod_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/products/prod_1`).update({ stock: 99, updatedAt: nowTs() }));
});

test("Owner CRUD: Alice can manage settings, categories, products, and transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/settings/app`).set(validSettings(ALICE_UID)));
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/categories/cat_1`).set({
      userId: ALICE_UID,
      name: "Sembako",
      isDefault: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/products/prod_1`).set(validProduct(ALICE_UID)));
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/products/prod_1`).update({
      stock: 49.5,
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/transactions/tx_1`).set(validTransaction(ALICE_UID)));
});

test("Terminal state lock: transaction can cancel once, cannot update again after CANCELLED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.doc(`users/${ALICE_UID}/transactions/tx_1`);
  await assertSucceeds(txRef.set(validTransaction(ALICE_UID)));

  // Cancel once
  await assertSucceeds(
    txRef.update({
      status: "CANCELLED",
      statusReason: "Salah input",
      stockRestored: true,
      updatedAt: nowTs(),
    })
  );

  // Second cancel/refund must fail (prevents double stock restoration)
  await assertFails(
    txRef.update({
      status: "REFUNDED",
      statusReason: "Double",
      stockRestored: true,
      updatedAt: nowTs(),
    })
  );
});

test("Shadow update & negative stock rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badProd = { ...validProduct(ALICE_UID), ghostField: true };
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/products/prod_bad`).set(badProd));

  const negStock = { ...validProduct(ALICE_UID), stock: -5 };
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/products/prod_neg`).set(negStock));
});

test("Expenses: owner CRUD works, cross-user & negative amount rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const validExpense = {
    userId: ALICE_UID,
    title: "Bayar Listrik Toko",
    category: "Listrik & Air",
    amount: 250000,
    notes: "Token PLN",
    expenseMillis: Date.now(),
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).set(validExpense));
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).update({
    amount: 275000,
    updatedAt: nowTs(),
  }));

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/expenses/exp_1`).get());
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/expenses/exp_neg`).set({ ...validExpense, amount: -100 }));
});

test("Monitor mode: authorized email can read Alice store directory & data read-only; unauthorized rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com" }).firestore();
  const ownerMonitorDb = testEnv.authenticatedContext(BOB_UID, { email: "Owner@Example.com" }).firestore();
  const strangerDb = testEnv.authenticatedContext("charlie_789", { email: "stranger@example.com" }).firestore();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/settings/app`).set({
      ...validSettings(ALICE_UID),
      ownerEmail: "alice@example.com",
      monitorEmails: ["owner@example.com"],
      showReceiptLogo: true,
      showReceiptCashierName: true,
      showReceiptStorePhone: true,
      showReceiptStoreAddress: true,
      showReceiptCustomerName: false,
      receiptFooterMessage: "Datang lagi ya!",
    })
  );
  await assertSucceeds(
    aliceDb.doc(`storeDirectory/${ALICE_UID}`).set({
      userId: ALICE_UID,
      ownerEmail: "alice@example.com",
      storeName: "Toko Berkah",
      monitorEmails: ["owner@example.com"],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/transactions/tx_1`).set(validTransaction(ALICE_UID)));

  // Authorized monitor (case-insensitive email claim) can query & read directory + transactions, cannot write
  await assertSucceeds(ownerMonitorDb.doc(`storeDirectory/${ALICE_UID}`).get());
  await assertSucceeds(
    ownerMonitorDb.collection("storeDirectory").where("monitorEmails", "array-contains", "owner@example.com").get()
  );
  await assertSucceeds(ownerMonitorDb.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(ownerMonitorDb.doc(`users/${ALICE_UID}/products/prod_x`).set(validProduct(ALICE_UID)));
  await assertFails(ownerMonitorDb.doc(`users/${ALICE_UID}/settings/app`).update({ storeName: "Hacked", updatedAt: nowTs() }));
  await assertFails(
    strangerDb.collection("storeDirectory").where("monitorEmails", "array-contains", "owner@example.com").get()
  );

  // Unauthorized stranger cannot read directory or transactions
  await assertFails(strangerDb.doc(`storeDirectory/${ALICE_UID}`).get());
  await assertFails(strangerDb.collection(`users/${ALICE_UID}/transactions`).get());
});

