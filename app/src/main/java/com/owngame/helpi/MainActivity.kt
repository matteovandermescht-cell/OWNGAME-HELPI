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
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(14, 17, 24)
        val foreground = Color.rgb(240, 243, 250)
        val muted = Color.rgb(165, 175, 194)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "OWNGAME HELPI"
            textSize = 28f
            setTextColor(foreground)
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val description = TextView(this).apply {
            text = "Android project shell\n\nPylaAI was made for desktop use and cannot be converted into a working Android app by renaming files alone. This APK currently provides project information only; it does not control games or automate gameplay."
            textSize = 16f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        root.addView(description, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val button = Button(this).apply {
            text = "Open PylaAI project"
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PylaAI/PylaAI")))
            }
        }
        root.addView(button, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
    }
}
