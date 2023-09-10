package com.example.formalizetext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.formalizetext.ui.theme.FormalizeTextTheme
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Timeout
import org.json.JSONObject
import java.io.IOException

class Formalize : ComponentActivity() {
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
            "prompt": "I need to send a message to my boss. Please make the following message more formal: $query",
            "max_tokens": 200,
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
