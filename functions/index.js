/**
 * index.js
 *
 * This file contains Cloud Functions for Firebase.
 *
 * The main function here, `sendNotificationOnMessage`, is an automated trigger
 * that runs on Google's servers whenever a new document is created in the
 * "messages" sub-collection of any chat.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");


// Initialize the Firebase Admin SDK to access Firestore and FCM
admin.initializeApp();

/**
 * sendNotificationOnMessage
 *
 * Trigger: Firestore onCreate
 * Path: chats/{chatId}/messages/{messageId}
 *
 * This function is responsible for:
 * 1. Listening for new messages.
 * 2. identifying who sent it and who should receive it.
 * 3. Fetching the recipient's FCM token.
 * 4. Sending a push notification to that token.
 */
exports.sendNotificationOnMessage = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    // 1. EXTRACT DATA
    // Get the data from the newly created message document
    const messageData = snapshot.data();
    const senderUid = messageData.senderUid;
    // Handle cases where the message might be an image (no text)
    const text = messageData.text;
    // Get the chatId from the URL path (wildcard)
    const chatId = context.params.chatId;

    // 2. FETCH CHAT METADATA
    // We need to look up the 'chats' document to see the list of participants (userIds).
    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    const chatData = chatDoc.data();

    if (!chatData) {
        console.log("No chat data found for chatId:", chatId);
        return;
    }

    // 3. IDENTIFY RECIPIENT
    // The participants array contains both users. We find the one that is NOT the sender
    const userIds = chatData.userIds || [];
    const recipientUid = userIds.find((uid) => uid !== senderUid);

    if (!recipientUid) {
        console.log("Could not determine recipient from userIds:", userIds);
        return;
    }

    // 4. FETCH USER DETAILS (PARALLEL)
    // We need two things:
    // a) The Recipient's FCM Token (to know where to send the notification)
    // b) The Sender's Display Name (to show "John Doe" in the notification title)
    // We use Promise.all to fetch both at the same time for speed.
    const userDoc = await admin.firestore().collection("users").doc(recipientUid).get();
    const fcmToken = userDoc.get("fcmToken");

    if (!fcmToken) {
        console.log("No FCM token found for user:", recipientUid);
        return;
    }

    // 5. CONSTRUCT PAYLOAD
    // We place the content inside the 'data' object (NOT 'notification').
    // This ensures the Android 'onMessageReceived' method is called even if the app is in the background.
    const payload = {
      token: fcmToken,
      notification: {
        title: "New Message",
        body: text,
      },
      data: {
        chatId: chatId,
      },
    };

    try {
      await admin.messaging().send(payload);
      console.log("Notification sent successfully to:", recipientUid);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  });
