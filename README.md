# DeepSeek 余额桌面组件

🪄 **在手机桌面实时显示 DeepSeek API 账户余额**

> 适用于 **HarmonyOS 4.x / Android 8.0+** 设备（Honor 30 · HarmonyOS 4.2.0.121 已验证）

---

## 效果预览

桌面组件会显示：
- **总余额**（大数字，一目了然）
- **余额状态**（正常 / 余额偏低 / 余额不足）
- **余额明细**（赠送金额 / 充值金额）
- **自动更新**（每 30 分钟刷新一次）
- **点击刷新**（点一下组件立刻查询）
- **余额过低时变色警告**

---

## 快速开始

### 方法一：直接下载 APK（推荐）

1. 在 [Releases](https://github.com/your-username/deepseek-balance-widget/releases) 页面下载最新的 APK
2. 在 Honor 30 上安装 APK
3. 长按桌面空白处 → **服务卡片/组件** → 找到 **DeepSeek 余额**
4. 添加到桌面 → 输入你的 DeepSeek API Key
5. ✅ 完成！

> **如何获取 DeepSeek API Key？**
> 1. 访问 [platform.deepseek.com](https://platform.deepseek.com/)
> 2. 登录 → API Keys → 创建新的 Key

### 方法二：自行编译

需要 **Android Studio**（免费，约 2GB）。

#### 环境要求
| 工具 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1.1) 或更新 |
| JDK | 17 或更新 |
| Android SDK | API 34 (Android 14) |

#### 编译步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-username/deepseek-balance-widget.git
cd deepseek-balance-widget

# 2. 用 Android Studio 打开

# 3. 等待 Gradle 同步完成

# 4. Build → Build APK(s)
#    生成位置: app/build/outputs/apk/release/app-release.apk

# 5. 传送到手机安装
adb install app/build/outputs/apk/release/app-release.apk
```

#### 命令行编译（需要已安装 Android SDK）

```bash
# 设置 Android SDK 路径
export ANDROID_HOME=/path/to/android-sdk

# 下载 Gradle Wrapper（如果还没有）
gradle wrapper --gradle-version 8.5

# 编译 Release APK
./gradlew assembleRelease

# APK 位置: app/build/outputs/apk/release/app-release.apk
```

---

## 使用方法

### 首次设置
1. 添加组件到桌面后自动打开设置界面
2. 输入你的 DeepSeek API Key
3. 点击 **保存** → 自动验证并显示余额
4. 返回桌面，组件已显示余额

### 组件交互
| 操作 | 效果 |
|------|------|
| **点击组件** | 手动刷新余额 |
| **点击"详情"** | 打开详细余额页面 |
| **长按 → 编辑** | 修改 API Key |

### 详细页面功能
- 查看完整的余额明细（赠送 / 充值分开显示）
- 点击余额数字可**复制到剪贴板**
- **刷新**按钮手动查询最新余额
- **清除**按钮删除 API Key

---

## 组件状态说明

| 图标 | 状态 | 说明 |
|------|------|------|
| ✅ 绿色 | 正常 | 余额充足 |
| ⚠️ 黄色 | 余额偏低 | 余额低于 ¥10 |
| ❌ 红色 | 余额不足 / 错误 | API Key 无效或网络问题 |
| ⚙️ 蓝色 | 未设置 | 需要配置 API Key |

---

## 技术架构

```
deepseek-balance-widget/
├── app/
│   └── src/main/java/com/deepseek/balancewidget/
│       ├── BalanceWidgetProvider.kt    # 桌面组件逻辑
│       ├── WidgetConfigActivity.kt     # 配置界面
│       ├── DeepSeekApi.kt             # API 调用
│       ├── BalanceData.kt             # 数据模型
│       ├── BalanceUpdateWorker.kt     # 定时更新
│       ├── RefreshReceiver.kt         # 点击刷新
│       └── KeyStore.kt               # 安全存储 API Key
├── app/src/main/res/
│   ├── layout/                        # UI 布局
│   ├── drawable/                      # 图标资源
│   └── xml/                           # 组件配置
└── README.md
```

### 安全说明
- **API Key 使用 EncryptedSharedPreferences 加密存储**（Android Keystore 加密）
- 数据仅在设备本地处理，**不经过任何第三方服务器**
- 直接调用 DeepSeek 官方 API，无中间人
- 无任何不必要的权限

### 更新频率
- 组件自动更新：每 **30 分钟**
- 后台 Worker 补充更新：每 **30 分钟**（需要网络）
- 手动点击组件：即时刷新
- 省电策略：采用 WorkManager，自动适配各家厂商的省电策略

---

## 常见问题

**Q: 添加组件时提示"没有此应用"或找不到组件？**
A: HarmonyOS 4.2 上添加方式可能不同：
- **方法一**：双指捏合桌面 → **窗口小工具** → 在列表中找到 **DeepSeek 余额** → 拖动到桌面
- **方法二**：长按桌面空白处 → **服务卡片** → 滑动到底部 → **其他** → 找到 **DeepSeek 余额**
- 如果都找不到，重启手机后再试

**Q: 组件不自动刷新？**
A: HarmonyOS 的省电策略可能会限制后台刷新，请按以下步骤设置：

**Honor 30 / HarmonyOS 4.2 专属设置：**
1. 设置 → 应用 → 应用启动管理 → 找到 **DeepSeek 余额** → 关闭"自动管理" → 将 **允许自启动**、**允许关联启动**、**允许后台活动** 全部打开 ✅
2. 设置 → 应用 → 权限管理 → 右上角 ⚙️ → **特殊访问权限** → **电池优化** → 选择 **DeepSeek 余额** → 设为 **不允许优化**
3. 设置 → 电池 → 更多电池设置 → 关闭 **"智能充电模式"** 旁边的休眠清理（如有）
4. 长按桌面组件 → **编辑** → 确认 API Key 未过期

> 如果以上都设置了还是不行，手动点击组件可以即时刷新。

**Q: 余额显示"— —"？**
A: 说明查询失败，可能原因：
- 网络不可用 → 检查网络连接
- API Key 无效 → 重新设置
- API 服务器问题 → 稍后再试

**Q: 是否支持其他货币？**
A: 自动适配 API 返回的货币类型（默认 CNY）。

---

## License

MIT
