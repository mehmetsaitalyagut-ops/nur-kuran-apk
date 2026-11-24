const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated user cannot read or write any user collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("settings").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("history").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("daily_ibadat").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("favorites").doc("surah_1").set({
      userId: ALICE_UID,
      kind: "surah",
      surahNumber: 1,
      surahNameTr: "Fâtiha",
      surahNameAr: "الفاتحة",
      ayahNumber: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and partially update preferences, and cross-user access is denied", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const prefRef = alice.collection("users").doc(ALICE_UID).collection("settings").doc("preferences");
  await assertSucceeds(
    prefRef.set({
      userId: ALICE_UID,
      selectedReciterId: "ar.alafasy",
      darkTheme: true,
      dailyReadGoalAyahs: 20,
      dailyListenGoalMinutes: 15,
      lastSurahNumber: 1,
      lastSurahNameTr: "Fâtiha",
      lastAyahNumber: 1,
      lastReciterId: "ar.alafasy",
      lastPlaybackPositionMs: 0,
      completedJuzs: [1, 2],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    prefRef.update({
      selectedReciterId: "ar.abdurrahmaansudais",
      completedJuzs: [1, 2, 3],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    prefRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    prefRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read/write
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").update({
      darkTheme: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can manage favorites, history, and daily_ibadat; invalid payloads rejected", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Valid favorite ayah (with optional fields)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("favorites").doc("ayah_2_255").set({
      userId: ALICE_UID,
      kind: "ayah",
      surahNumber: 2,
      surahNameTr: "Bakara",
      surahNameAr: "البقرة",
      ayahNumber: 255,
      arabicText: "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
      turkishText: "Allah, kendisinden başka hiçbir ilah olmayandır.",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject @DocumentId "id" inside payload
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("favorites").doc("surah_36").set({
      id: "surah_36",
      userId: ALICE_UID,
      kind: "surah",
      surahNumber: 36,
      surahNameTr: "Yâsîn",
      surahNameAr: "يس",
      ayahNumber: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid listening history create + update
  const histRef = alice.collection("users").doc(ALICE_UID).collection("history").doc("surah_36");
  await assertSucceeds(
    histRef.set({
      userId: ALICE_UID,
      surahNumber: 36,
      surahNameTr: "Yâsîn",
      surahNameAr: "يس",
      reciterId: "ar.alafasy",
      reciterName: "Mishary Rashid Alafasy",
      ayahNumber: 12,
      listenCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    histRef.update({
      ayahNumber: 24,
      listenCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid daily_ibadat create + update
  const dailyRef = alice.collection("users").doc(ALICE_UID).collection("daily_ibadat").doc("2026-11-23");
  await assertSucceeds(
    dailyRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-23",
      ayahsRead: 10,
      listenedSeconds: 300,
      completedDailyGoal: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    dailyRef.update({
      ayahsRead: 25,
      listenedSeconds: 950,
      completedDailyGoal: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("daily_ibadat").doc("2026-11-24").set({
      userId: ALICE_UID,
      dateKey: "2026-11-24",
      ayahsRead: 5,
      listenedSeconds: 100,
      completedDailyGoal: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // List queries succeed on user subcollections
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("history").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("daily_ibadat").get());
});
