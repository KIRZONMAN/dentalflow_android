package com.dentalflow.myapplication.Asistente

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.ActivityConfiguracion
import com.dentalflow.myapplication.data.local.SessionManager
import com.dentalflow.myapplication.data.remote.CitaHoyAdapter
import com.dentalflow.myapplication.data.remote.CitaHoyAdapterAsistente
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DashboardAsistente : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private val client = OkHttpClient()
    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_asistente)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCitasHoy)
        val display = intent.getStringExtra("DISPLAY_NAME")
            ?: SessionManager(this).getDisplayName().orEmpty()
        toolbar.title = if (display.isNotBlank()) "Bienvenido, $display" else "Bienvenido"

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_citas -> startActivity(Intent(this, ActivityCitas::class.java))

                R.id.nav_historial -> startActivity(
                    Intent(this, com.dentalflow.myapplication.Odontologo.ActivityAgenda::class.java)
                )

                R.id.nav_registrar -> startActivity(Intent(this, RegistroPacientes::class.java))

                R.id.nav_configuracion ->
                    startActivity(Intent(this, ActivityConfiguracion::class.java))

                R.id.nav_logout -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        SessionManager.clear(this@DashboardAsistente)
                        withContext(Dispatchers.Main) {
                            startActivity(
                                Intent(
                                    this@DashboardAsistente,
                                    com.dentalflow.myapplication.MainActivity::class.java
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                            finish()
                        }
                    }
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        rv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        cargarCitasHoy(rv)
    }

    // CARGAR Citas HOY
    private fun cargarCitasHoy(rv: androidx.recyclerview.widget.RecyclerView) {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val request = Request.Builder()
            .url("$BASE_URL/api/citas/hoy")
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: return

                val lista = mutableListOf<JSONObject>()

                try {
                    val arr = JSONObject(json).getJSONArray("data")
                    for (i in 0 until arr.length()) lista.add(arr.getJSONObject(i))

                } catch (_: Exception) {}

                runOnUiThread {
                    rv.adapter = CitaHoyAdapterAsistente(lista)
                }
            }
        })
    }
}
