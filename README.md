# 🏥 村医AI - 离线大模型问诊Android应用

基于 **llama.cpp + GGUF** 的本地离线医疗问诊 Android 应用，无需网络即可运行大语言模型推理。

## ✨ 特性

- 🔒 **完全离线** — 模型下载后无需网络，数据不上传
- 🧠 **本地推理** — 基于 llama.cpp JNI 桥接，纯 CPU 运行
- 🗣️ **语音播报** — TTS 自动朗读 AI 回复，可一键开关
- 📱 **多模型选择**
-     **轻量小模型** — 支持 Qwen2.5-0.5B 等小型量化模型
-     **医疗大模型** — 支持 medgemma-1.5-4b-it医疗大模型
- 💬 **流式输出** — 逐 token 生成，实时显示回复内容

## 🏗️ 技术架构

```
┌─────────────────────────────────┐
│         Android UI Layer        │
│  MainActivity / MainViewModel   │
│        ChatAdapter / TTS        │
├─────────────────────────────────┤
│        Kotlin Bridge Layer      │
│       LlamaEngine / Callback    │
├─────────────────────────────────┤
│          JNI Native Layer       │
│  llama_jni.cpp → llama.cpp     │
│     (CMake + NDK cross-compile) │
└─────────────────────────────────┘
```

## 🔧 编译环境

- Android SDK 35 (Build Tools 35.0.0)
- NDK 27.x
- CMake 3.22+
- Java 21
- Gradle 8.5

## 📦 构建步骤

```bash
# 1. 克隆仓库（含 submodule）
git clone --recurse-submodules https://github.com/tongbao2/cunyi-doctor-android.git

# 2. 编译 Debug APK
./gradlew assembleDebug

# 3. APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 📲 使用方法

1. 安装 APK 到 Android 设备
2. 首次打开点击「📥 下载模型」按钮
3. 等待模型下载完成（约 500MB）
4. 开始问诊对话

## 🤖 支持的模型

| 模型 | 大小 | 说明 |
|------|------|------|
| Qwen2.5-0.5B-Instruct-Q4_K_M | ~500MB | 默认，速度快 |
| medgemma-1.5-4b-it医疗大模型   | ~2.5GB | 医疗大模型,效果更好 |

修改 `LlamaEngine.kt` 中的 `MODEL_URL` 和 `MODEL_FILE` 即可切换模型。

## 🔴 注意事项

- 纯 CPU 推理，大模型加载和生成速度较慢属正常现象
- 首次加载模型需 30秒-2分钟，请耐心等待


## ⚠️ 免责声明

AI 建议仅供参考，不能替代医生诊断。如有不适，请及时就医。

## 📄 License

MIT
