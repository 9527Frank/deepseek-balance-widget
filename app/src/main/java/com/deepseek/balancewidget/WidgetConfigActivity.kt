package com.deepseek.balancewidget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.deepseek.balancewidget.databinding.ConfigActivityBinding

/**
 * Widget configuration activity.
 *
 * First launch: prompts the user to enter their DeepSeek API key.
 * Subsequent launches: shows the current balance with a refresh button
 * and the option to update the API key.
 */
class WidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ConfigActivityBinding

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConfigActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setupUI()
        loadExistingKey()
    }

    private fun setupUI() {
        // Save / Apply button
        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            if (apiKey.isBlank()) {
                Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate the key by testing it
            validateAndSaveKey(apiKey)
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            refreshBalance()
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            KeyStore.clearApiKey(this)
            binding.etApiKey.setText("")
            binding.balanceCard.isVisible = false
            binding.btnRefresh.isVisible = false
            binding.btnClear.isVisible = false
            binding.etApiKey.requestFocus()
            Toast.makeText(this, "API Key 已清除", Toast.LENGTH_SHORT).show()
            updateWidgets()
        }

        // Balance card click → copy balance to clipboard
        binding.balanceCard.setOnClickListener {
            val balance = binding.tvBalance.text.toString()
            if (balance.isNotBlank() && balance != "—") {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("balance", balance))
                Toast.makeText(this, "已复制余额", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadExistingKey() {
        val key = KeyStore.getApiKey(this)
        if (key.isNotBlank()) {
            binding.etApiKey.setText(key)
            binding.balanceCard.isVisible = true
            binding.btnRefresh.isVisible = true
            binding.btnClear.isVisible = true
            refreshBalance()
        } else {
            binding.balanceCard.isVisible = false
            binding.btnRefresh.isVisible = false
            binding.btnClear.isVisible = false
        }
    }

    private fun validateAndSaveKey(apiKey: String) {
        binding.btnSave.isEnabled = false
        binding.tvStatus.text = "验证 API Key 中…"
        binding.progressBar.isVisible = true

        Thread {
            val result = DeepSeekApi.fetchBalance(apiKey)

            runOnUiThread {
                binding.progressBar.isVisible = false
                binding.btnSave.isEnabled = true

                when (result) {
                    is DeepSeekApi.ApiResult.Success -> {
                        KeyStore.saveApiKey(this, apiKey)
                        binding.tvStatus.text = "✓ API Key 验证成功"
                        binding.tvStatus.setTextColor(
                            resources.getColor(android.R.color.holo_green_dark, theme)
                        )
                        binding.balanceCard.isVisible = true
                        binding.btnRefresh.isVisible = true
                        binding.btnClear.isVisible = true
                        showBalance(result.data)
                        updateWidgets()

                        Toast.makeText(this, "✓ 保存成功", Toast.LENGTH_SHORT).show()

                        // If launched for widget config, finish on success
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            finish()
                        }
                    }

                    is DeepSeekApi.ApiResult.Error -> {
                        binding.tvStatus.text = "✗ ${result.message}"
                        binding.tvStatus.setTextColor(
                            resources.getColor(android.R.color.holo_red_light, theme)
                        )
                        Toast.makeText(this, "验证失败：${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.apply {
            isDaemon = true
            name = "ds-key-validate"
            start()
        }
    }

    private fun refreshBalance() {
        val apiKey = KeyStore.getApiKey(this)
        if (apiKey.isBlank()) return

        binding.tvStatus.text = "查询余额中…"
        binding.progressBar.isVisible = true
        binding.btnRefresh.isEnabled = false

        Thread {
            val result = DeepSeekApi.fetchBalance(apiKey)

            runOnUiThread {
                binding.progressBar.isVisible = false
                binding.btnRefresh.isEnabled = true

                when (result) {
                    is DeepSeekApi.ApiResult.Success -> {
                        binding.tvStatus.text = "✓ 更新成功"
                        binding.tvStatus.setTextColor(
                            resources.getColor(android.R.color.holo_green_dark, theme)
                        )
                        showBalance(result.data)
                    }

                    is DeepSeekApi.ApiResult.Error -> {
                        binding.tvStatus.text = "✗ ${result.message}"
                        binding.tvStatus.setTextColor(
                            resources.getColor(android.R.color.holo_red_light, theme)
                        )
                    }
                }
            }
        }.apply {
            isDaemon = true
            name = "ds-balance-refresh"
            start()
        }
    }

    private fun showBalance(data: BalanceData) {
        binding.tvBalance.text = "¥${data.totalBalance}"
        binding.tvBalanceDetails.text = buildString {
            append("可用状态：${if (data.isAvailable) "可用" else "不可用"}")
            append("\n赠送余额：¥${data.grantedBalance}")
            append("\n充值余额：¥${data.toppedUpBalance}")
        }
    }

    /** Force all widgets to refresh immediately. */
    private fun updateWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val componentName = android.content.ComponentName(this, BalanceWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(componentName)
        for (id in ids) {
            BalanceWidgetProvider.updateAppWidget(this, manager, id)
        }
    }
}
