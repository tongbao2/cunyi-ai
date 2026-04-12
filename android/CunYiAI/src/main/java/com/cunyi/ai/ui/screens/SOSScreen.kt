package com.cunyi.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cunyi.ai.data.EmergencyContact
import com.cunyi.ai.manager.SOSManager
import com.cunyi.ai.ui.components.*
import com.cunyi.ai.ui.theme.*

/**
 * SOS 紧急求救页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSScreen(
    sosManager: SOSManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(sosManager.getContacts()) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showSOSDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    
    // 权限Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showSOSDialog = true
        } else {
            Toast.makeText(context, "需要短信权限才能发送求救", Toast.LENGTH_LONG).show()
        }
    }

    // 检查权限并发送SOS
    fun checkPermissionAndSend() {
        val smsPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        )
        
        if (smsPermission == PackageManager.PERMISSION_GRANTED) {
            showSOSDialog = true
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("紧急求救", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AlertRed,
                    titleContentColor = TextOnPrimary,
                    navigationIconContentColor = TextOnPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimensions.SpacingL.dp),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingL.dp)
        ) {
            // SOS 大按钮
            SOSButton(
                onClick = { checkPermissionAndSend() },
                enabled = !isSending && contacts.isNotEmpty()
            )

            if (contacts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AlertOrange.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "⚠️ 请先添加紧急联系人",
                        modifier = Modifier.padding(Dimensions.SpacingL.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AlertOrange
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 紧急联系人列表
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
            ) {
                Column(modifier = Modifier.padding(Dimensions.SpacingL.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "紧急联系人",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showAddContactDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                "添加",
                                tint = PrimaryGreen
                            )
                        }
                    }

                    if (contacts.isEmpty()) {
                        Text(
                            text = "暂无联系人\n点击 + 添加紧急联系人",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = Dimensions.SpacingL.dp)
                        )
                    } else {
                        contacts.forEach { contact ->
                            ContactItem(
                                contact = contact,
                                onDelete = {
                                    sosManager.deleteContact(contact.id)
                                    contacts = sosManager.getContacts()
                                },
                                onSetPrimary = {
                                    sosManager.setPrimaryContact(contact.id)
                                    contacts = sosManager.getContacts()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加联系人对话框
    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, phone, relationship ->
                val contact = EmergencyContact(
                    name = name,
                    phone = phone,
                    relationship = relationship,
                    isPrimary = contacts.isEmpty()
                )
                sosManager.addContact(contact)
                contacts = sosManager.getContacts()
                showAddContactDialog = false
            }
        )
    }

    // SOS 确认对话框
    if (showSOSDialog) {
        SOSConfirmDialog(
            sosManager = sosManager,
            onDismiss = {
                showSOSDialog = false
                isSending = false
            },
            onSending = { isSending = true },
            onSuccess = {
                isSending = false
                showSOSDialog = false
                Toast.makeText(context, "求救短信已发送！", Toast.LENGTH_LONG).show()
            },
            onError = { error ->
                isSending = false
                showSOSDialog = false
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
private fun ContactItem(
    contact: EmergencyContact,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.SpacingS.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "主要",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryGreen
                    )
                }
            }
            Text(
                text = "${contact.phone} ${if (contact.relationship.isNotEmpty()) "(${contact.relationship})" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Row {
            if (!contact.isPrimary) {
                IconButton(onClick = onSetPrimary) {
                    Icon(
                        Icons.Default.Star,
                        "设为主要",
                        tint = TextSecondary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = AlertRed
                )
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加紧急联系人", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("电话") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("关系（选填）") },
                    placeholder = { Text("如：子女、配偶、邻居") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name.trim(), phone.trim(), relationship.trim())
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("添加", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

@Composable
private fun SOSConfirmDialog(
    sosManager: SOSManager,
    onDismiss: () -> Unit,
    onSending: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    var symptoms by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AlertRed.copy(alpha = 0.1f),
        title = { 
            Text(
                "⚠️ 确认发送求救短信？",
                style = MaterialTheme.typography.headlineSmall,
                color = AlertRed
            ) 
        },
        text = {
            Column {
                Text(
                    text = "将向以下联系人发送求救短信：",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                sosManager.getContacts().forEach { contact ->
                    Text(
                        text = "• ${contact.name} (${contact.phone})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "请描述当前症状：",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = symptoms,
                    onValueChange = { symptoms = it },
                    placeholder = { Text("如：胸闷、头晕、呼吸困难") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSending()
                    val symptomList = symptoms.split("、", "，", ",", " ")
                        .filter { it.isNotBlank() }
                    sosManager.sendSOS(
                        symptoms = symptomList,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
            ) {
                Text("确认发送", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}
