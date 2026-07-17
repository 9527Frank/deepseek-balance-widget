package com.deepseek.balancewidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives manual refresh requests from widget taps.
 * Triggers an immediate widget update.
 */
class RefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                BalanceWidgetProvider.updateAppWidget(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId
                )
            } else {
                // Refresh all widgets if no specific ID
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    android.content.ComponentName(context, BalanceWidgetProvider::class.java)
                )
                for (id in ids) {
                    BalanceWidgetProvider.updateAppWidget(context, manager, id)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balancewidget.ACTION_REFRESH"
    }
}
