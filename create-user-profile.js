// Run this with: node create-user-profile.js YOUR_USER_ID YOUR_EMAIL
const admin = require('firebase-admin');
const serviceAccount = require('./path/to/serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function createUserProfile(userId, email, tier = 'FREE') {
  const profile = {
    userId: userId,
    email: email,
    tier: tier,
    repoLimit: tier === 'FREE' ? 2 : -1,
    aiAnalysisRemaining: tier === 'FREE' ? 0 : -1,
    showAds: tier === 'FREE',
    subscriptionExpiresAt: tier === 'PRO' ? 1893456000000 : null,
    createdAt: Date.now()
  };

  await db.collection('user_profiles').doc(userId).set(profile);
  console.log(`✅ ${tier} profile created for ${email}`);
  console.log('Profile:', profile);
}

const userId = process.argv[2];
const email = process.argv[3];
const tier = process.argv[4] || 'FREE';

if (!userId || !email) {
  console.log('Usage: node create-user-profile.js USER_ID EMAIL [FREE|PRO]');
  console.log('Example: node create-user-profile.js abc123 test@example.com FREE');
  process.exit(1);
}

createUserProfile(userId, email, tier)
  .then(() => process.exit(0))
  .catch(err => {
    console.error('Error:', err);
    process.exit(1);
  });
