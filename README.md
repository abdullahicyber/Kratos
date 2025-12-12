Kratos — Fitness + Social Chat App (Android / Kotlin)

A modern Android app combining simple workout tracking, streaks, calorie estimation, and real-time 1:1 chat.

Kratos helps beginner and intermediate gym-goers stay motivated through a lightweight fitness workflow and built-in social accountability. 
Users can log workouts, maintain streaks, track calories burned, send direct messages, and receive push notifications when a new message arrives.

🚀 Features
🔐 Authentication

Firebase Email/Password login

Automatic routing based on authentication state

👤 Profile Management

“Create Profile” screen for new users

“My Account” screen with profile details and sign-out

💬 1:1 Chat

Real-time Firestore messaging

Smooth UI updates powered by Kotlin Flow

Push notifications via Firebase Cloud Messaging (FCM), even when the app is closed

🏋️ Workout Tracking

Calorie Calculator: Enter workout duration → estimate calories burned

Weekly Calorie Summary: Track total calories burned each week

Daily Streak System: Streak increases when a workout is logged each day

🛠 How to Build & Run Kratos
1. Clone the repository to your local machine.

2. Create a Firebase project, register the Android app, download the google-services.json file, and place it inside the app/ module.

3. Sync the project with Gradle to load all Firebase dependencies.

4. Build the project using Android Studio’s Build menu.

5. Run the app on an emulator or physical Android device of your choice.
