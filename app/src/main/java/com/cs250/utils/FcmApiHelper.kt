package com.cs250.kratos.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FcmApiHelper {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // WARNING: Storing the server key in the app is not secure for production.
    // Ideally, this should be done via Cloud Functions.
    // Go to Firebase Console -> Project Settings -> Cloud Messaging -> Enable Cloud Messaging API (Legacy)
    // Copy the "Server key"
    private const val SERVER_KEY = "6268ffabf4dd85285e15deacfe037c8a26d4cd87" // Ensure this is your correct legacy server key

    fun sendNotification(recipientToken: String, title: String, message: String) {
        val json = JSONObject()
        val notificationJson = JSONObject()

        notificationJson.put("title", title)
        notificationJson.put("body", message)

        json.put("to", recipientToken)
        json.put("notification", notificationJson)

        val body = json.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .addHeader("Authorization", "key=$SERVER_KEY")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle network failure here (e.g., log it)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                // You must close the response body to avoid leaking resources
                response.close()
            }
        })
    }
}
