package com.dentalflow.myapplication.Administrador

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.data.remote.Api
import com.dentalflow.myapplication.data.remote.UsuarioCreateReq
import com.dentalflow.myapplication.databinding.ActivityAgregarUsuarioBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgregarUsuario : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarUsuarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolver.setOnClickListener { finish() }
        binding.btnRegistrar.setOnClickListener { registrarUsuario() }
    }

    private fun registrarUsuario() {
        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString().trim() // en demo no se envía
        val direccion = binding.etDireccion.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val estado = (binding.spEstado.selectedItem?.toString() ?: "activo").lowercase()
        val rol = binding.spRol.selectedItem?.toString() ?: "Asistente"
        val especialidad = binding.etEspecialidad.text.toString().trim()

        if (nombres.isEmpty() || apellidos.isEmpty() || correo.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val req = UsuarioCreateReq(
                    nombres = nombres,
                    apellidos = apellidos,
                    correo = correo,
                    estado = estado,
                    rol = rol,
                    direccion = direccion.ifEmpty { null },
                    telefono = telefono.ifEmpty { null },
                    especialidad = if (especialidad.isNotEmpty()) especialidad else null

                )
                // dentro de registrarUsuario(), antes de Api.createUsuario(req)
                android.util.Log.d("AgregarUsuario", "payload=" + com.google.gson.Gson().toJson(req))
                val resp = Api.createUsuario(req)
                if (!resp.ok) throw IllegalStateException(resp.error ?: "Error creando usuario")

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AgregarUsuario, "Usuario creado", Toast.LENGTH_LONG).show()
                    // NOTI: usuario creado
                    com.dentalflow.myapplication.data.work.WorkEnqueue.event(
                        context = this@AgregarUsuario,
                        action  = "create_user",
                        title   = "Usuario creado",
                        message = "${nombres} ${apellidos} (${rol})"
                    )
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AgregarUsuario, e.message ?: "Error", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegistrar.isEnabled = !loading
        binding.btnVolver.isEnabled = !loading
        binding.tvTitle.text = if (loading) "Registrando..." else "Registrar Nuevo Usuario"
    }
}
