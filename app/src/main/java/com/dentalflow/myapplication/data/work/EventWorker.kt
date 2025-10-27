package com.dentalflow.myapplication.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dentalflow.myapplication.util.NotificationHelper

class EventWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val action  = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val title   = inputData.getString(KEY_TITLE)  ?: "DentalFlow"
        val message = inputData.getString(KEY_MESSAGE) ?: action

        // Aquí podrías además enviar un “audit log” al backend si lo deseas.
        NotificationHelper.show(applicationContext, title, message)
        return Result.success()
    }

    companion object {
        const val KEY_ACTION  = "action"
        const val KEY_TITLE   = "title"
        const val KEY_MESSAGE = "message"
    }
}
