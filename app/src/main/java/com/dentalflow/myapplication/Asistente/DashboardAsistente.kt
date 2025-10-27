package com.dentalflow.myapplication.Asistente

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.local.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardAsistente : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_asistente)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Título
        val display = intent.getStringExtra("DISPLAY_NAME")
            ?: SessionManager(this).getDisplayName().orEmpty()
        if (display.isNotBlank()) toolbar.title = "Bienvenido, $display"

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setCheckedItem(R.id.nav_inicio)

        // Back: primero cierra el drawer
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
                R.id.nav_inicio -> { /* ya aquí */ }

                R.id.nav_citas -> startActivity(
                    Intent(this, com.dentalflow.myapplication.Asistente.ActivityCitas::class.java)
                )

                R.id.nav_historial -> startActivity(
                    Intent(this, com.dentalflow.myapplication.Odontologo.ActivityAgenda::class.java)
                )

                R.id.nav_registrar -> startActivity(
                    Intent(this, com.dentalflow.myapplication.Asistente.RegistroPacientes::class.java)
                )

                R.id.nav_configuracion -> startActivity(
                    Intent(this, com.dentalflow.myapplication.ActivityConfiguracion::class.java)
                )

                R.id.nav_logout -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        SessionManager.clear(this@DashboardAsistente)
                        withContext(Dispatchers.Main) {
                            val i = Intent(
                                this@DashboardAsistente,
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
