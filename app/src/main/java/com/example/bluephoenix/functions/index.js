const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.listSubcollections = functions.https.onCall(async (data, context) => {
  const mainTopic = data.mainTopic;

  if (!mainTopic) {
    throw new functions.https.HttpsError('invalid-argument', 'mainTopic is required');
  }

  try {
    const docRef = admin.firestore().collection('codals').doc(mainTopic);
    const collections = await docRef.listCollections();
    const subCollectionNames = collections.map(col => col.id);
    return subCollectionNames;
  } catch (error) {
    console.error('Error listing subcollections:', error);
    throw new functions.https.HttpsError('internal', 'Unable to list subcollections');
  }
});
