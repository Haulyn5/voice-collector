package com.example.voicecollector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startActivityButton:Button = findViewById(R.id.startActivityButton)
        startActivityButton.setOnClickListener {
            val intent:Intent = Intent(this, CustomizableRecorderActivity::class.java)
            startActivity(intent)
        }
    }
}