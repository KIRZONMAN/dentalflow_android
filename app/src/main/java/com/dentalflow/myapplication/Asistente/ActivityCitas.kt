package com.dentalflow.myapplication.Asistente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dentalflow.myapplication.databinding.ActivityCitasBinding

class ActivityCitas : AppCompatActivity() {
    private lateinit var binding: ActivityCitasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCrear.setOnClickListener {
            var irAgendarCita = Intent(this, ActivityCrearCita::class.java)
            startActivity(irAgendarCita)
        }

    }
}