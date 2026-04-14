package com.cunyi.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.cunyi.ai.data.EmergencyContact
import com.cunyi.ai.manager.SOSManager
import com.cunyi.ai.ui.components.*
import com.cunyi.ai.ui.theme.*

/**
 * SOS 紧急求救页面 - 一键拨打电话
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
            // 拨打120大按钮
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:120"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                shape = MaterialTheme.shapes.large
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = TextOnPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "拨打 120 急救电话",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextOnPrimary
                    )
                }
            }

            // 拨打联系人电话
            if (contacts.isNotEmpty()) {
                val primaryContact = contacts.firstOrNull { it.isPrimary } ?: contacts.first()
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${primaryContact.phone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertOrange),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "联系 ${primaryContact.name} (${primaryContact.phone})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (contacts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AlertOrange.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "⚠️ 请先添加紧急联系人，紧急时可一键拨打",
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
                            Icon(Icons.Default.Add, "添加", tint = PrimaryGreen)
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
                                },
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

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
}

@Composable
private fun ContactItem(
    contact: EmergencyContact,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit,
    onCall: () -> Unit
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
                Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "主要", style = MaterialTheme.typography.labelMedium, color = PrimaryGreen)
                }
            }
            Text(
                text = "${contact.phone} ${if (contact.relationship.isNotEmpty()) "(${contact.relationship})" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Row {
            IconButton(onClick = onCall) {
                Icon(Icons.Default.Phone, "拨打", tint = PrimaryGreen)
            }
            if (!contact.isPrimary) {
                IconButton(onClick = onSetPrimary) {
                    Icon(Icons.Default.Star, "设为主要", tint = TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = AlertRed)
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("电话") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = relationship, onValueChange = { relationship = it }, label = { Text("关系（选填）") }, placeholder = { Text("如：子女、配偶、邻居") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onConfirm(name.trim(), phone.trim(), relationship.trim()) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("添加", style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", style = MaterialTheme.typography.titleMedium) }
        }
    )
}
