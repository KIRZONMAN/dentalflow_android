package com.dentalflow.myapplication.Administrador

import android.os.Bundle
import android.text.InputFilter
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dentalflow.myapplication.databinding.ActivityProcedimientosBinding
import java.text.NumberFormat
import java.util.Locale

data class Procedimiento(
    val id: Int,
    val nombre: String,
    val costo: Double
)

class ProcedimientosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcedimientosBinding
    private lateinit var adapter: ProcedimientosAdapter

    // Demo data (puedes borrar luego)
    private val procedimientos = mutableListOf(
        Procedimiento(1, "Consulta General",      50000.0),
        Procedimiento(2, "Radiografía",          120000.0),
        Procedimiento(3, "Extracción Dental",    300000.0),
        Procedimiento(4, "Limpieza Dental",       80000.0),
        Procedimiento(5, "Ortodoncia Inicial",   450000.0),
        Procedimiento(6, "Blanqueamiento Dental",250000.0),
        Procedimiento(7, "Endodoncia",           320000.0),
        Procedimiento(8, "Implante Dental",     1500000.0),
    )

    private val moneyFmt: NumberFormat by lazy {
        // Ajusta a tu región si quieres (ej. Locale("es", "CO"))
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcedimientosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar (volver)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.title = "Procedimientos"

        // Adapter + lista
        adapter = ProcedimientosAdapter(
            currencyFormatter = { moneyFmt.format(it) },
            onDelete = { proc ->
                val idx = procedimientos.indexOfFirst { it.id == proc.id }
                if (idx >= 0) {
                    procedimientos.removeAt(idx)
                    adapter.submitList(procedimientos.toList())
                }
            }
        )
        binding.rvProcedimientos.layoutManager = LinearLayoutManager(this)
        binding.rvProcedimientos.adapter = adapter
        adapter.submitList(procedimientos.toList())

        // Filtros simples del costo (opcional)
        binding.etCosto.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 2))

        // Botón Guardar
        binding.btnGuardar.setOnClickListener {
            val nombre = binding.etProcedimiento.text?.toString()?.trim().orEmpty()
            val costoStr = binding.etCosto.text?.toString()?.trim().orEmpty()

            var ok = true
            binding.tilProcedimiento.error = if (nombre.isEmpty()) "Requerido" else null
            binding.tilCosto.error = if (costoStr.isEmpty()) "Requerido" else null
            ok = nombre.isNotEmpty() && costoStr.isNotEmpty()

            val costo = parseCosto(costoStr)
            if (ok && costo == null) {
                binding.tilCosto.error = "Formato inválido"
                ok = false
            }

            if (ok) {
                val nextId = (procedimientos.maxOfOrNull { it.id } ?: 0) + 1
                procedimientos.add(Procedimiento(nextId, nombre, costo!!))
                adapter.submitList(procedimientos.toList())

                binding.etProcedimiento.text?.clear()
                binding.etCosto.text?.clear()
                hideKeyboard()
            }
        }

        // (opcional) botón “limpiar” si lo agregas
        binding.btnLimpiar?.setOnClickListener {
            binding.etProcedimiento.text?.clear()
            binding.etCosto.text?.clear()
            binding.tilProcedimiento.error = null
            binding.tilCosto.error = null
        }
    }

    private fun parseCosto(raw: String): Double? {
        // Acepta "50.000,00" o "50000.00" o "$ 50,000.00"
        val s = raw
            .replace("$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
        return s.toDoubleOrNull()
    }

    private fun hideKeyboard() {
        currentFocus?.let { v ->
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
        }
    }
}

/** Restringe a N enteros y M decimales. */
class DecimalDigitsInputFilter(private val digitsBeforeZero: Int, private val digitsAfterZero: Int) :
    InputFilter {
    private val regex = Regex("^[0-9]{0,$digitsBeforeZero}+(\\.[0-9]{0,$digitsAfterZero})?$")
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: android.text.Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val out = dest?.replaceRange(dstart, dend, source?.subSequence(start, end) ?: "")
        return if (out != null && out.matches(regex)) null else ""
    }
}
