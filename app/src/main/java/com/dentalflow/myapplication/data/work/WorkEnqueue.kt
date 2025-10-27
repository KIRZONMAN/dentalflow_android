package com.dentalflow.myapplication.data.work

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import android.content.Context

object WorkEnqueue {
    fun event(
        context: Context,
        action: String,
        title: String,
        message: String
    ) {
        val data: Data = workDataOf(
            EventWorker.KEY_ACTION to action,
            EventWorker.KEY_TITLE to title,
            EventWorker.KEY_MESSAGE to message
        )

        val req = OneTimeWorkRequestBuilder<EventWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(req)
    }
}
