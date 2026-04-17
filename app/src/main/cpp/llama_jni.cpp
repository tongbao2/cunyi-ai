#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdlib>
#include <thread>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

#define LOG_TAG "llama-jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)

using namespace std;

static JavaVM* g_jvm = nullptr;

struct LlamaState {
    llama_model*   model = nullptr;
    llama_context* ctx    = nullptr;
    atomic<bool>   running{false};
    thread*        inferenceThread = nullptr;
    mutex          mtx;
    string         modelPath;
    string         lastError;
};

static LlamaState g_state;

// ─── JNI helpers ──────────────────────────────────────────────────────────────
static JNIEnv* attachThread() {
    JNIEnv* env = nullptr;
    if (!g_jvm) return nullptr;
    if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
    g_jvm->AttachCurrentThread(&env, nullptr);
    return env;
}

static void deliverCallback(JNIEnv* env, jobject cb, const char* method, const char* text) {
    if (!cb || !env || !method) return;
    jclass cls = env->GetObjectClass(cb);
    if (!cls) return;
    jmethodID mid = env->GetMethodID(cls, method, "(Ljava/lang/String;)V");
    if (!mid) { env->DeleteLocalRef(cls); return; }
    jstring jarg = text ? env->NewStringUTF(text) : env->NewStringUTF("");
    if (jarg) {
        env->CallVoidMethod(cb, mid, jarg);
        env->DeleteLocalRef(jarg);
    }
    env->DeleteLocalRef(cls);
}

// ─── Generation thread ────────────────────────────────────────────────────────
static void runInference(const string& prompt, jobject callback) {
    JNIEnv* env = attachThread();
    if (!env) {
        LOGE("Cannot attach thread");
        g_state.running = false;
        g_jvm->DetachCurrentThread();
        return;
    }

    if (!g_state.model || !g_state.ctx) {
        deliverCallback(env, callback, "onError", "[ERROR] 模型未加载");
        env->DeleteGlobalRef(callback);
        g_jvm->DetachCurrentThread();
        g_state.running = false;
        return;
    }

    // 每次推理前清空 KV cache，避免上下文污染
    llama_kv_cache_clear(g_state.ctx);

    // ── 1. Tokenize ──────────────────────────────────────────────────────────
    const int MAX_TOKENS = 512;
    vector<llama_token> tokens(MAX_TOKENS);

    int n_tokens = llama_tokenize(
        g_state.model,
        prompt.c_str(),
        (int32_t)prompt.size(),
        tokens.data(),
        MAX_TOKENS,
        true,   // add_special (BOS)
        true    // parse_special
    );

    if (n_tokens < 0) {
        // 负值表示需要更大的 buffer
        int needed = -n_tokens;
        LOGW("Tokenize needs %d tokens, retrying", needed);
        tokens.resize(needed);
        n_tokens = llama_tokenize(
            g_state.model,
            prompt.c_str(),
            (int32_t)prompt.size(),
            tokens.data(),
            needed,
            true, true
        );
    }

    if (n_tokens <= 0) {
        LOGE("Tokenization failed: %d", n_tokens);
        deliverCallback(env, callback, "onError", "[ERROR] 分词失败");
        env->DeleteGlobalRef(callback);
        g_jvm->DetachCurrentThread();
        g_state.running = false;
        return;
    }
    tokens.resize(n_tokens);
    LOGI("Prompt tokenized: %d tokens", n_tokens);

    // ── 2. Decode prompt (不要 free llama_batch_get_one 的结果) ──────────────
    // llama_batch_get_one 返回的是栈上结构，不需要 free
    {
        llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens, 0, 0);
        int ret = llama_decode(g_state.ctx, batch);
        if (ret != 0) {
            LOGE("Prompt decode failed: %d", ret);
            deliverCallback(env, callback, "onError", "[ERROR] 提示词解码失败");
            env->DeleteGlobalRef(callback);
            g_jvm->DetachCurrentThread();
            g_state.running = false;
            return;
        }
    }

    // ── 3. Autoregressive generation ─────────────────────────────────────────
    string output;
    const int max_new = 256;  // 减少最大生成长度，降低崩溃风险
    llama_pos cur_pos = (llama_pos)n_tokens;  // 当前 KV cache 位置

    for (int i = 0; i < max_new && g_state.running; ++i) {
        // 获取最后一个 token 的 logits
        float* logits = llama_get_logits_ith(g_state.ctx, -1);
        if (!logits) {
            LOGE("logits null at step %d", i);
            break;
        }

        const int n_vocab = llama_n_vocab(g_state.model);
        vector<llama_token_data> cdata(n_vocab);
        for (int j = 0; j < n_vocab; ++j) {
            cdata[j].id    = (llama_token)j;
            cdata[j].logit = logits[j];
            cdata[j].p     = 0.0f;
        }
        llama_token_data_array cands{ cdata.data(), (size_t)n_vocab, false };

        llama_token new_tok = llama_sample_token_greedy(g_state.ctx, &cands);

        if (llama_token_is_eog(g_state.model, new_tok)) {
            LOGI("EOS at step %d", i);
            break;
        }

        // token → piece
        char buf[128] = {0};
        int n_char = llama_token_to_piece(
            g_state.model, new_tok,
            buf, (int32_t)(sizeof(buf) - 1),
            0, true
        );
        if (n_char > 0) {
            buf[n_char] = '\0';
            output += buf;
            deliverCallback(env, callback, "onToken", buf);
        }

        // decode 下一个 token，position = cur_pos
        llama_batch b2 = llama_batch_get_one(&new_tok, 1, cur_pos, 0);
        cur_pos++;
        int ret = llama_decode(g_state.ctx, b2);
        if (ret != 0) {
            LOGE("Decode failed at step %d: %d", i, ret);
            break;
        }
    }

    deliverCallback(env, callback, "onDone", output.c_str());
    LOGI("Inference done. output_len=%zu", output.size());

    env->DeleteGlobalRef(callback);
    g_jvm->DetachCurrentThread();
    g_state.running = false;
}

// ─── Native method implementations (plain names, no JNI mangling) ────────────
static jboolean nativeInit(JNIEnv* env, jobject, jstring modelPath) {
    lock_guard<mutex> lock(g_state.mtx);

    LOGI("nativeInit called");

    if (g_state.ctx)  { llama_free(g_state.ctx);  g_state.ctx  = nullptr; }
    if (g_state.model) { llama_free_model(g_state.model); g_state.model = nullptr; }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) {
        g_state.lastError = "无效的模型路径";
        LOGE("modelPath is null");
        return JNI_FALSE;
    }
    g_state.modelPath = path;
    env->ReleaseStringUTFChars(modelPath, path);

    LOGI("Loading model: %s", g_state.modelPath.c_str());

    // 检查文件是否存在
    FILE* f = fopen(g_state.modelPath.c_str(), "rb");
    if (!f) {
        g_state.lastError = "模型文件不存在或无法读取: " + g_state.modelPath;
        LOGE("Cannot open model file: %s", g_state.modelPath.c_str());
        return JNI_FALSE;
    }
    
    // 检查文件大小
    fseek(f, 0, SEEK_END);
    long fileSize = ftell(f);
    fclose(f);
    LOGI("Model file size: %ld bytes (%.1f MB)", fileSize, fileSize / 1024.0 / 1024.0);
    
    if (fileSize < 100000000) {  // < 100MB
        g_state.lastError = "模型文件太小，可能下载不完整";
        LOGE("Model file too small: %ld bytes", fileSize);
        return JNI_FALSE;
    }

    // 纯 CPU 模式，更稳定
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;
    mparams.use_mmap     = true;
    mparams.use_mlock    = false;

    LOGI("Calling llama_load_model_from_file...");
    
    // 使用 try-catch 捕获可能的崩溃
    try {
        g_state.model = llama_load_model_from_file(g_state.modelPath.c_str(), mparams);
    } catch (const std::exception& e) {
        g_state.lastError = string("模型加载异常: ") + e.what();
        LOGE("Exception during llama_load_model_from_file: %s", e.what());
        return JNI_FALSE;
    } catch (...) {
        g_state.lastError = "模型加载时发生未知异常";
        LOGE("Unknown exception during llama_load_model_from_file");
        return JNI_FALSE;
    }
    
    if (!g_state.model) {
        g_state.lastError = "模型加载失败：llama_load_model_from_file 返回空";
        LOGE("llama_load_model_from_file returned null");
        return JNI_FALSE;
    }

    LOGI("Model loaded OK, creating context...");

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = 512;
    cparams.n_threads       = 2;
    cparams.n_threads_batch = 2;

    LOGI("Calling llama_new_context_with_model...");
    
    try {
        g_state.ctx = llama_new_context_with_model(g_state.model, cparams);
    } catch (const std::exception& e) {
        g_state.lastError = string("创建上下文异常: ") + e.what();
        LOGE("Exception during llama_new_context_with_model: %s", e.what());
        llama_free_model(g_state.model);
        g_state.model = nullptr;
        return JNI_FALSE;
    } catch (...) {
        g_state.lastError = "创建上下文时发生未知异常";
        LOGE("Unknown exception during llama_new_context_with_model");
        llama_free_model(g_state.model);
        g_state.model = nullptr;
        return JNI_FALSE;
    }
    
    if (!g_state.ctx) {
        g_state.lastError = "无法创建推理上下文，设备内存可能不足";
        LOGE("llama_new_context_with_model returned null");
        llama_free_model(g_state.model);
        g_state.model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Init complete. ctx=%p model=%p", (void*)g_state.ctx, (void*)g_state.model);
    return JNI_TRUE;
}

static jboolean nativeIsModelLoaded(JNIEnv*, jobject) {
    return (g_state.model != nullptr && g_state.ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

static jstring nativeGetLastError(JNIEnv* env, jobject) {
    return env->NewStringUTF(g_state.lastError.c_str());
}

static void nativeGenerateAsync(JNIEnv* env, jobject, jstring prompt, jobject callback) {
    if (g_state.running) {
        LOGW("Already running, ignoring");
        return;
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    string prompt_copy(promptStr ? promptStr : "");
    env->ReleaseStringUTFChars(prompt, promptStr);

    g_state.running = true;
    jobject cbRef = env->NewGlobalRef(callback);

    g_state.inferenceThread = new thread([prompt_copy, cbRef]() {
        runInference(prompt_copy, cbRef);
    });
}

static void nativeStop(JNIEnv*, jobject) {
    LOGI("Stop requested");
    g_state.running = false;
    if (g_state.inferenceThread && g_state.inferenceThread->joinable()) {
        g_state.inferenceThread->join();
        delete g_state.inferenceThread;
        g_state.inferenceThread = nullptr;
    }
}

static jboolean nativeIsRunning(JNIEnv*, jobject) {
    return g_state.running ? JNI_TRUE : JNI_FALSE;
}

static void nativeRelease(JNIEnv*, jobject) {
    lock_guard<mutex> lock(g_state.mtx);
    if (g_state.running) {
        g_state.running = false;
        if (g_state.inferenceThread && g_state.inferenceThread->joinable()) {
            g_state.inferenceThread->join();
            delete g_state.inferenceThread;
            g_state.inferenceThread = nullptr;
        }
    }
    if (g_state.ctx)  { llama_free(g_state.ctx);  g_state.ctx  = nullptr; }
    if (g_state.model) { llama_free_model(g_state.model); g_state.model = nullptr; }
    LOGI("Released");
}

// ─── RegisterNatives: 显式绑定，彻底避免名称转义问题 ──────────────────────────
static const JNINativeMethod METHODS[] = {
    {
        "_init",                    // Kotlin: private external fun _init(...)
        "(Ljava/lang/String;)Z",
        (void*)nativeInit
    },
    {
        "_isLoaded",                // Kotlin: private external fun _isLoaded()
        "()Z",
        (void*)nativeIsModelLoaded
    },
    {
        "_getLastError",            // Kotlin: private external fun _getLastError()
        "()Ljava/lang/String;",
        (void*)nativeGetLastError
    },
    {
        "_generateAsync",           // Kotlin: private external fun _generateAsync(...)
        "(Ljava/lang/String;Lcom/cunyi/doctor/llm/LlamaEngine$GenerationCallback;)V",
        (void*)nativeGenerateAsync
    },
    {
        "_stop",                    // Kotlin: private external fun _stop()
        "()V",
        (void*)nativeStop
    },
    {
        "_isRunning",               // Kotlin: private external fun _isRunning()
        "()Z",
        (void*)nativeIsRunning
    },
    {
        "_release",                 // Kotlin: private external fun _release()
        "()V",
        (void*)nativeRelease
    }
};

static const int METHOD_COUNT = sizeof(METHODS) / sizeof(METHODS[0]);

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("GetEnv failed");
        return JNI_ERR;
    }

    jclass cls = env->FindClass("com/cunyi/doctor/llm/LlamaEngine");
    if (!cls) {
        LOGE("FindClass LlamaEngine failed");
        return JNI_ERR;
    }

    if (env->RegisterNatives(cls, METHODS, METHOD_COUNT) != 0) {
        LOGE("RegisterNatives failed");
        return JNI_ERR;
    }

    env->DeleteLocalRef(cls);
    LOGI("JNI_OnLoad OK — %d methods registered", METHOD_COUNT);
    return JNI_VERSION_1_6;
}
