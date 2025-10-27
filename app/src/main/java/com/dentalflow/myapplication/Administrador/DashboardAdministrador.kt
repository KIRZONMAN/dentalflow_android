package com.dentalflow.myapplication.Administrador

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.Asistente.DashboardAsistente
import com.dentalflow.myapplication.Laboratorista.DashboardLaboratorista
import com.dentalflow.myapplication.MainActivity
import com.dentalflow.myapplication.Odontologo.DashboardOdontologo
import com.dentalflow.myapplication.data.local.SessionManager
import com.dentalflow.myapplication.databinding.ActivityDashboardAdministradorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Pantallas extra del administrador
import com.dentalflow.myapplication.Administrador.GestionProveedoresActivity
import com.dentalflow.myapplication.Administrador.ProcedimientosActivity

class DashboardAdministrador : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardAdministradorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardAdministradorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gestión de usuarios
        binding.btnUsuarios.setOnClickListener {
            startActivity(Intent(this, GestionUsuarios::class.java))
        }
        // Asistente
        binding.btnAsistente.setOnClickListener {
            startActivity(Intent(this, DashboardAsistente::class.java))
        }
        // Odontólogo
        binding.btnOdontologo.setOnClickListener {
            startActivity(Intent(this, DashboardOdontologo::class.java))
        }
        // Laboratorista
        binding.btnLaboratorista.setOnClickListener {
            startActivity(Intent(this, DashboardLaboratorista::class.java))
        }
        // Proveedores
        binding.btnProveedores.setOnClickListener {
            startActivity(Intent(this, GestionProveedoresActivity::class.java))
        }
        // Procedimientos
        binding.btnProcedimientos.setOnClickListener {
            startActivity(Intent(this, ProcedimientosActivity::class.java))
        }

        // Cerrar sesión (ahora sólo limpiamos estado local y vamos al login)
        binding.btnCerrarSesion.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // Aquí ya no hay logout remoto de Realm; sólo limpieza local
                SessionManager.clear(this@DashboardAdministrador)
                withContext(Dispatchers.Main) {
                    startActivity(
                        Intent(this@DashboardAdministrador, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
            }
        }
    }
}
