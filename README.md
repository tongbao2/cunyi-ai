# 村医 AI - 离线医学助手

基于 Meta Llama 模型开发的离线医学助手，专为偏远地区乡村医生设计。

![Cover image](assets/cover-image.png)

## 功能特点

- **100% 离线运行** - 不需要互联网连接即可使用
- **轻量级模型** - 使用 Meta 的 Llama 3.2 1B/3B 轻量模型
- **本地部署** - 所有数据存储在设备本地，保护隐私
- **医疗辅助** - 提供基本急救指导、诊断建议、药物信息查询

## 技术栈

- **Llama 模型**: Meta Llama 3.2 1B/3B
- **推理引擎**: Python Executor (`.pte` 格式)
- **Android**: Java/Kotlin, Android Studio
- **后端**: Python, Streamlit
- **部署平台**: Google Kaggle

## 快速开始

### 1. 下载 APK

首次运行会自动下载并接入模型地址：
https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf

### 2. 安装应用

1. 下载 APK 后安装到 Android 设备
2. 打开应用，进入设置
3. 点击"模型下载"，选择模型后开始下载
4. 下载完成后即可离线使用 AI 助手

## 开发指南

### 环境要求

- Python 3.10+
- Java 17 JDK
- Android SDK API 34
- Git

### 本地开发

```bash
# 克隆仓库
git clone https://github.com/你的用户名/cunyi-ai.git
cd cunyi-ai

# 进入 Android 目录
cd android/CunYiAI

# 使用 Android Studio 打开项目
```

### 模型转换

模型需要转换为 `.pte` 格式：

```bash
# 安装 llama.cpp
pip install llama.cpp

# 转换模型
python -m examples.models.llama.export_llama \
  --checkpoint /path/to/model.gguf \
  --params /path/to/params.json \
  --outfile model.pte
```

## 自动构建

本项目配置了 GitHub Actions，每次 push 到 main 分支会自动构建 APK。

### 部署步骤

1. 在 GitHub 上创建新仓库（如 `cunyi-ai`）
2. 将本地代码推送到 GitHub：

```bash
cd cunyi-ai
git init
git add .
git commit -m "Initial commit: 村医AI离线版"
git branch -M main
git remote add origin https://github.com/你的用户名/cunyi-ai.git
git push -u origin main
```

3. 推送后，GitHub Actions 会自动构建 APK
4. 构建完成后，在 Releases 页面下载 APK

APK 下载地址：见 GitHub Releases

## 许可证

MIT License

## 贡献者

- 项目维护者

## 致谢

- [Meta Llama](https://ai.meta.com/llama/) - 提供开源语言模型
- [llama.cpp](https://github.com/ggerganov/llama.cpp) - 高效推理引擎
- [Hugging Face](https://huggingface.co/) - 模型托管