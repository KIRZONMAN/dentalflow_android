package com.dentalflow.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dentalflow.myapplication.Administrador.DashboardAdministrador
import com.dentalflow.myapplication.Asistente.DashboardAsistente
import com.dentalflow.myapplication.Laboratorista.DashboardLaboratorista
import com.dentalflow.myapplication.Odontologo.DashboardOdontologo
import com.dentalflow.myapplication.data.local.SessionManager
import com.dentalflow.myapplication.data.remote.Api
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl // ⬅️ IMPORT CLAVE
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import com.dentalflow.myapplication.data.remote.Http

class MainActivity : AppCompatActivity() {

    private lateinit var tilUsuario: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsuario: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnIngresar: MaterialButton

    private val client by lazy { Http.client }
    private val gson by lazy { Gson() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tilUsuario = findViewById(R.id.tilUsuario)
        tilPassword = findViewById(R.id.tilPassword)
        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnIngresar = findViewById(R.id.btnIngresar)

        btnIngresar.setOnClickListener { doLogin() }

        if (Build.VERSION.SDK_INT >= 33 &&
            !com.dentalflow.myapplication.util.NotificationHelper.canPost(this)
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Notificaciones desactivadas (puedes habilitarlas en Ajustes)", Toast.LENGTH_LONG).show()
        }
    }

    private fun doLogin() {
        tilUsuario.error = null
        tilPassword.error = null

        val email = etUsuario.text?.toString()?.trim().orEmpty()
        val pass  = etPassword.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (email.isEmpty()) { tilUsuario.error = "Ingresa tu correo"; hasError = true }
        if (pass.isEmpty())  { tilPassword.error = "Ingresa tu contraseña"; hasError = true }
        if (hasError) return

        btnIngresar.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1) Buscar usuario por correo en el backend (demo auth)
                val user = fetchUserByEmail(email)

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Usuario no encontrado", Toast.LENGTH_LONG).show()
                        btnIngresar.isEnabled = true
                    }
                    return@launch
                }

                // 2) Validaciones básicas
                val estado = (user["estado"]?.asString ?: "").lowercase(Locale.ROOT)
                if (estado != "activo") {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Cuenta no activa ($estado)", Toast.LENGTH_LONG).show()
                        btnIngresar.isEnabled = true
                    }
                    return@launch
                }

                val rol = user["rol"]?.asString?.trim().orEmpty()
                if (rol.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Rol no asignado", Toast.LENGTH_LONG).show()
                        btnIngresar.isEnabled = true
                    }
                    return@launch
                }

                // Nombre para mostrar
                val nombreCompacto = user["nombre"]?.asString
                val nombres = user["nombres"]?.asString
                val apellidos = user["apellidos"]?.asString
                val displayName = when {
                    !nombreCompacto.isNullOrBlank() -> nombreCompacto
                    else -> listOfNotNull(nombres, apellidos)
                        .filter { it.isNotBlank() }             // ⬅️ cambio aquí
                        .joinToString(" ")
                }

                // (Demo) guarda en sesión local
                withContext(Dispatchers.Main) {
                    SessionManager(this@MainActivity).apply {
                        saveDisplayName(displayName)
                        saveRole(rol)
                    }
                }

                // 3) Redirección por rol
                withContext(Dispatchers.Main) {
                    Log.d("LoginDebug", "login ok: correo=$email, rol=$rol, nombre=$displayName")

                    when (rol) {
                        "Odontologo", "Odontólogo" -> {
                            startActivity(
                                Intent(this@MainActivity, DashboardOdontologo::class.java)
                                    .putExtra("DISPLAY_NAME", displayName)
                            )
                            finish()
                        }
                        "Asistente" -> {
                            startActivity(
                                Intent(this@MainActivity, DashboardAsistente::class.java)
                                    .putExtra("DISPLAY_NAME", displayName)
                            )
                            finish()
                        }
                        "Administrador" -> {
                            startActivity(
                                Intent(this@MainActivity, DashboardAdministrador::class.java)
                                    .putExtra("DISPLAY_NAME", displayName)
                            )
                            finish()
                        }
                        "Laboratorista" -> {
                            startActivity(
                                Intent(this@MainActivity, DashboardLaboratorista::class.java)
                                    .putExtra("DISPLAY_NAME", displayName)
                            )
                            finish()
                        }
                        else -> {
                            Toast.makeText(this@MainActivity, "Rol no reconocido: $rol", Toast.LENGTH_LONG).show()
                            btnIngresar.isEnabled = true
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error de inicio de sesión: ${e.message}", Toast.LENGTH_LONG).show()
                    btnIngresar.isEnabled = true
                }
            }
        }
    }

    /**
     * Busca en GET /usuarios?search=<email> y toma el primer match cuyo 'correo' coincida exactamente (case-insensitive).
     * Devuelve el objeto JSON del usuario (o null si no).
     */
    private fun fetchUserByEmail(email: String): JsonObject? {
        val url = "https://lucid-youthfulness-production.up.railway.app/api/usuarios"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("search", email)
            .build()

        val req = Request.Builder()
            .url(url)
            // NO hace falta header aquí; lo pone Http.apiKeyInterceptor
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val bodyStr = resp.body?.string().orEmpty()
            Log.d("LoginDebug", "GET /usuarios resp=$bodyStr")

            val json = JsonParser.parseString(bodyStr).asJsonObject
            val ok = json["ok"]?.asBoolean ?: false
            if (!ok) return null

            val arr = json["data"]?.asJsonArray ?: return null
            val found = arr.firstOrNull { el ->
                val obj = el.asJsonObject
                val c = obj["correo"]?.asString ?: return@firstOrNull false
                c.equals(email, ignoreCase = true)
            } ?: return null

            return found.asJsonObject
        }
    }
}
