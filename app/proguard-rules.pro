# Keep data classes for JSON parsing
-keepclassmembers class com.deepseek.balancewidget.BalanceData { *; }

# Keep API key in SharedPreferences (don't obfuscate)
-keep class * extends androidx.security.crypto.EncryptedSharedPreferences { *; }
