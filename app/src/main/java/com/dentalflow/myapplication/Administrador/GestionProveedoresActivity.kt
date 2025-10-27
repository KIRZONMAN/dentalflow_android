package com.dentalflow.myapplication.Administrador

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dentalflow.myapplication.databinding.ActivityGestionProveedoresBinding

class GestionProveedoresActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGestionProveedoresBinding
    private lateinit var adapter: ProveedoresAdapter   // ← ahora lateinit

    // Lista “maqueta”
    private val proveedores = mutableListOf(
        Proveedor("123",   "Suministros Médicos S.A.", "987654321", "contacto@sumedicos.com"),
        Proveedor("432",   "DentalCare",               "444555666", "contacto@dentalcare.com"),
        Proveedor("665",   "KetoCorp",                 "896555666", "contacto@ketocorp.com"),
        Proveedor("69543", "CocaCorp S.A",             "6537843",   "general@cocacorp.com"),
        Proveedor("777",   "KZCORP",                   "3143561509","basstrex96@gmail.com")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestionProveedoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // AppBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Gestión de Proveedores"

        // Adapter (ya podemos referenciar 'adapter' dentro del lambda)
        adapter = ProveedoresAdapter(onDelete = { prov ->
            val idx = proveedores.indexOfFirst { it.nit == prov.nit }
            if (idx >= 0) {
                proveedores.removeAt(idx)
                adapter.submitList(proveedores.toList())
            }
        })

        // RecyclerView
        binding.rvProveedores.layoutManager = LinearLayoutManager(this)
        binding.rvProveedores.adapter = adapter
        adapter.submitList(proveedores.toList())

        // Botones del formulario
        binding.btnLimpiar.setOnClickListener {
            binding.etNit.text?.clear()
            binding.etNombre.text?.clear()
            binding.etTelefono.text?.clear()
            binding.etCorreo.text?.clear()
        }

        binding.btnGuardar.setOnClickListener {
            val nit = binding.etNit.text?.toString()?.trim().orEmpty()
            val nombre = binding.etNombre.text?.toString()?.trim().orEmpty()
            val tel = binding.etTelefono.text?.toString()?.trim().orEmpty()
            val correo = binding.etCorreo.text?.toString()?.trim().orEmpty()

            binding.tilNit.error      = if (nit.isEmpty()) "Requerido" else null
            binding.tilNombre.error   = if (nombre.isEmpty()) "Requerido" else null
            binding.tilTelefono.error = if (tel.isEmpty()) "Requerido" else null
            binding.tilCorreo.error   = if (correo.isEmpty()) "Requerido" else null

            val ok = nit.isNotEmpty() && nombre.isNotEmpty() && tel.isNotEmpty() && correo.isNotEmpty()
            if (ok) {
                proveedores.removeAll { it.nit == nit }
                proveedores.add(0, Proveedor(nit, nombre, tel, correo))
                adapter.submitList(proveedores.toList())
                hideKeyboard()
            }
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let { v ->
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
        }
    }
}
