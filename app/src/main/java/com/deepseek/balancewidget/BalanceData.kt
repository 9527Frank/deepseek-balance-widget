package com.deepseek.balancewidget

import org.json.JSONObject

/**
 * Represents the DeepSeek account balance fetched from the API.
 */
data class BalanceData(
    val isAvailable: Boolean,
    val currency: String,
    val totalBalance: String,
    val grantedBalance: String,
    val toppedUpBalance: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalBalanceDouble: Double get() = totalBalance.toDoubleOrNull() ?: 0.0
    val grantedBalanceDouble: Double get() = grantedBalance.toDoubleOrNull() ?: 0.0
    val toppedUpBalanceDouble: Double get() = toppedUpBalance.toDoubleOrNull() ?: 0.0

    companion object {
        /** Parse from DeepSeek API response JSON. */
        fun fromJson(json: JSONObject): BalanceData {
            val balanceInfos = json.optJSONArray("balance_infos")
            val info = balanceInfos?.optJSONObject(0) ?: JSONObject()

            return BalanceData(
                isAvailable = json.optBoolean("is_available", false),
                currency = info.optString("currency", "CNY"),
                totalBalance = info.optString("total_balance", "0.00"),
                grantedBalance = info.optString("granted_balance", "0.00"),
                toppedUpBalance = info.optString("topped_up_balance", "0.00")
            )
        }

        /** Create an error-state placeholder. */
        fun error(): BalanceData = BalanceData(
            isAvailable = false,
            currency = "CNY",
            totalBalance = "—",
            grantedBalance = "—",
            toppedUpBalance = "—"
        )
    }
}
