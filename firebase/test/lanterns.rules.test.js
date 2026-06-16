/**
 * Duren Firestore Rules — lanterns.
 *
 * A lantern is a drift message any wanderer can "find". The only thing finding it may
 * do is bump foundCount by one — never edit its text, move its expiry, or change who
 * lit it. These tests hold that line.
 */
const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { setDoc, updateDoc, doc, Timestamp } = require('firebase/firestore');

let testEnv;
const now = Timestamp.now();
const expiry = Timestamp.fromMillis(now.toMillis() + 48 * 3600 * 1000);

const OWNER = 'owner-uid';
const WANDERER = 'wanderer-uid';

const baseLantern = (extra = {}) => ({
  authorId: OWNER,
  text: 'a light left in the dark',
  createdAt: now,
  expiresAt: expiry,
  foundCount: 0,
  ...extra,
});

async function seedLantern(extra = {}) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'lanterns/l1'), baseLantern(extra));
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

describe('lanterns', () => {
  it('you can light your own lantern (foundCount starts at zero)', async () => {
    const owner = testEnv.authenticatedContext(OWNER).firestore();
    await assertSucceeds(setDoc(doc(owner, 'lanterns/new1'), baseLantern()));
  });

  it('you cannot light a lantern under someone else\'s name', async () => {
    const wanderer = testEnv.authenticatedContext(WANDERER).firestore();
    await assertFails(setDoc(doc(wanderer, 'lanterns/new2'), baseLantern()));
  });

  it('a wanderer may mark it found (foundCount +1)', async () => {
    await seedLantern();
    const wanderer = testEnv.authenticatedContext(WANDERER).firestore();
    await assertSucceeds(updateDoc(doc(wanderer, 'lanterns/l1'), { foundCount: 1 }));
  });

  it('a wanderer cannot inflate foundCount', async () => {
    await seedLantern();
    const wanderer = testEnv.authenticatedContext(WANDERER).firestore();
    await assertFails(updateDoc(doc(wanderer, 'lanterns/l1'), { foundCount: 99 }));
  });

  it('a wanderer cannot rewrite the lantern\'s words', async () => {
    await seedLantern();
    const wanderer = testEnv.authenticatedContext(WANDERER).firestore();
    await assertFails(updateDoc(doc(wanderer, 'lanterns/l1'), { foundCount: 1, text: 'changed' }));
  });
});
