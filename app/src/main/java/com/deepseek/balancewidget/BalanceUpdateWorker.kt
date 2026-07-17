package com.deepseek.balancewidget

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager-based periodic worker that refreshes all widget instances.
 *
 * Falls back to Android's minimum 15-minute interval. The widget's built-in
 * [android.appwidget.AppWidgetProviderInfo.updatePeriodMillis] provides a
 * 30-minute baseline; this worker supplements it for more reliable updates
 * across different OEM power-saving policies.
 */
class BalanceUpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(applicationContext)
        val componentName = android.content.ComponentName(
            applicationContext,
            BalanceWidgetProvider::class.java
        )
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (widgetIds.isEmpty()) {
            // No widgets left — clean up schedule
            return Result.success()
        }

        // Refresh all widgets
        for (id in widgetIds) {
            BalanceWidgetProvider.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "deepseek_balance_periodic_update"

        /** Schedule updates every 30 minutes. */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BalanceUpdateWorker>(
                30, TimeUnit.MINUTES
            ).apply {
                setConstraints(constraints)
                setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
            }.build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancel periodic updates. */
        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
