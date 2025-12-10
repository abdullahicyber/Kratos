const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// Trigger when a new message is added to ANY chat document
exports.sendNotificationOnMessage = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const messageData = snapshot.data();
    const senderUid = messageData.senderUid;
    const text = messageData.text;
    const chatId = context.params.chatId;

    // 1. Get the Chat Document to find out who is in this chat
    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    const chatData = chatDoc.data();

    if (!chatData) {
        console.log("No chat data found for chatId:", chatId);
        return;
    }

    // 2. Find the "Other" User (The Recipient)
    // We assume 'userIds' is an array of [uid1, uid2]
    const userIds = chatData.userIds || [];
    const recipientUid = userIds.find((uid) => uid !== senderUid);

    if (!recipientUid) {
        console.log("Could not determine recipient from userIds:", userIds);
        return;
    }

    // 3. Get the Recipient's FCM Token
    const userDoc = await admin.firestore().collection("users").doc(recipientUid).get();
    const fcmToken = userDoc.get("fcmToken");

    if (!fcmToken) {
        console.log("No FCM token found for user:", recipientUid);
        return;
    }

    // 4. Send the Notification
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
