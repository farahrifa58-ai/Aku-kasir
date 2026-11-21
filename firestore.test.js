const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_EMAIL = "farahrifa58@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
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

test("Verified admin (farahrifa58@gmail.com) has full CRUD & list access to activation_codes", async () => {
  const adminDb = testEnv
    .authenticatedContext("admin_uid", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const ref = adminDb.doc("activation_codes/AK-2026-0001");
  await assertSucceeds(
    ref.set({
      code: "AK-2026-0001",
      status: "available",
      createdAt: nowTs(),
      createdBy: ADMIN_EMAIL,
      notes: "Batch perdana",
    })
  );
  await assertSucceeds(ref.get());
  await assertSucceeds(adminDb.collection("activation_codes").get());
  await assertSucceeds(
    ref.update({
      notes: "Updated by admin",
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(ref.delete());
});

test("Verified admin (farahrifa58@gmail.com) has full CRUD & list access to users and subcollections", async () => {
  const adminDb = testEnv
    .authenticatedContext("admin_uid", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const userRef = adminDb.doc("users/merchant_001");
  await assertSucceeds(
    userRef.set({
      uid: "merchant_001",
      email: "merchant@example.com",
      businessName: "Toko Maju",
      role: "user",
      status: "active",
      isActive: true,
      createdAt: nowTs(),
    })
  );
  await assertSucceeds(userRef.get());
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    userRef.update({
      status: "suspended",
      notes: "Checked by admin",
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    adminDb.doc("users/merchant_001/settings/app").set({ storeName: "Toko Maju" })
  );
  await assertSucceeds(adminDb.doc("users/merchant_001/settings/app").get());
  await assertSucceeds(userRef.delete());
});

test("Email spoofing attack: unverified farahrifa58@gmail.com is rejected from admin operations on activation_codes and users", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc("users/merchant_002").set({
      uid: "merchant_002",
      email: "other@example.com",
      createdAt: nowTs(),
    });
  });

  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofedDb.doc("activation_codes/AK-SPOOF-001").set({
      status: "available",
      createdAt: nowTs(),
    })
  );
  await assertFails(spoofedDb.collection("activation_codes").get());
  await assertFails(spoofedDb.collection("users").get());
  await assertFails(spoofedDb.doc("users/merchant_002").get());
  await assertFails(spoofedDb.doc("users/merchant_002").delete());
});

test("Users collection enforces owner-only isolation and blocks privilege escalation / list scraping", async () => {
  const userADb = testEnv
    .authenticatedContext("user_a", {
      email: "usera@example.com",
      email_verified: true,
    })
    .firestore();

  const userBDb = testEnv
    .authenticatedContext("user_b", {
      email: "userb@example.com",
      email_verified: true,
    })
    .firestore();

  // User A can create and read their own profile
  await assertSucceeds(
    userADb.doc("users/user_a").set({
      uid: "user_a",
      email: "usera@example.com",
      businessName: "Warung A",
      createdAt: nowTs(),
    })
  );
  await assertSucceeds(userADb.doc("users/user_a").get());

  // User A can update their own allowed profile fields
  await assertSucceeds(
    userADb.doc("users/user_a").update({
      businessName: "Warung A Baru",
      updatedAt: nowTs(),
    })
  );

  // User A CANNOT self-assign admin role (privilege escalation)
  await assertFails(
    userADb.doc("users/user_a").update({
      role: "admin",
    })
  );

  // User A CANNOT inject ghost fields (shadow update)
  await assertFails(
    userADb.doc("users/user_a").update({
      isSuperAdmin: true,
    })
  );

  // User B CANNOT read, list, update, or delete User A's profile (PII isolation)
  await assertFails(userBDb.doc("users/user_a").get());
  await assertFails(userBDb.collection("users").get());
  await assertFails(
    userBDb.doc("users/user_a").update({
      businessName: "Hacked",
    })
  );
  await assertFails(userBDb.doc("users/user_a").delete());

  // Unauthenticated client CANNOT access users
  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(anonDb.doc("users/user_a").get());
  await assertFails(anonDb.collection("users").get());
});

test("Regular/unauthenticated device can check available code and activate once (available -> used)", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc("activation_codes/KASIR-PRO-001").set({
      code: "KASIR-PRO-001",
      status: "available",
      createdAt: nowTs(),
      createdBy: ADMIN_EMAIL,
      notes: "Pembeli reguler",
    });
  });

  const deviceDb = testEnv.unauthenticatedContext().firestore();
  const ref = deviceDb.doc("activation_codes/KASIR-PRO-001");

  // Device can check exact available code
  await assertSucceeds(ref.get());

  // Device can also check a non-existent valid code (returns exists=false cleanly)
  await assertSucceeds(deviceDb.doc("activation_codes/TIDAK-ADA-999").get());

  // Device activates once: available -> used (even when admin set extra metadata fields)
  await assertSucceeds(
    ref.update({
      status: "used",
      usedByBusinessName: "Warung Maju Jaya",
      registeredDeviceId: "DEV-ANDROID-123456",
      usedAt: nowTs(),
    })
  );

  // After activation, non-admin device CANNOT read used code (PII protection)
  await assertFails(ref.get());

  // Terminal state lock: second activation attempt MUST fail
  await assertFails(
    ref.update({
      status: "used",
      usedByBusinessName: "Toko Lain",
      registeredDeviceId: "DEV-OTHER-999",
      usedAt: nowTs(),
    })
  );

  // Admin can still read & manage the used code to audit merchant & device
  const adminDb = testEnv
    .authenticatedContext("admin_uid", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(adminDb.doc("activation_codes/KASIR-PRO-001").get());
  await assertSucceeds(
    adminDb.doc("activation_codes/KASIR-PRO-001").update({
      notes: "Terverifikasi oleh admin",
      updatedAt: nowTs(),
    })
  );
});

test("Security bounds: regular user cannot list, create, delete, inject ghost fields, or forge future timestamp on activation_codes", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc("activation_codes/KASIR-PRO-002").set({
      status: "available",
      createdAt: nowTs(),
    });
  });

  const regularUserDb = testEnv
    .authenticatedContext("regular_uid", {
      email: "biasa@example.com",
      email_verified: true,
    })
    .firestore();
  const ref = regularUserDb.doc("activation_codes/KASIR-PRO-002");

  // Regular user can check available code
  await assertSucceeds(ref.get());

  // Cannot list all codes (anti-scraping)
  await assertFails(regularUserDb.collection("activation_codes").get());

  // Cannot create or delete
  await assertFails(
    regularUserDb.doc("activation_codes/HACK-CODE-01").set({
      status: "available",
    })
  );
  await assertFails(ref.delete());

  // Shadow update (extra ghost field) rejected
  await assertFails(
    ref.update({
      status: "used",
      usedByBusinessName: "Toko Ghost",
      registeredDeviceId: "DEV-12345",
      usedAt: nowTs(),
      isAdmin: true,
    })
  );

  // Modifying admin-owned field like notes rejected
  await assertFails(
    ref.update({
      status: "used",
      usedByBusinessName: "Toko Ghost",
      registeredDeviceId: "DEV-12345",
      usedAt: nowTs(),
      notes: "Tampered",
    })
  );

  // Empty businessName rejected
  await assertFails(
    ref.update({
      status: "used",
      usedByBusinessName: "",
      registeredDeviceId: "DEV-12345",
      usedAt: nowTs(),
    })
  );

  // Future timestamp rejected
  await assertFails(
    ref.update({
      status: "used",
      usedByBusinessName: "Toko Future",
      registeredDeviceId: "DEV-12345",
      usedAt: new Date(Date.now() + 60000),
    })
  );

  // Keeping status as available rejected
  await assertFails(
    ref.update({
      status: "available",
      usedByBusinessName: "Toko Bypass",
      registeredDeviceId: "DEV-12345",
      usedAt: nowTs(),
    })
  );
});
