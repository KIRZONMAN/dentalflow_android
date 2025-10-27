package com.dentalflow.myapplication.Laboratorista

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.MainActivity
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.ActivityConfiguracion
import com.dentalflow.myapplication.Odontologo.ActivityInsumos
import com.dentalflow.myapplication.Odontologo.ActivityOrdenes
import com.dentalflow.myapplication.data.local.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardLaboratorista : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_laboratorista)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Nombre en bienvenida (intent o sesión)
        findViewById<TextView>(R.id.txtBienvenida)?.let { tv ->
            val name = intent.getStringExtra("DISPLAY_NAME")
                ?: SessionManager(this).getDisplayName().orEmpty()
            if (name.isNotBlank()) tv.text = "Bienvenido, $name 👋"
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Back: primero cierra el drawer si está abierto
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
                R.id.nav_pedidos -> startActivity(Intent(this, PedidosLaboratorio::class.java))
                R.id.nav_insumos -> startActivity(Intent(this, ActivityInsumos::class.java))
                R.id.nav_config -> startActivity(Intent(this, ActivityConfiguracion::class.java))
                R.id.nav_logout -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        SessionManager.clear(this@DashboardLaboratorista)
                        withContext(Dispatchers.Main) {
                            val i = Intent(this@DashboardLaboratorista, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
