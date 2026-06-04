/**
 * Duren Phase 0 Firestore Rules smoke test.
 *
 * Proves the rules engine loads and enforces the most basic invariant:
 * a user can write their own profile; another user cannot.
 *
 * Full coverage (every match block + edge cases) lands in Phase 1+.
 *
 * Run: from this directory, npm install, then with Firestore emulator running:
 *   firebase emulators:exec --only firestore "npm test"
 */
const fs = require('fs');
const path = require('path');
const assert = require('assert');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { setDoc, doc, serverTimestamp } = require('firebase/firestore');

let testEnv;

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

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

describe('Duren Phase 0 Firestore rules — profiles', () => {
  it('owner can create their own profile', async () => {
    const alice = testEnv.authenticatedContext('alice-uid').firestore();
    await assertSucceeds(
      setDoc(doc(alice, 'profiles/alice-uid'), {
        username: 'alice',
        displayName: 'Alice',
        email: 'alice@example.com',
        createdAt: serverTimestamp(),
        lastSeen: serverTimestamp(),
      })
    );
  });

  it('user cannot create a profile under another uid', async () => {
    const alice = testEnv.authenticatedContext('alice-uid').firestore();
    await assertFails(
      setDoc(doc(alice, 'profiles/bob-uid'), {
        username: 'alice',
        displayName: 'Alice',
        email: 'alice@example.com',
        createdAt: serverTimestamp(),
        lastSeen: serverTimestamp(),
      })
    );
  });

  it('unauthenticated user cannot create any profile', async () => {
    const anon = testEnv.unauthenticatedContext().firestore();
    await assertFails(
      setDoc(doc(anon, 'profiles/alice-uid'), {
        username: 'alice',
        displayName: 'Alice',
        email: 'alice@example.com',
        createdAt: serverTimestamp(),
        lastSeen: serverTimestamp(),
      })
    );
  });

  it('authed user can claim a free username sentinel doc', async () => {
    const alice = testEnv.authenticatedContext('alice-uid').firestore();
    await assertSucceeds(
      setDoc(doc(alice, 'usernames/alice'), { uid: 'alice-uid' })
    );
  });

  it('user cannot claim a username pointing at another uid', async () => {
    const alice = testEnv.authenticatedContext('alice-uid').firestore();
    await assertFails(
      setDoc(doc(alice, 'usernames/bob'), { uid: 'bob-uid' })
    );
  });
});
