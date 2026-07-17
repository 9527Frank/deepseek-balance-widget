package com.deepseek.balancewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home screen widget provider for DeepSeek balance monitoring.
 *
 * Displays the current DeepSeek account balance on the home screen
 * with auto-refresh every 30 minutes and manual refresh on tap.
 */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added — schedule periodic updates via WorkManager
        BalanceUpdateWorker.schedulePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed — cancel periodic updates
        BalanceUpdateWorker.cancelPeriodic(context)
    }

    companion object {

        /**
         * Update a single widget instance with the latest balance, or show
         * a setup prompt if no API key is configured yet.
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // ── Set up click to refresh ──────────────────────────────
            val refreshIntent = Intent(context, RefreshReceiver::class.java).apply {
                action = RefreshReceiver.ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, refreshPending)

            // ── Check for configured API key ──────────────────────────
            if (!KeyStore.hasApiKey(context)) {
                views.setTextViewText(R.id.widget_balance, "— —")
                views.setTextViewText(R.id.widget_status, "点此设置 API Key")
                views.setTextViewText(R.id.widget_currency, "")
                views.setTextViewText(R.id.widget_sub_info, "首次使用需要配置")
                views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_setup)

                // Click on setup state opens config activity
                val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val configPending = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, configPending)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            // ── Show loading state ────────────────────────────────────
            views.setTextViewText(R.id.widget_balance, "…")
            views.setTextViewText(R.id.widget_status, "查询中")
            views.setTextViewText(R.id.widget_currency, "")
            views.setTextViewText(R.id.widget_sub_info, "")
            views.setImageViewResource(R.id.widget_status_icon, R.drawable.ic_sync)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // ── Fetch balance on a background thread ──────────────────
            val apiKey = KeyStore.getApiKey(context)
            Thread {
                val result = DeepSeekApi.fetchBalance(apiKey)

                appWidgetManager.partiallyUpdateAppWidget(appWidgetId,
                    buildRemoteViews(context, appWidgetId, result)
                )
            }.apply {
                // Allow the thread to run; doesn't hold UI
                isDaemon = true
                name = "ds-balance-fetch-$appWidgetId"
                start()
            }
        }

        /**
         * Build RemoteViews based on the API result.
         */
        private fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            result: DeepSeekApi.ApiResult<BalanceData>
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Refresh intent (always set)
            val refreshIntent = Intent(context, RefreshReceiver::class.java).apply {
                action = RefreshReceiver.ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, refreshPending)

            // Click opens config for details / API key management
            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val configPending = PendingIntent.getActivity(
                context,
                appWidgetId + 1000,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_details_btn, configPending)

            when (result) {
                is DeepSeekApi.ApiResult.Success -> {
                    val data = result.data
                    views.setTextViewText(R.id.widget_balance, formatBalance(data.totalBalance))
                    views.setTextViewText(R.id.widget_currency, data.currency)

                    // Status text
                    val statusText = if (data.isAvailable) {
                        if (data.totalBalanceDouble <= 0) "余额不足"
                        else if (data.totalBalanceDouble < 10) "余额偏低"
                        else "正常"
                    } else {
                        "账户不可用"
                    }
                    views.setTextViewText(R.id.widget_status, statusText)

                    // Status icon
                    val iconRes = when {
                        !data.isAvailable -> R.drawable.ic_error
                        data.totalBalanceDouble <= 0 -> R.drawable.ic_warning
                        data.totalBalanceDouble < 10 -> R.drawable.ic_warning
                        else -> R.drawable.ic_check
                    }
                    views.setImageViewResource(R.id.widget_status_icon, iconRes)

                    // Sub-info line: granted / topped-up breakdown
                    val subInfo = buildString {
                        if (data.grantedBalanceDouble > 0) {
                            append("赠送 ¥${data.grantedBalance}")
                        }
                        if (data.toppedUpBalanceDouble > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("充值 ¥${data.toppedUpBalance}")
                        }
                        if (isEmpty()) append("无余额明细")
                    }
                    views.setTextViewText(R.id.widget_sub_info, subInfo)
                }

                is DeepSeekApi.ApiResult.Error -> {
                    views.setTextViewText(R.id.widget_balance, "— —")
                    views.setTextViewText(R.id.widget_currency, "")
                    views.setTextViewText(R.id.widget_status, result.message)
                    views.setImageViewResource(
                        R.id.widget_status_icon,
                        if (result.isAuthError) R.drawable.ic_error else R.drawable.ic_warning
                    )
                    views.setTextViewText(R.id.widget_sub_info, "点击重试")
                }
            }

            return views
        }

        /** Format balance for display: strip trailing zeros, add ¥ prefix. */
        private fun formatBalance(balance: String): String {
            val num = balance.toDoubleOrNull() ?: return balance
            return if (num == num.toLong().toDouble()) {
                "¥${num.toLong()}"
            } else {
                "¥${String.format("%.2f", num)}"
            }
        }
    }
}
