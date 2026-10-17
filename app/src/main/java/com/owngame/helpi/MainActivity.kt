package com.owngame.helpi

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var session: OrtSession? = null
    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val background = Color.rgb(14, 17, 24)
        val foreground = Color.rgb(240, 243, 250)
        val muted = Color.rgb(165, 175, 194)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(background)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "OWNGAME HELPI"
            textSize = 27f
            setTextColor(foreground)
            gravity = Gravity.CENTER
        }
        content.addView(title, matchWrap())
        val subtitle = TextView(this).apply {
            text = "Android ONNX model runtime"
            textSize = 17f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }
        content.addView(subtitle, matchWrap())

        val choose = Button(this).apply {
            text = "Load PylaAI .onnx model"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(intent, PICK_MODEL)
            }
        }
        content.addView(choose, matchWrap())

        status = TextView(this).apply {
            text = "Choose a model file to test whether Android ONNX Runtime can load it."
            textSize = 15f
            setTextColor(foreground)
            setPadding(0, 20, 0, 20)
        }
        content.addView(status, matchWrap())

        val source = Button(this).apply {
            text = "Open original PylaAI source"
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PylaAI/PylaAI")))
            }
        }
        content.addView(source, matchWrap())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
    }

    @Deprecated("Uses document picker result callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_MODEL || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        status.text = "Loading model…"
        Thread {
            try {
                val modelFile = copyModelToCache(uri)
                val newSession = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
                session?.close()
                session = newSession
                val inputs = newSession.inputInfo.entries.joinToString("\n") { (name, info) ->
                    "Input: $name\n  $info"
                }
                val outputs = newSession.outputInfo.entries.joinToString("\n") { (name, info) ->
                    "Output: $name\n  $info"
                }
                val report = "Model loaded successfully\n\n$inputs\n\n$outputs\n\nModel loading is verified; the full PylaAI device-control and gameplay pipeline is not ported yet."
                runOnUiThread { status.text = report }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Could not load this model on Android.\n\n${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}"
                }
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

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        session?.close()
        super.onDestroy()
    }

    companion object {
        private const val PICK_MODEL = 4102
    }
}
