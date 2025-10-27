package com.dentalflow.myapplication.Odontologo

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.ActivityConfiguracion
import com.dentalflow.myapplication.data.local.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardOdontologo : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_odontologo)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Título: intent o sesión local
        val display = intent.getStringExtra("DISPLAY_NAME")
            ?: SessionManager(this).getDisplayName().orEmpty()
        toolbar.title = if (display.isNotBlank()) "Bienvenido, $display" else "Bienvenido"

        // Back: primero cierra el drawer
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        // Menú lateral
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio   -> { /* ya aquí */ }
                R.id.nav_agenda   -> { startActivity(Intent(this, ActivityAgenda::class.java)) }
                R.id.nav_ordenes  -> { startActivity(Intent(this, ActivityOrdenes::class.java)) }
                R.id.nav_protesis -> { startActivity(Intent(this, SolicitudProtesis::class.java)) }
                R.id.nav_insumos  -> { startActivity(Intent(this, ActivityInsumos::class.java))  }
                R.id.nav_configuracion -> {
                    startActivity(Intent(this, ActivityConfiguracion::class.java))
                }
                R.id.nav_logout   -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        SessionManager.clear(this@DashboardOdontologo)
                        withContext(Dispatchers.Main) {
                            val i = Intent(
                                this@DashboardOdontologo,
                                com.dentalflow.myapplication.MainActivity::class.java
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(i)
                            finish()
                        }
                    }
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }
}
