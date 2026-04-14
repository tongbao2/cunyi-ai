# 村医 AI - 离线医学助手

基于 Google Gemma 4 E2B 模型开发的离线医学助手，专为偏远地区农村老年人设计。


## 🏥 功能特点

### 核心功能

| 功能 | 描述 |
|------|------|
| **🆘 一键求救** | 按下SOS按钮，自动发送求救短信给紧急联系人（无需网络） |
| **💬 语音问诊** | 对着手机说话，AI分析症状并给出建议 |
| **📷 拍照识药** | 拍摄药盒，识别药品名称，查看用药说明 |
| **📊 慢病管理** | 记录血压、血糖、心率，趋势分析，异常预警 |
| **💊 药物冲突检测** | 自动检查药物相互作用，提前预警 |

### 安全特性

- **医学阈值告警**：危险判断使用硬编码医学阈值，不依赖AI
  - 血压 > 180/120 mmHg → 🔴 红色警报
  - 血糖 > 16.7 或 < 2.8 mmol/L → 🔴 红色警报
  - 心率 > 150 或 < 40 次/分 → 🔴 红色警报

- **内置药品数据库**：15种农村常用药品
- **药物相互作用检测**：10条药物相互作用规则

### 老年人友好设计

- ✅ 最小字号 18sp，标题 28sp
- ✅ 按钮最小 72dp 高，手指粗也按得到
- ✅ 高对比度配色（深绿底白字）
- ✅ 全程语音播报
- ✅ 无注册、无登录、无广告
- ✅ **100% 离线运行**

## 📱 技术架构

```
┌──────────────────────────────────────┐
│           村医AI App                │
├──────────────┬───────────────────────┤
│   UI 层     │  Jetpack Compose      │
├──────────────┼───────────────────────┤
│  业务层     │  RulesEngine (规则引擎) │
│             │  SOSManager (求救)     │
│             │  HealthRecordManager  │
├──────────────┴───────────────────────┤
│  数据层     │  MedicineDatabase      │
│             │  SharedPreferences    │
├──────────────┬───────────────────────┤
│  模型层     │  Gemma 4 E2B (Q4_K_M)  │
│             │  ~2.4GB, 4GB内存可用   │
└──────────────┴───────────────────────┘
```

### 技术栈

- **模型**: Google Gemma 4 E2B (INT4量化，2.4GB)
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Clean Architecture
- **存储**: SharedPreferences
- **网络**: 仅用于下载模型（按需）

## 🚀 快速开始

### 下载 APK

1. 访问 [GitHub Actions](https://github.com/tongbao2/cunyi-ai/actions)
2. 选择最新的 workflow run
3. 在 **Artifacts** 部分下载 `cunyi-ai-debug.apk.zip`
4. 解压后安装到 Android 手机

### 下载模型（首次使用）

1. 打开应用，进入「设置」
2. 点击「下载模型」
3. 从 HuggingFace 下载 Gemma 4 E2B 模型：
   - 地址：https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf
4. 返回应用，点击「选择模型文件」
5. 等待加载完成，即可离线使用

### 系统要求

- Android 8.0+ (API 26+)
- 4GB+ 运行内存
- 约 3GB 存储空间（模型）

## 💻 开发指南

### 环境要求

- Android Studio Hedgehog (2023.1.1)+
- Java 17 JDK
- Android SDK API 34
- Gradle 8.5

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/tongbao2/cunyi-ai.git
cd cunyi-ai

# 进入 Android 目录
cd android/CunYiAI

# 使用 Android Studio 打开项目
# 或使用 Gradle 构建
./gradlew assembleDebug
```

### 项目结构

```
android/CunYiAI/src/main/java/com/cunyi/ai/
├── MainActivity.kt           # 主界面
├── CunYiAIApplication.kt     # Application类
├── data/                     # 数据模型
│   ├── Medicine.kt          # 药品数据类
│   ├── MedicineDatabase.kt   # 药品数据库
│   ├── HealthRecord.kt       # 健康记录
│   ├── EmergencyContact.kt   # 紧急联系人
│   └── SOSMessage.kt         # 求救消息
├── manager/                  # 业务逻辑
│   ├── RulesEngine.kt        # 规则引擎（安全兜底）
│   ├── HealthRecordManager.kt # 健康记录管理
│   └── SOSManager.kt         # SOS求救管理
└── ui/                       # UI层
    ├── theme/                # 主题和样式
    ├── components/            # 通用组件
    └── screens/               # 页面
        ├── HealthManagementScreen.kt  # 慢病管理
        ├── SOSScreen.kt              # 紧急求救
        └── MedicineRecognitionScreen.kt # 拍照识药
```

## 🔧 自动构建

本项目配置了 GitHub Actions，每次 push 到 main 分支会自动构建 APK。

构建完成后，在 **Actions** 页面下载 artifact。

## ⚠️ 免责声明

AI 建议仅供参考，不能替代医生诊断。如有不适，请及时就医。

## 📄 许可证

MIT License

## 🙏 致谢

- [Google Gemma](https://ai.google.dev/gemma) - 开源语言模型
- [Unsloth](https://unsloth.ai/) - 高效量化模型
- [MediaPipe](https://google.github.io/mediapipe/) - 端侧推理
