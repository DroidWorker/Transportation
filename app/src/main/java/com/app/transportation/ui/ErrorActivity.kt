package com.app.transportation.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.app.transportation.R

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        findViewById<TextView>(R.id.tvError).text = intent.getStringExtra("errText")
        findViewById<Button>(R.id.closeErr).setOnClickListener{
            finish()
        }
    }
}