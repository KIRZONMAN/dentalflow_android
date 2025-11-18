package com.dentalflow.myapplication.Odontologo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.dentalflow.myapplication.*
import com.dentalflow.myapplication.data.local.SessionManager
import com.dentalflow.myapplication.data.remote.CitaHoyAdapter
import com.dentalflow.myapplication.databinding.ActivityDashboardOdontologoBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DashboardOdontologo : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardOdontologoBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardOdontologoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ----------------------------
        //   Toolbar + Drawer Setup
        // ----------------------------
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navigationView

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Nombre en Toolbar
        val display = intent.getStringExtra("DISPLAY_NAME")
            ?: SessionManager(this).getDisplayName().orEmpty()
        toolbar.title = if (display.isNotBlank()) "Bienvenido, $display" else "Bienvenido"

        // Back para cerrar drawer primero
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        // ----------------------------
        //  Navegación del menú lateral
        // ----------------------------
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> { /* estás aquí */ }
                R.id.nav_agenda -> startActivity(Intent(this, ActivityAgenda::class.java))
                R.id.nav_ordenes -> startActivity(Intent(this, ActivityOrdenes::class.java))
                R.id.nav_protesis -> startActivity(Intent(this, SolicitudProtesis::class.java))
                R.id.nav_insumos -> startActivity(Intent(this, ActivityInsumos::class.java))
                R.id.nav_configuracion -> startActivity(Intent(this, ActivityConfiguracion::class.java))

                R.id.nav_logout -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        SessionManager.clear(this@DashboardOdontologo)
                        withContext(Dispatchers.Main) {
                            val intent = Intent(
                                this@DashboardOdontologo,
                                MainActivity::class.java
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // --------------------------------
        //   Botones y Chips del Dashboard
        // --------------------------------
        binding.btnVerAgenda.setOnClickListener {
            startActivity(Intent(this, ActivityAgenda::class.java))
        }

        binding.btnIrOrdenes.setOnClickListener {
            startActivity(Intent(this, ActivityOrdenes::class.java))
        }

        binding.chipInsumos.setOnClickListener {
            startActivity(Intent(this, ActivityInsumos::class.java))
        }

        binding.chipProtesis.setOnClickListener {
            startActivity(Intent(this, SolicitudProtesis::class.java))
        }

        // -------------------------
        //     Configurar Recycler
        // -------------------------
        binding.rvCitasHoy.layoutManager = LinearLayoutManager(this)

        // Cargar citas
        cargarCitasHoy()
    }

    // ---------------------------------------------------------
    //       FUNCIÓN PARA OBTENER LAS CITAS DE HOY
    // ---------------------------------------------------------
    private fun cargarCitasHoy() {
        val url =
            "https://lucid-youthfulness-production.up.railway.app/api/citas/hoy"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // Puedes mostrar un SweetAlert aquí
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)

                if (!json.optBoolean("ok")) return

                val dataArray = json.optJSONArray("data") ?: return
                val lista = mutableListOf<JSONObject>()

                for (i in 0 until dataArray.length()) {
                    lista.add(dataArray.getJSONObject(i))
                }

                runOnUiThread {
                    binding.rvCitasHoy.adapter = CitaHoyAdapter(lista)
                }
            }
        })
    }
}
