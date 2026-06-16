/**
 * Duren Firestore Rules — embers.
 *
 * The ember is the heart of the network and the most-attacked surface: any authed
 * user can echo, whisper, cold-mark, or vote on anyone's ember, so the update rule
 * has to permit those *bounded* bumps while making it impossible to deface the post,
 * hijack its author, inflate a counter, or immortalise it (defeating ephemerality).
 *
 * Run from this directory with the Firestore emulator up:
 *   firebase emulators:exec --only firestore "npm test"
 */
const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { setDoc, updateDoc, deleteDoc, getDoc, doc, Timestamp } = require('firebase/firestore');

let testEnv;
const now = Timestamp.now();
const expiry = Timestamp.fromMillis(now.toMillis() + 48 * 3600 * 1000);

const AUTHOR = 'author-uid';
const STRANGER = 'stranger-uid';

const baseEmber = (extra = {}) => ({
  authorId: AUTHOR,
  authorName: 'Author',
  text: 'a quiet ember',
  mode: 'named',
  mediaUrls: [],
  createdAt: now,
  expiresAt: expiry,
  echoCount: 0,
  coldMarkCount: 0,
  whisperCount: 0,
  pollYes: 0,
  pollNo: 0,
  ...extra,
});

async function seedEmber(extra = {}) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'embers/e1'), baseEmber(extra));
  });
}

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

describe('embers — create', () => {
  it('an author can post their own ember (counters start cold)', async () => {
    const author = testEnv.authenticatedContext(AUTHOR).firestore();
    await assertSucceeds(setDoc(doc(author, 'embers/new1'), baseEmber()));
  });

  it('you cannot post as someone else', async () => {
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(setDoc(doc(stranger, 'embers/new2'), baseEmber()));
  });

  it('you cannot post an ember that arrives pre-warmed', async () => {
    const author = testEnv.authenticatedContext(AUTHOR).firestore();
    await assertFails(setDoc(doc(author, 'embers/new3'), baseEmber({ echoCount: 50 })));
    await assertFails(setDoc(doc(author, 'embers/new4'), baseEmber({ whisperCount: 9 })));
  });
});

describe('embers — engagement is permitted but bounded', () => {
  it('a stranger may echo (echoCount +1)', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertSucceeds(updateDoc(doc(stranger, 'embers/e1'), { echoCount: 1 }));
  });

  it('a stranger may add a whisper bump (whisperCount +1)', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertSucceeds(updateDoc(doc(stranger, 'embers/e1'), { whisperCount: 1 }));
  });

  it('an echo-extension to within the 32-day ceiling is allowed', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    const extended = Timestamp.fromMillis(now.toMillis() + 72 * 3600 * 1000);
    await assertSucceeds(updateDoc(doc(stranger, 'embers/e1'), { expiresAt: extended, extended: true }));
  });
});

describe('embers — abuse is blocked', () => {
  it('a stranger cannot inflate echoCount past a single step', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(updateDoc(doc(stranger, 'embers/e1'), { echoCount: 9999 }));
  });

  it('a stranger cannot deface the text', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(updateDoc(doc(stranger, 'embers/e1'), { text: 'hijacked' }));
  });

  it('a stranger cannot reassign the author', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(updateDoc(doc(stranger, 'embers/e1'), { authorId: STRANGER }));
  });

  it('a stranger cannot swap the media onto someone else\'s post', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(updateDoc(doc(stranger, 'embers/e1'), { mediaUrls: ['data:evil'] }));
  });

  it('a stranger cannot immortalise an ember (defeating ephemerality)', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    const farFuture = Timestamp.fromMillis(now.toMillis() + 365 * 24 * 3600 * 1000);
    await assertFails(updateDoc(doc(stranger, 'embers/e1'), { expiresAt: farFuture }));
  });
});

describe('embers — kindling (anonymous react)', () => {
  it('a stranger may kindle (kindlingCount +1)', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertSucceeds(updateDoc(doc(stranger, 'embers/e1'), { kindlingCount: 1 }));
  });

  it('a stranger cannot inflate kindlingCount', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(updateDoc(doc(stranger, 'embers/e1'), { kindlingCount: 500 }));
  });

  it('a kindler records only their own write-once mark', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertSucceeds(setDoc(doc(stranger, `embers/e1/kindling/${STRANGER}`), { createdAt: now }));
    await assertFails(setDoc(doc(stranger, `embers/e1/kindling/${AUTHOR}`), { createdAt: now }));
  });

  it('the author cannot peek at who kindled their ember', async () => {
    await seedEmber();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), `embers/e1/kindling/${STRANGER}`), { createdAt: now });
    });
    const author = testEnv.authenticatedContext(AUTHOR).firestore();
    await assertFails(getDoc(doc(author, `embers/e1/kindling/${STRANGER}`)));
  });
});

describe('embers — delete', () => {
  it('the author can delete their own ember', async () => {
    await seedEmber();
    const author = testEnv.authenticatedContext(AUTHOR).firestore();
    await assertSucceeds(deleteDoc(doc(author, 'embers/e1')));
  });

  it('a stranger cannot delete the author\'s ember', async () => {
    await seedEmber();
    const stranger = testEnv.authenticatedContext(STRANGER).firestore();
    await assertFails(deleteDoc(doc(stranger, 'embers/e1')));
  });
});
