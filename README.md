Kratos — Firebase Chat App (Android / Kotlin)

A clean, MVVM-structured Android app that demonstrates Firebase Authentication, Cloud Firestore, and 1:1 chat using Kotlin coroutines, Flow, and modern Android architecture.

Features

Authentication
• Sign in with Email/Password via FirebaseUI
• Automatically create Firestore user profiles on first login

Profile Management
• Stores users/{uid} documents with name, email, photo, createdAt
• “Create Profile” screen for new users

Chat
• Real-time Firestore messages
• Deterministic chatId per user pair (sorted UIDs)
• Sent and received message layouts

People List
• Shows all registered users from Firestore
• Tap to open a 1-on-1 chat

Account
• Displays user info and allows sign-out

Navigation
• Splash → Sign In → (Create Profile) → Main (People / My Account)

App Flow

SplashActivity
├─ if not signed in → SignInActivity
│ └─ may go to CreateProfileActivity
└─ if signed in → MainActivity
├─ PeopleFragment → ChatActivity (pick a user)
└─ MyAccountFragment (sign out)

Project Structure

data/ — Firebase repositories for Auth & Chat
model/ — Kotlin data models (UserProfile, Message)
ui/ — Fragments for People, Account, Dashboard, Home, Notifications
activities — App entry points (SplashActivity, SignInActivity, MainActivity, etc.)

Key classes:

AuthRepository → Syncs FirebaseAuth and Firestore user profiles.

ChatRepository → Handles message sending, chat IDs, and Flow listeners.

PeopleFragment → Lists all users and starts chats.

ChatActivity → Handles live chat stream & message sending.

MyAccountFragment → Displays user info and sign-out.

CreateProfileActivity → Collects display name for new users.

Tech Stack

Language: Kotlin
Architecture: MVVM
UI: ViewBinding, RecyclerView
Async: Coroutines, Flow
Auth: FirebaseUI (Email/Password)
Database: Cloud Firestore
Navigation: Fragments + BottomNavigationView

Requirements

Android Studio Ladybug or newer

Android SDK 24+

JDK 17

A Firebase Project (you’ll set this up below)

Internet connection (Firestore & Auth require it)

Firebase Setup (for your own account)

Since you can’t use the original API key, you must link your own Firebase project.
Follow these steps carefully 

1️⃣ Create a Firebase Project

Go to https://console.firebase.google.com/
 → Add project

Name it (e.g., kratos-chat-demo)

Disable Google Analytics (optional)

Click Create Project

2️⃣ Register the Android App in Firebase

Go to Project settings → Your apps → Add app → Android

Fill in:
• Package name: com.cs250.kratos (must match your moduleId)
• App nickname: optional
• SHA fingerprints: recommended for Auth

Run this in terminal:
./gradlew signingReport

Copy SHA-1 and SHA-256 for the debug variant, and add them to Firebase under App fingerprints.

Click Register app, then download the google-services.json file.

Place it at:
app/google-services.json

3️⃣ Enable Firebase Products

Authentication

Firebase Console → Build → Authentication → Sign-in method

Enable Email/Password

Firestore Database

Firebase Console → Build → Firestore Database → Create database

Choose a region

Start in Test Mode for development

Example test rules:

{
  "rules": {
    "match": "/**" {
      "allow read, write: if request.auth != null";
    }
  }
}

4️⃣ Add Firebase SDKs to Gradle

In app/build.gradle:

plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    implementation 'com.google.firebase:firebase-auth:23.0.0'
    implementation 'com.google.firebase:firebase-firestore:25.0.0'
    implementation 'com.firebaseui:firebase-ui-auth:8.0.2'
}

In project-level build.gradle:

buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.2'
    }
}
Sync Gradle after editing.

5️⃣ Run the App

Connect a device or emulator

Build and Run

SplashActivity checks FirebaseAuth state

If signed out → SignInActivity

If signed in → MainActivity

Sign in, create profile, and start chatting 🎉

Firestore Data Model

users/{uid}:
{
  "uid": "abc123",
  "displayName": "Alice",
  "email": "alice@example.com",
  "photoUrl": null,
  "createdAt": 1717080000000
}
chats/{chatId}/messages/{messageId}:

json
Copy code
{
  "id": "uuid",
  "chatId": "uid1_uid2",
  "senderUid": "uid1",
  "text": "Hey there!",
  "sentAt": 1717080000000
}

Firestore Rules (Development)

Use these only for testing (secure them before production):

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}

Architecture Summary

SplashActivity
├─ checks FirebaseAuth state
├─ → SignInActivity (via FirebaseUI Auth)
│ └─ CreateProfileActivity → ensures Firestore user doc
└─ → MainActivity (BottomNav)
├─ PeopleFragment → lists users → ChatActivity
└─ MyAccountFragment → profile + sign out

Common Issues
Problem	Cause	Fix
Missing google-services.json	Firebase not linked	Add file to app folder
Sign-in fails	Email provider not enabled	Enable Email/Password in Firebase Auth
Chat empty	Firestore rules or offline	Check rules, ensure signed in
"Missing API key"	Using another user’s Firebase config	Register your own Firebase project

Developer Notes

Firestore debug logging is enabled in SplashActivity (can disable later).

All Firebase calls use Coroutines (.await()) or Flow for async.

ViewBinding replaces findViewById.

Uses Activity Result API for FirebaseUI.

Contributing

Fork the repo

Create a feature branch (git checkout -b feature/xyz)

Commit and push

Open a pull request

License

This project is for educational use in CS250 / Kratos demos.
You may modify and reuse freely without sharing API keys or Firebase credentials.

Quick Reference
Screen	Class	Purpose
Splash	SplashActivity	Checks auth, routes user
Sign In	SignInActivity	FirebaseUI login
Create Profile	CreateProfileActivity	Setup display name
Main	MainActivity	Hosts fragments
People	PeopleFragment	List all users
Chat	ChatActivity	1-on-1 messaging
My Account	MyAccountFragment	User info & sign out
Made with Kotlin + Firebase
Built for learning, collaboration, and modern Android development :)
