package com.dentalflow.myapplication.Administrador

import android.os.Bundle
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.data.remote.Api
import com.dentalflow.myapplication.data.remote.model.UsuarioPatchReq
import com.dentalflow.myapplication.databinding.ActivityEditUsuarioBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditUsuarioActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditUsuarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditUsuarioBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnVolver.setOnClickListener { finish() }

        val id = intent.getStringExtra("id").orEmpty()
        val userId = intent.getStringExtra("userId").orEmpty()
        val correo = intent.getStringExtra("correo").orEmpty()
        val nombre = intent.getStringExtra("nombre").orEmpty()
        val estado = intent.getStringExtra("estado").orEmpty()
        val rol = intent.getStringExtra("rol").orEmpty()

        val (nombres, apellidos) = splitNombre(nombre)
        b.etNombres.setText(nombres)
        b.etApellidos.setText(apellidos)
        b.etCorreo.setText(correo)
        setSpinnerToValue(b.spEstado, estado)
        setSpinnerToValue(b.spRol, rol)

        b.btnGuardar.setOnClickListener {
            guardarCambios(id, userId, correo)
        }
    }

    private fun guardarCambios(id: String, userId: String, correoOriginal: String) {
        val nombres = b.etNombres.text.toString().trim()
        val apellidos = b.etApellidos.text.toString().trim()
        val dir = b.etDireccion.text.toString().trim()
        val tel = b.etTelefono.text.toString().trim()
        val estado = (b.spEstado.selectedItem?.toString() ?: "").trim().lowercase()
        val rol = (b.spRol.selectedItem?.toString() ?: "").trim()
        val correo = b.etCorreo.text.toString().trim().ifEmpty { correoOriginal }
        val esp = b.etEspecialidad.text.toString().trim()

        if (nombres.isEmpty()) {
            Toast.makeText(this, "Nombres requeridos", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val espList = esp.takeIf { it.isNotEmpty() }?.let { listOf(it) }

                val patch = UsuarioPatchReq(
                    userId = userId.ifBlank { null },
                    nombres = nombres,
                    apellidos = apellidos,
                    correo = correo,
                    estado = estado,
                    rol = rol,
                    direccion = dir.ifEmpty { null },
                    telefono = tel.ifEmpty { null },
                    especialidad = espList          // << aquí
                )

                val resp = Api.patchUsuario(id, patch)
                if (!resp.ok) throw IllegalStateException(resp.error ?: "Error actualizando")

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditUsuarioActivity, "Guardado ✔", Toast.LENGTH_LONG).show()
                    com.dentalflow.myapplication.data.work.WorkEnqueue.event(
                        context = this@EditUsuarioActivity,
                        action = "edit_user",
                        title = "Usuario actualizado",
                        message = "${b.etNombres.text} ${b.etApellidos.text} → ${b.spRol.selectedItem}"
                    )
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditUsuarioActivity, e.message ?: "Error", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            }
        }
    }

    private fun setSpinnerToValue(sp: Spinner, value: String) {
        val count = sp.adapter?.count ?: 0
        val idx = (0 until count).firstOrNull { i ->
            sp.adapter.getItem(i)?.toString()?.equals(value, true) == true
        } ?: 0
        sp.setSelection(idx)
    }

    private fun splitNombre(full: String): Pair<String, String> {
        val p = full.trim().split(Regex("\\s+"))
        return if (p.size <= 1) full to "" else p.dropLast(1).joinToString(" ") to p.last()
    }

    private fun setLoading(loading: Boolean) {
        b.btnGuardar.isEnabled = !loading
        b.btnVolver.isEnabled = !loading
        b.tvTitle.text = if (loading) "Guardando..." else "Editar usuario"
    }
}
