package com.dentalflow.myapplication

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MongoConnection {
    private const val TAG = "MongoConnection"

    /**
     * Stub tras migración a REST. No usa Realm ni App Services.
     * Se deja para compatibilidad con llamadas antiguas.
     */
    fun checkConnection() {
        Log.d(TAG, "REST backend activo. Realm/App Services deshabilitado.")
    }
}
