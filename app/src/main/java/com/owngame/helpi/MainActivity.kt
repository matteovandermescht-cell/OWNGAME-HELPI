package com.owngame.helpi

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var apiHost: EditText
    private var session: OrtSession? = null
    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fg = Color.rgb(240, 243, 250)
        val muted = Color.rgb(165, 175, 194)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(Color.rgb(14, 17, 24))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        content.addView(TextView(this).apply {
            text = "OWNGAME HELPI"; textSize = 27f; setTextColor(fg); gravity = Gravity.CENTER
        }, matchWrap())
        content.addView(TextView(this).apply {
            text = "PylaAI Android compatibility tools"; textSize = 17f; setTextColor(muted)
            gravity = Gravity.CENTER; setPadding(0, 10, 0, 18)
        }, matchWrap())
        content.addView(TextView(this).apply {
            text = "PylaAI API connection"; textSize = 18f; setTextColor(fg); setPadding(0, 8, 0, 6)
        }, matchWrap())
        apiHost = EditText(this).apply {
            hint = "API hostname (without https://)"; setSingleLine(true)
            setTextColor(fg); setHintTextColor(muted); setText("localhost")
        }
        content.addView(apiHost, matchWrap())
        content.addView(Button(this).apply {
            text = "Test PylaAI API"; setOnClickListener { testApiConnection() }
        }, matchWrap())
        content.addView(TextView(this).apply {
            text = "ONNX model"; textSize = 18f; setTextColor(fg); setPadding(0, 20, 0, 6)
        }, matchWrap())
        content.addView(Button(this).apply {
            text = "Load PylaAI .onnx model"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                }, PICK_MODEL)
            }
        }, matchWrap())
        status = TextView(this).apply {
            text = "Tests /check_version and /get_brawler_list. localhost on Android means this phone itself."
            textSize = 14f; setTextColor(fg); setPadding(0, 18, 0, 18)
        }
        content.addView(status, matchWrap())
        content.addView(Button(this).apply {
            text = "Open original PylaAI source"
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PylaAI/PylaAI"))) }
        }, matchWrap())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    private fun testApiConnection() {
        var host = apiHost.text.toString().trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (host.isBlank() || host.contains("/") || host.contains(" ")) {
            status.text = "Enter a valid API hostname, e.g. api.example.com."; return
        }
        val base = "https://" + host
        status.text = "Testing API endpoints…"
        Thread {
            val report = "API host: " + base + "\n\n" +
                checkEndpoint(base + "/check_version", "GET") + "\n\n" +
                checkEndpoint(base + "/get_brawler_list", "POST") +
                "\n\nConnectivity check only; this does not start gameplay."
            runOnUiThread { status.text = report }
        }.start()
    }

    private fun checkEndpoint(endpoint: String, method: String): String {
        var connection: HttpURLConnection? = null
        val path = endpoint.substringAfter(".com", endpoint)
        return try {
            connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/json")
            if (method == "POST") {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText().take(500) }.orEmpty()
            method + " " + URL(endpoint).path + " → HTTP " + code + "\n" +
                if (body.isBlank()) "(empty response)" else body
        } catch (e: Exception) {
            method + " " + path + " → FAILED: " + e.javaClass.simpleName + ": " + (e.message ?: "connection error")
        } finally {
            connection?.disconnect()
        }
    }

    @Deprecated("Uses document picker result callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_MODEL || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        status.text = "Loading model…"
        Thread {
            try {
                val file = copyModelToCache(uri)
                val newSession = env.createSession(file.absolutePath, OrtSession.SessionOptions())
                session?.close(); session = newSession
                val inputs = newSession.inputInfo.entries.joinToString("\n") { (name, info) -> "Input: " + name + "\n  " + info }
                val outputs = newSession.outputInfo.entries.joinToString("\n") { (name, info) -> "Output: " + name + "\n  " + info }
                runOnUiThread { status.text = "Model loaded successfully\n\n" + inputs + "\n\n" + outputs + "\n\nModel metadata only; full PylaAI inference/device control is not ported." }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not load model:\n" + e.javaClass.simpleName + ": " + (e.message ?: "Unknown error") }
            }
        }.start()
    }

    private fun copyModelToCache(uri: Uri): File {
        val file = File(cacheDir, "selected-model.onnx")
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open selected file" }.use { source ->
                file.outputStream().use { target -> source.copyTo(target) }
            }
        }
        return file
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun onDestroy() {
        session?.close()
        super.onDestroy()
    }

    companion object { private const val PICK_MODEL = 4102 }
}