package com.example.formalizetext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.formalizetext.ui.theme.FormalizeTextTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class Concise : ComponentActivity() {
    private val client = OkHttpClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            // Get the text from the intent extra.
            val selectedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: ""

            getResp(selectedText) {response ->

                var message = response.replace("\n", " ")
                message = message.substring(2)
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("text", message)
                clipboardManager.setPrimaryClip(clipData)
            }
            finish()
        }
    }
    fun getResp(query:String, callback: (String) -> Unit) {

        val apiKey = "OPENAI_API_KEY"
        val url = "https://api.openai.com/v1/engines/text-davinci-003/completions"

        val reqBody = """
        {
            "prompt": "You are a writing assistant that helps make texts more concise. Make the following text more concise: $query",
            "max_tokens": 500,
            "temperature": 0
        }
    """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(reqBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    Log.v("data", body)
                }

                val json = JSONObject(body)
                val nestedJson = JSONObject(json.getJSONArray("choices").get(0).toString())
                val editedMessage = nestedJson.getString("text")

                callback(editedMessage)
            }
        })
    }
}