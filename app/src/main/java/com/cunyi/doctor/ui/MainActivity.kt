package com.cunyi.doctor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cunyi.doctor.databinding.ActivityMainBinding
import com.cunyi.doctor.R
import com.cunyi.doctor.llm.LlamaEngine
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var vb: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    // TTS 相关
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsEnabled = true  // 默认开启
    private var pendingText: String? = null  // TTS 还没就绪时暂存的文字

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "缺少通知权限", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        requestNotificationPermission()
        setupToolbar()
        setupRecyclerView()
        setupInputBar()
        initTts()
        observeState()
        observeModelLoadState()
        autoLoadModel()
    }

    // ─── TTS 初始化 ───────────────────────────────────────────────────────────
    private fun initTts() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINA)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 尝试大陆中文
                tts?.setLanguage(Locale.CHINESE)?.let { r ->
                    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsReady = false
                        return
                    }
                }
            }
            ttsReady = true
            // TTS 就绪后，播报暂存的文字
            pendingText?.let { speak(it) }
            pendingText = null
        } else {
            ttsReady = false
        }
    }

    private fun speak(text: String) {
        if (!ttsEnabled || !ttsReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "doctor_response")
    }

    private fun stopSpeaking() {
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    // ─── Toolbar 菜单 ────────────────────────────────────────────────────────
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        updateTtsMenuIcon(menu.findItem(R.id.action_tts))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_tts -> {
                ttsEnabled = !ttsEnabled
                item.setIcon(if (ttsEnabled) android.R.drawable.ic_lock_silent_mode_off
                             else android.R.drawable.ic_lock_silent_mode)
                item.title = if (ttsEnabled) "关闭语音" else "开启语音"
                if (!ttsEnabled) stopSpeaking()
                Toast.makeText(this,
                    if (ttsEnabled) "🔊 语音播报已开启" else "🔇 语音播报已关闭",
                    Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateTtsMenuIcon(item: MenuItem?) {
        item?.setIcon(if (ttsEnabled) android.R.drawable.ic_lock_silent_mode_off
                      else android.R.drawable.ic_lock_silent_mode)
        item?.title = if (ttsEnabled) "关闭语音" else "开启语音"
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupToolbar() {
        vb.toolbar.title = "村医AI"
        vb.toolbar.subtitle = "本地离线 · 隐私安全"
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        vb.recyclerChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupInputBar() {
        vb.btnSend.setOnClickListener { submitMessage() }

        vb.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitMessage(); true
            } else false
        }
    }

    private fun submitMessage() {
        val text = vb.etInput.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        vb.etInput.text?.clear()
        vm.sendMessage(text)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.modelLoadState.collect { state ->
                        when (state) {
                            is LlamaEngine.LoadState.Idle -> {
                                vb.tvStatus.text = "等待加载模型..."
                                vb.progressLoad.visibility = View.GONE
                            }
                            is LlamaEngine.LoadState.Loading -> {
                                vb.progressLoad.visibility = View.VISIBLE
                            }
                            is LlamaEngine.LoadState.Loaded -> {
                                vb.tvStatus.text = "✅ 模型已就绪"
                                vb.progressLoad.visibility = View.GONE
                                vb.btnSend.isEnabled = true
                                vb.etInput.isEnabled = true
                            }
                            is LlamaEngine.LoadState.Error -> {
                                vb.tvStatus.text = "❌ 加载失败: ${state.msg}"
                                vb.progressLoad.visibility = View.GONE
                                vb.btnSend.isEnabled = false
                                vb.btnRetry.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    vm.chatMessages.collect { msgs ->
                        chatAdapter.submitList(msgs)
                        if (msgs.isNotEmpty()) {
                            vb.recyclerChat.scrollToPosition(msgs.size - 1)
                        }
                    }
                }

                launch {
                    vm.currentResponse.collect { resp ->
                        if (resp.isNotEmpty()) {
                            chatAdapter.setStreamingResponse(resp)
                            vb.recyclerChat.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
                }

                launch {
                    vm.isLoading.collect { loading ->
                        vb.btnSend.visibility = if (loading) View.GONE else View.VISIBLE
                        vb.btnStop.visibility = if (loading) View.VISIBLE else View.GONE
                        vb.progressTyping.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                // TTS：监听消息列表变化，当新增 AI 消息时播报
                launch {
                    var lastMessageCount = 0
                    vm.chatMessages.collect { msgs ->
                        if (msgs.size > lastMessageCount) {
                            val lastMsg = msgs.lastOrNull()
                            // 只播报 AI 的消息（非用户消息）
                            if (lastMsg != null && !lastMsg.isUser) {
                                speak(lastMsg.content)
                            }
                        }
                        lastMessageCount = msgs.size
                    }
                }
            }
        }
    }

    private fun autoLoadModel() {
        val engine = LlamaEngine.getInstance(this)
        val modelFile = engine.getModelFile()

        if (engine.isModelLoaded) {
            vb.tvStatus.text = "✅ 模型已就绪"
            vb.btnSend.isEnabled = true
            vb.etInput.isEnabled = true
            vb.btnDownload.visibility = View.GONE
        } else if (modelFile.exists()) {
            // 模型文件存在但未加载，直接加载
            vb.btnSend.isEnabled = false
            vb.etInput.isEnabled = false
            vb.btnDownload.visibility = View.GONE
            vm.loadModel { progress, msg ->
                runOnUiThread { updateLoadProgress(progress, msg) }
            }
        } else {
            // 模型文件不存在，显示下载按钮
            vb.tvStatus.text = "📥 模型未下载（约${LlamaEngine.MODEL_SIZE}）"
            vb.btnSend.isEnabled = false
            vb.etInput.isEnabled = false
            vb.btnDownload.visibility = View.VISIBLE
            vb.btnDownload.setOnClickListener { showDownloadConfirmDialog() }
        }
    }

    private fun showDownloadConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("下载模型")
            .setMessage("村医AI需要下载大语言模型（约${LlamaEngine.MODEL_SIZE}），请确保：\n\n" +
                    "1. 连接Wi-Fi网络\n" +
                    "2. 手机存储空间充足\n" +
                    "3. 下载过程请勿关闭应用\n\n" +
                    "下载地址：\n${LlamaEngine.MODEL_URL}")
            .setCancelable(true)
            .setPositiveButton("开始下载") { _, _ ->
                vb.btnDownload.visibility = View.GONE
                startModelDownload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startModelDownload() {
        vm.loadModel { progress, msg ->
            runOnUiThread { updateLoadProgress(progress, msg) }
        }
    }



    private var loadDialog: AlertDialog? = null

    private fun updateLoadProgress(progress: Int, msg: String) {
        // 进度 100% 时关闭对话框
        if (progress >= 100) {
            if (loadDialog?.isShowing == true) {
                loadDialog?.dismiss()
            }
            loadDialog = null
            return
        }

        if (loadDialog == null) {
            val view = layoutInflater.inflate(R.layout.dialog_download, null)
            loadDialog = AlertDialog.Builder(this)
                .setTitle("正在下载模型")
                .setView(view)
                .setCancelable(false)
                .create()
            loadDialog?.show()
        }
        loadDialog?.findViewById<android.widget.ProgressBar>(R.id.pb_download)?.progress = progress
        loadDialog?.findViewById<android.widget.TextView>(R.id.tv_download_msg)?.text = msg
        loadDialog?.setMessage("$progress%  $msg")
    }

    // 监听模型加载状态
    private fun observeModelLoadState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.modelLoadState.collect { state ->
                    when (state) {
                        is LlamaEngine.LoadState.Loaded -> {
                            // 确保对话框关闭
                            if (loadDialog?.isShowing == true) {
                                loadDialog?.dismiss()
                            }
                            loadDialog = null
                            vb.btnDownload.visibility = View.GONE
                        }
                        is LlamaEngine.LoadState.Error -> {
                            // 确保对话框关闭
                            if (loadDialog?.isShowing == true) {
                                loadDialog?.dismiss()
                            }
                            loadDialog = null
                            vb.btnDownload.visibility = View.VISIBLE
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("模型加载失败")
                                .setMessage(state.msg)
                                .setPositiveButton("重新下载") { _, _ ->
                                    startModelDownload()
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                        else -> { /* 继续显示加载中 */ }
                    }
                }
            }
        }
    }
}
