package com.cunyi.doctor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cunyi.doctor.databinding.ActivityMainBinding
import com.cunyi.doctor.R
import com.cunyi.doctor.llm.LlamaEngine
import com.cunyi.doctor.llm.ModelConfig
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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

    // 拍照相关
    private var currentPhotoPath: String? = null
    private var pendingImagePath: String? = null  // 待发送的图片路径

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "缺少通知权限", Toast.LENGTH_SHORT).show()
    }

    // 拍照权限
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    // 拍照
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                handleCapturedImage(path)
            }
        }
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

        // 图片选择按钮
        vb.btnPickImage.setOnClickListener {
            if (checkImagePermission()) {
                imagePickerLauncher.launch("image/*")
            }
        }

        // 拍照按钮
        vb.btnCamera.setOnClickListener {
            if (checkCameraPermission()) {
                launchCamera()
            }
        }
    }

    // 检查图片读取权限
    private fun checkImagePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                imagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                false
            } else true
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                imagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                false
            } else true
        }
    }

    // 图片权限 launcher
    private val imagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    // 检查相机权限
    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            false
        } else true
    }

    // 启动相机
    private fun launchCamera() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            currentPhotoPath = file.absolutePath
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(photoUri)
        }
    }

    // 创建图片文件
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 处理选择的图片
    private fun handleSelectedImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, "IMG_${timeStamp}.jpg")
            
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            pendingImagePath = file.absolutePath
            Toast.makeText(this, "已选择图片: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "图片选择失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 处理拍摄的照片
    private fun handleCapturedImage(path: String) {
        pendingImagePath = path
        Toast.makeText(this, "已拍摄照片", Toast.LENGTH_SHORT).show()
    }

    private fun submitMessage() {
        val text = vb.etInput.text?.toString()?.trim() ?: ""
        val imagePath = pendingImagePath
        
        // 至少要有文字或图片
        if (text.isEmpty() && imagePath == null) return
        
        vb.etInput.text?.clear()
        
        // 发送消息（带可选图片）
        vm.sendMessage(text, imagePath)
        
        // 清空待发送的图片
        pendingImagePath = null
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
                
                // OCR 状态观察
                launch {
                    vm.isRecognizing.collect { recognizing ->
                        vb.progressOcr.visibility = if (recognizing) View.VISIBLE else View.GONE
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
        val downloaded = engine.findDownloadedModel()

        if (engine.isModelLoaded) {
            vb.tvStatus.text = "✅ 模型已就绪"
            vb.btnSend.isEnabled = true
            vb.etInput.isEnabled = true
            vb.btnDownload.visibility = View.GONE
        } else if (downloaded != null) {
            // 已有模型文件，直接加载
            engine.selectModel(downloaded)
            vb.tvStatus.text = "正在加载 ${downloaded.name}..."
            vb.btnSend.isEnabled = false
            vb.etInput.isEnabled = false
            vb.btnDownload.visibility = View.GONE
            vm.loadModel { progress, msg ->
                runOnUiThread { updateLoadProgress(progress, msg) }
            }
        } else {
            // 没有任何模型文件，显示下载按钮
            vb.tvStatus.text = "📥 请下载模型"
            vb.btnSend.isEnabled = false
            vb.etInput.isEnabled = false
            vb.btnDownload.visibility = View.VISIBLE
            vb.btnDownload.setOnClickListener { showModelSelectDialog() }
        }
    }

    private fun showModelSelectDialog() {
        val modelNames = LlamaEngine.MODELS.map { "${it.name} — ${it.desc}" }.toTypedArray()
        var selectedIdx = 0

        AlertDialog.Builder(this)
            .setTitle("选择模型")
            .setSingleChoiceItems(modelNames, 0) { _, which -> selectedIdx = which }
            .setPositiveButton("下载") { _, _ ->
                val model = LlamaEngine.MODELS[selectedIdx]
                showDownloadConfirmDialog(model)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDownloadConfirmDialog(model: ModelConfig) {
        AlertDialog.Builder(this)
            .setTitle("下载 ${model.name}")
            .setMessage("即将下载 ${model.name}（约${model.size}），请确保：\n\n" +
                    "1. 连接Wi-Fi网络\n" +
                    "2. 手机存储空间充足\n" +
                    "3. 下载过程请勿关闭应用\n\n" +
                    "下载地址：\n${model.url}")
            .setCancelable(true)
            .setPositiveButton("开始下载") { _, _ ->
                vb.btnDownload.visibility = View.GONE
                vm.loadModel(model) { progress, msg ->
                    runOnUiThread { updateLoadProgress(progress, msg) }
                }
            }
            .setNegativeButton("取消", null)
            .show()
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
                            vb.btnDownload.setOnClickListener { showModelSelectDialog() }
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("模型加载失败")
                                .setMessage(state.msg)
                                .setPositiveButton("重新下载") { _, _ ->
                                    vb.btnDownload.visibility = View.GONE
                                    vm.loadModel { progress, msg ->
                                        runOnUiThread { updateLoadProgress(progress, msg) }
                                    }
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
