# Keep data class used with JSONObject (reflective field access)
-keepclassmembers class com.deepseek.balancewidget.BalanceData { *; }

# Keep Kotlin data class copy/component methods
-keepclassmembers class com.deepseek.balancewidget.BalanceData {
    *** copy(...);
    *** component*();
}

# Keep ViewBinding classes
-keep class com.deepseek.balancewidget.databinding.** { *; }

# EncryptedSharedPreferences (Tink) 引用了 javax.annotation 但实际不需要
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
