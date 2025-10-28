package com.dentalflow.myapplication.Asistente

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.Api
import com.dentalflow.myapplication.data.remote.model.PacienteDto
import com.dentalflow.myapplication.databinding.ActivityRegistroPacientesBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistroPacientes : AppCompatActivity() {

    private lateinit var b: ActivityRegistroPacientesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegistroPacientesBinding.inflate(layoutInflater)
        setContentView(b.root)

        initTipoSangreSpinner()
        b.btnRegistrarPaciente.setOnClickListener { registrar() }
    }

    private fun initTipoSangreSpinner() {
        val tipos = try {
            resources.getStringArray(R.array.tipo_sangre_array).toList() //error en esta linea
        } catch (_: Exception) {
            listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        }
        b.spTipoSangre.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)
    }

    private fun registrar() {
        val id = b.etCedula.text.toString().trim()
        val nombres = b.etNombres.text.toString().trim()
        val apellidos = b.etApellidos.text.toString().trim()
        val edadTxt = b.etEdad.text.toString().trim()
        val genero = b.etGenero.text.toString().trim()
        val telefono = b.etTelefono.text.toString().trim().ifEmpty { null }
        val correo = b.etCorreo.text.toString().trim().ifEmpty { null }
        val tipoSangre = (b.spTipoSangre.selectedItem?.toString() ?: "").trim()

        if (id.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || genero.isEmpty() || tipoSangre.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_LONG).show()
            return
        }
        val edad = edadTxt.toIntOrNull()
        if (edad == null || edad < 0 || edad > 120) {
            Toast.makeText(this, "Edad inválida (0–120)", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val req = PacienteDto(
                    _id = id,
                    nombres = nombres,
                    apellidos = apellidos,
                    edad = edad,
                    genero = genero,
                    telefono = telefono,
                    direccion = null,
                    correo = correo,
                    tipo_sangre = tipoSangre
                )
                val resp = Api.upsertPaciente(req)
                if (!resp.ok) throw IllegalStateException(resp.error ?: "Error registrando")

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistroPacientes, "Registrado", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistroPacientes, e.message ?: "Error", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.btnRegistrarPaciente.isEnabled = !loading
        b.tvTitulo.text = if (loading) "Registrando..." else "Registro de Paciente"
    }
}
