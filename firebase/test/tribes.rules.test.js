/**
 * Duren Firestore Rules — tribes.
 *
 * A tribe's headcount ("14 souls around the fire") is client-maintained, so the rule
 * lets any member nudge memberCount by one (join/leave) while freezing the tribe's
 * identity — its name and creator can never change, and the counter can't be inflated.
 */
const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { setDoc, updateDoc, deleteDoc, doc, Timestamp } = require('firebase/firestore');

let testEnv;
const now = Timestamp.now();

const KEEPER = 'keeper-uid';
const MEMBER = 'member-uid';

const baseTribe = (extra = {}) => ({
  name: 'Night Owls',
  description: 'the world is asleep',
  genre: 'life',
  createdBy: KEEPER,
  createdAt: now,
  memberCount: 1,
  ...extra,
});

async function seedTribe(extra = {}) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'tribes/t1'), baseTribe(extra));
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

describe('tribes', () => {
  it('a keeper can found a tribe starting at one soul', async () => {
    const keeper = testEnv.authenticatedContext(KEEPER).firestore();
    await assertSucceeds(setDoc(doc(keeper, 'tribes/new1'), baseTribe()));
  });

  it('a tribe cannot be founded pre-populated', async () => {
    const keeper = testEnv.authenticatedContext(KEEPER).firestore();
    await assertFails(setDoc(doc(keeper, 'tribes/new2'), baseTribe({ memberCount: 500 })));
  });

  it('a member may join (memberCount +1)', async () => {
    await seedTribe();
    const member = testEnv.authenticatedContext(MEMBER).firestore();
    await assertSucceeds(updateDoc(doc(member, 'tribes/t1'), { memberCount: 2 }));
  });

  it('a member may leave (memberCount -1)', async () => {
    await seedTribe({ memberCount: 5 });
    const member = testEnv.authenticatedContext(MEMBER).firestore();
    await assertSucceeds(updateDoc(doc(member, 'tribes/t1'), { memberCount: 4 }));
  });

  it('nobody can inflate the headcount', async () => {
    await seedTribe();
    const member = testEnv.authenticatedContext(MEMBER).firestore();
    await assertFails(updateDoc(doc(member, 'tribes/t1'), { memberCount: 9999 }));
  });

  it('nobody can rename a tribe out from under its keeper', async () => {
    await seedTribe();
    const member = testEnv.authenticatedContext(MEMBER).firestore();
    await assertFails(updateDoc(doc(member, 'tribes/t1'), { name: 'Hijacked' }));
  });

  it('only the keeper can delete the tribe', async () => {
    await seedTribe();
    const member = testEnv.authenticatedContext(MEMBER).firestore();
    await assertFails(deleteDoc(doc(member, 'tribes/t1')));
    const keeper = testEnv.authenticatedContext(KEEPER).firestore();
    await assertSucceeds(deleteDoc(doc(keeper, 'tribes/t1')));
  });
});
