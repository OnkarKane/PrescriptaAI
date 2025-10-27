package com.example.prescriptaai

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.prescriptaai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Set the content view to the root of the binding
        setContentView(binding.root)

        // You can now access your views like this:
        // binding.myTextView.text = "Hello from MainActivity!"
    }
}