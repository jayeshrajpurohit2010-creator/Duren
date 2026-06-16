/**
 * Duren Firestore Rules — the signup batch.
 *
 * Reproduces exactly what AuthRepository.signUp writes (profile doc + username
 * sentinel, in one atomic batch) to prove the rules permit a real first signup —
 * the most important write in the app, since a denied profile create rolls the
 * whole account back and signs the new user out.
 */
const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { writeBatch, setDoc, doc, serverTimestamp } = require('firebase/firestore');

let testEnv;
const UID = 'new-soul-uid';

const profileData = () => ({
  username: 'embertester',
  displayName: 'EmberTester',
  email: 'embertester@test.com',
  bio: '',
  pronouns: '',
  avatarUrl: 'https://api.dicebear.com/x.svg',
  createdAt: serverTimestamp(),
  lastSeen: serverTimestamp(),
  accentColor: '#2dd4bf',
  lightModeEnabled: false,
  avatarColor: '#FF6B35',
  showLantern: true,
  showMoodCanvas: false,
  allowAnonBox: true,
  showTestimonials: false,
  hasOnboarded: false,
});

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'duren-rules-test',
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '..', '..', 'firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});
after(async () => { if (testEnv) await testEnv.cleanup(); });
beforeEach(async () => { await testEnv.clearFirestore(); });

describe('signup', () => {
  it('a new soul can write their profile + username sentinel in one batch', async () => {
    const db = testEnv.authenticatedContext(UID).firestore();
    const batch = writeBatch(db);
    batch.set(doc(db, `profiles/${UID}`), profileData());
    batch.set(doc(db, 'usernames/embertester'), { uid: UID });
    await assertSucceeds(batch.commit());
  });

  it('a taken username sentinel blocks the batch (update is denied)', async () => {
    // Pre-seed the sentinel as if someone already holds the name.
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'usernames/embertester'), { uid: 'someone-else' });
    });
    const db = testEnv.authenticatedContext(UID).firestore();
    const batch = writeBatch(db);
    batch.set(doc(db, `profiles/${UID}`), profileData());
    batch.set(doc(db, 'usernames/embertester'), { uid: UID });
    await assertFails(batch.commit());
  });
});
