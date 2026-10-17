package com.owngame.helpi

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.FloatBuffer
import java.util.Locale
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var apiHost: EditText
    private var session: OrtSession? = null
    private var selectedImage: Bitmap? = null
    private var selectedImageName = "No screenshot selected"
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
            text = "Android vision and model debugging"; textSize = 17f; setTextColor(muted)
            gravity = Gravity.CENTER; setPadding(0, 10, 0, 18)
        }, matchWrap())

        section(content, "PylaAI API diagnostics", fg)
        apiHost = EditText(this).apply {
            hint = "HTTPS API hostname (without https://)"; setSingleLine(true)
            setTextColor(fg); setHintTextColor(muted); setText("localhost")
        }
        content.addView(apiHost, matchWrap())
        content.addView(Button(this).apply {
            text = "Test PylaAI API"
            setOnClickListener { testApiConnection() }
        }, matchWrap())

        section(content, "ONNX model diagnostics", fg)
        content.addView(Button(this).apply {
            text = "Load .onnx model"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                }, PICK_MODEL)
            }
        }, matchWrap())
        content.addView(Button(this).apply {
            text = "Choose screenshot for testing"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
                }, PICK_IMAGE)
            }
        }, matchWrap())
        content.addView(Button(this).apply {
            text = "Run model on screenshot"
            setOnClickListener { runScreenshotInference() }
        }, matchWrap())
        status = TextView(this).apply {
            text = "No model loaded. Select a compatible image-input ONNX model and a screenshot."
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

    private fun section(parent: LinearLayout, title: String, color: Int) {
        parent.addView(TextView(this).apply {
            text = title; textSize = 18f; setTextColor(color); setPadding(0, 20, 0, 6)
        }, matchWrap())
    }

    private fun testApiConnection() {
        val host = apiHost.text.toString().trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (host.isBlank() || host.contains("/") || host.contains(" ")) {
            status.text = "Enter a valid host name, e.g. api.example.com."
            return
        }
        if (host == "localhost" || host.startsWith("127.0.0.1") || host.startsWith("10.0.2.2")) {
            status.text = "On Android, localhost refers to the phone. Enter a reachable HTTPS server hostname."
            return
        }
        val base = "https://" + host
        status.text = "Testing API endpoints…"
        Thread {
            val report = "API host: $base\n\n" +
                checkEndpoint(base + "/check_version", "GET") + "\n\n" +
                checkEndpoint(base + "/get_brawler_list", "POST") +
                "\n\nConnectivity test only; it does not start gameplay."
            runOnUiThread { status.text = report }
        }.start()
    }

    private fun checkEndpoint(endpoint: String, method: String): String {
        var connection: HttpURLConnection? = null
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
            val body = stream?.bufferedReader()?.use { it.readText().take(700) }.orEmpty()
            "$method ${URL(endpoint).path} → HTTP $code\n" + if (body.isBlank()) "(empty response)" else body
        } catch (e: Exception) {
            "$method ${URL(endpoint).path} → FAILED: ${e.javaClass.simpleName}: ${e.message ?: "connection error"}"
        } finally {
            connection?.disconnect()
        }
    }

    private fun runScreenshotInference() {
        val activeSession = session
        val bitmap = selectedImage
        if (activeSession == null) {
            status.text = "Load an ONNX model first."
            return
        }
        if (bitmap == null) {
            status.text = "Choose a screenshot first."
            return
        }
        status.text = "Running ONNX inference on $selectedImageName…"
        Thread {
            try {
                val entry = activeSession.inputInfo.entries.firstOrNull {
                    it.value.info is TensorInfo
                } ?: throw IllegalArgumentException("Model has no tensor input.")
                val tensorInfo = entry.value.info as TensorInfo
                val shape = tensorInfo.shape
                if (shape.size != 4) {
                    throw IllegalArgumentException("Expected a rank-4 image tensor; model input shape is ${shape.contentToString()}.")
                }
                val nchw = shape[1] == 3L || shape[1] == 1L || (shape[1] <= 0L && shape[3] != 3L)
                val height = dimension(if (nchw) shape[2] else shape[1], 640)
                val width = dimension(if (nchw) shape[3] else shape[2], 640)
                val channels = if (nchw) dimension(shape[1], 3) else dimension(shape[3], 3)
                if (channels != 1 && channels != 3) {
                    throw IllegalArgumentException("Only 1-channel or 3-channel image inputs are supported; detected $channels.")
                }
                val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
                val pixels = IntArray(width * height)
                resized.getPixels(pixels, 0, width, 0, 0, width, height)
                val values = FloatArray(width * height * channels)
                if (nchw) {
                    val plane = width * height
                    for (i in pixels.indices) {
                        val color = pixels[i]
                        values[i] = Color.red(color) / 255f
                        if (channels == 3) {
                            values[plane + i] = Color.green(color) / 255f
                            values[plane * 2 + i] = Color.blue(color) / 255f
                        }
                    }
                } else {
                    for (i in pixels.indices) {
                        val color = pixels[i]
                        val p = i * channels
                        values[p] = Color.red(color) / 255f
                        if (channels == 3) {
                            values[p + 1] = Color.green(color) / 255f
                            values[p + 2] = Color.blue(color) / 255f
                        }
                    }
                }
                val tensorShape = if (nchw) longArrayOf(1, channels.toLong(), height.toLong(), width.toLong())
                                  else longArrayOf(1, height.toLong(), width.toLong(), channels.toLong())
                val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(values), tensorShape)
                try {
                    val results = activeSession.run(mapOf(entry.key to inputTensor))
                    try {
                        val summary = results.map { result ->
                            "Output: ${result.key}\n${summarize(result.value.value)}"
                        }.joinToString("\n\n")
                        runOnUiThread {
                            status.text = "Inference completed\nImage: $selectedImageName\nInput: ${entry.key} ${tensorShape.contentToString()}\nResize: ${width}x$height; RGB values normalized to 0..1\n\n$summary\n\nNote: preprocessing and output decoding are model-specific. Compare with PylaAI's expected normalization before using results."
                        }
                    } finally {
                        results.close()
                    }
                } finally {
                    inputTensor.close()
                    if (resized !== bitmap) resized.recycle()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Inference failed\n${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}\n\nSome models require different image normalization, dimensions, or input types."
                }
            }
        }.start()
    }

    private fun dimension(value: Long, fallback: Int): Int {
        if (value <= 0L) return fallback
        if (value > 2048L) throw IllegalArgumentException("Model image dimension $value is too large for this debug screen.")
        return max(1, value.toInt())
    }

    private fun summarize(value: Any?): String {
        if (value == null) return "No value"
        return when (value) {
            is FloatArray -> "FloatArray size=${value.size}, first values=${value.take(12).joinToString { fmt(it) }}"
            is DoubleArray -> "DoubleArray size=${value.size}, first values=${value.take(12).joinToString { fmt(it.toFloat()) }}"
            is LongArray -> "LongArray size=${value.size}, first values=${value.take(12)}"
            is IntArray -> "IntArray size=${value.size}, first values=${value.take(12)}"
            is Array<*> -> "Array size=${value.size}\n" + value.take(4).joinToString("\n") { summarize(it) }
            else -> value.toString().take(1200)
        }
    }

    private fun fmt(value: Float) = String.format(Locale.US, "%.5f", value)

    @Deprecated("Uses document picker result callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            PICK_MODEL -> loadModel(uri)
            PICK_IMAGE -> loadImage(uri)
        }
    }

    private fun loadModel(uri: Uri) {
        status.text = "Loading model…"
        Thread {
            try {
                val file = copyToCache(uri, "selected-model.onnx")
                val newSession = env.createSession(file.absolutePath, OrtSession.SessionOptions())
                val inputs = newSession.inputInfo.entries.joinToString("\n") { (name, info) -> "Input: $name\n  $info" }
                val outputs = newSession.outputInfo.entries.joinToString("\n") { (name, info) -> "Output: $name\n  $info" }
                val old = session
                session = newSession
                old?.close()
                runOnUiThread { status.text = "Model loaded\n\n$inputs\n\n$outputs\n\nChoose a screenshot and run inference to inspect model outputs." }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not load model:\n${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}" }
            }
        }.start()
    }

    private fun loadImage(uri: Uri) {
        status.text = "Loading screenshot…"
        Thread {
            try {
                val bitmap = contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Could not open selected image" }.use { BitmapFactory.decodeStream(it) }
                } ?: throw IllegalArgumentException("Unsupported or invalid image.")
                val old = selectedImage
                selectedImage = bitmap
                old?.recycle()
                selectedImageName = uri.lastPathSegment ?: "selected screenshot"
                runOnUiThread { status.text = "Screenshot loaded: $selectedImageName (${bitmap.width}x${bitmap.height})\nLoad a model or run inference." }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not load screenshot:\n${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}" }
            }
        }.start()
    }

    private fun copyToCache(uri: Uri, name: String): File {
        val file = File(cacheDir, name)
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
        selectedImage?.recycle()
        super.onDestroy()
    }

    companion object {
        private const val PICK_MODEL = 4102
        private const val PICK_IMAGE = 4103
    }
}
