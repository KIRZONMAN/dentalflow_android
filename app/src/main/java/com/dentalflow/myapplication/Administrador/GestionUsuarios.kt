package com.dentalflow.myapplication.Administrador

import android.content.Intent
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dentalflow.myapplication.data.remote.Api
import com.dentalflow.myapplication.databinding.ActivityGestionUsuariosBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class GestionUsuarios : AppCompatActivity() {

    private lateinit var binding: ActivityGestionUsuariosBinding
    private lateinit var adapter: UsuariosAdapter
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastQuery = ""

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            fetchUsuarios(lastQuery)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestionUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = UsuariosAdapter(
            onEdit = { u -> openEdit(u) },
            onDelete = { u -> confirmarYEliminar(u) }
        )
        binding.recyclerUsuarios.layoutManager = LinearLayoutManager(this)
        binding.recyclerUsuarios.adapter = adapter

        binding.btnBuscarUsuario.setOnClickListener {
            lastQuery = binding.inputBuscarUsuario.text?.toString()?.trim().orEmpty()
            fetchUsuarios(lastQuery)
        }
        binding.btnAgregarUsuario.setOnClickListener {
            startActivity(Intent(this@GestionUsuarios, AgregarUsuario::class.java))
        }

        fetchUsuarios("")
    }

    override fun onResume() {
        super.onResume()
        fetchUsuarios("")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancela trabajos en background cuando la Activity muere
        (ioScope.coroutineContext[Job] as? Job)?.cancel()
    }

    private fun fetchUsuarios(search: String) {
        setLoading(true)
        ioScope.launch {
            try {
                val resp = Api.getUsuarios(search)
                if (!resp.ok) throw IllegalStateException(resp.error ?: "Error desconocido")

                val items = (resp.data ?: emptyList()).map { dto ->
                    UsuarioUi(
                        id = dto._id,
                        userId = dto.userId,
                        nombre = dto.nombre,
                        correo = dto.correo,
                        estado = dto.estado,
                        rol = dto.rol
                    )
                }

                withContext(Dispatchers.Main) {
                    val widths = computeColumnWidths(items).withMin(80)
                    applyHeaderWidths(widths)
                    adapter.updateColumnWidths(widths)
                    adapter.submitList(items)
                    setLoading(false, items.isEmpty())
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                Log.e("GestionUsuarios", "fetchUsuarios error", e)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(
                        this@GestionUsuarios,
                        parseErrMsg(e, "Error cargando usuarios"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun confirmarYEliminar(u: UsuarioUi) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar usuario")
            .setMessage("¿Seguro que quieres eliminar a ${u.nombre}?")
            .setPositiveButton("Eliminar") { _, _ -> doDelete(u) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doDelete(u: UsuarioUi) {
        setLoading(true)
        ioScope.launch {
            try {
                val resp = Api.deleteUsuario(u.id)
                if (!resp.ok) throw IllegalStateException(resp.error ?: "Error eliminando")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GestionUsuarios, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                    com.dentalflow.myapplication.data.work.WorkEnqueue.event(
                        context = this@GestionUsuarios,
                        action = "delete_user",
                        title = "Usuario eliminado",
                        message = u.nombre
                    )
                    fetchUsuarios(lastQuery)
                }
            } catch (e: Exception) {
                Log.e("GestionUsuarios", "delete error", e)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(
                        this@GestionUsuarios,
                        parseErrMsg(e, "Error eliminando"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openEdit(u: UsuarioUi) {
        val i = Intent(this, EditUsuarioActivity::class.java).apply {
            putExtra("id", u.id)
            putExtra("userId", u.userId ?: "")
            putExtra("correo", u.correo)
            putExtra("nombre", u.nombre)
            putExtra("estado", u.estado ?: "")
            putExtra("rol", u.rol ?: "")
        }
        editLauncher.launch(i)
    }

    // -------------------- UI helpers & layout widths --------------------

    private fun setLoading(loading: Boolean, empty: Boolean = false) {
        binding.btnBuscarUsuario.isEnabled = !loading
        binding.btnAgregarUsuario.isEnabled = !loading
        binding.tvTablaUsuarios.text = when {
            loading -> "Cargando..."
            empty -> "Sin resultados"
            else -> "Tabla completa"
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun IntArray.withMin(minDp: Int): IntArray {
        val px = dp(minDp)
        return IntArray(size) { i -> this[i].coerceAtLeast(px) }
    }

    private fun measureMaxWidth(texts: List<String>, template: TextView): Int {
        val paint = TextPaint(template.paint)
        var max = 0f
        for (t in texts) max = max.coerceAtLeast(paint.measureText(t))
        val padding = template.paddingLeft + template.paddingRight
        return (ceil(max) + padding + dp(16)).toInt()
    }

    private fun computeColumnWidths(items: List<UsuarioUi>): IntArray {
        val tId = binding.headerId
        val tNombre = binding.headerNombre
        val tCorreo = binding.headerCorreo
        val tEstado = binding.headerEstado
        val tRol = binding.headerRol
        val tAcc = binding.headerAcciones

        val colId = listOf(tId.text.toString()) + items.map { it.id?.takeLast(6) }
        val colNombre = listOf(tNombre.text.toString()) + items.map { it.nombre }
        val colCorreo = listOf(tCorreo.text.toString()) + items.map { it.correo }
        val colEstado = listOf(tEstado.text.toString()) + items.map { it.estado ?: "" }
        val colRol = listOf(tRol.text.toString()) + items.map { it.rol ?: "" }
        val colAcc = listOf(tAcc.text.toString())

        return intArrayOf(
            measureMaxWidth(colId as List<String>, tId),
            measureMaxWidth(colNombre as List<String>, tNombre),
            measureMaxWidth(colCorreo as List<String>, tCorreo),
            measureMaxWidth(colEstado, tEstado),
            measureMaxWidth(colRol, tRol),
            measureMaxWidth(colAcc, tAcc)
        )
    }

    private fun applyHeaderWidths(w: IntArray) {
        binding.headerId.layoutParams.width = w[0]
        binding.headerNombre.layoutParams.width = w[1]
        binding.headerCorreo.layoutParams.width = w[2]
        binding.headerEstado.layoutParams.width = w[3]
        binding.headerRol.layoutParams.width = w[4]
        binding.headerAcciones.layoutParams.width = w[5]
        binding.headerId.requestLayout()
        binding.headerNombre.requestLayout()
        binding.headerCorreo.requestLayout()
        binding.headerEstado.requestLayout()
        binding.headerRol.requestLayout()
        binding.headerAcciones.requestLayout()
    }

    // -------------------- Error helpers --------------------

    private fun parseErrMsg(e: Throwable, fallback: String): String {
        val raw = e.message?.trim().orEmpty()
        return when {
            raw.isBlank() -> fallback
            raw.length > 300 -> raw.take(300) + "..."
            else -> raw
        }
    }
}
