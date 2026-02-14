package com.example.dmemo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.dmemo.data.GroupRepository
import com.example.dmemo.data.MemoGroup

@Composable
fun GroupManagementScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    groupRepository: GroupRepository
) {
    var newGroupName by remember { mutableStateOf("") }
    var editingGroup by remember { mutableStateOf<MemoGroup?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameGroupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分组管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 分组列表
            LazyColumn {
                items(groupRepository.groups) { group ->
                    GroupItem(
                        group = group,
                        isDefault = group.name == "默认",
                        onRename = {
                            editingGroup = group
                            renameGroupName = group.name
                            showRenameDialog = true
                        },
                        onDelete = { if (group.name != "默认") groupRepository.deleteGroup(group.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 添加分组按钮
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showAddDialog = true }
            ) {
                Text("添加分组")
            }
        }
    }

    // 添加分组对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加分组") },
            text = {
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("分组名称") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newGroupName.isNotBlank() && !groupRepository.groupExists(newGroupName)) {
                                groupRepository.addGroup(newGroupName)
                                newGroupName = ""
                                showAddDialog = false
                            }
                        }
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank() && !groupRepository.groupExists(newGroupName)) {
                            groupRepository.addGroup(newGroupName)
                            newGroupName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 改名对话框
    if (showRenameDialog && editingGroup != null && editingGroup!!.name != "默认") {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("改名") },
            text = {
                TextField(
                    value = renameGroupName,
                    onValueChange = { renameGroupName = it },
                    label = { Text("新分组名称") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (renameGroupName.isNotBlank() && !groupRepository.groupExists(renameGroupName)) {
                                groupRepository.updateGroup(editingGroup!!.name, renameGroupName)
                                showRenameDialog = false
                            }
                        }
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameGroupName.isNotBlank() && !groupRepository.groupExists(renameGroupName)) {
                            groupRepository.updateGroup(editingGroup!!.name, renameGroupName)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun GroupItem(
    group: MemoGroup,
    isDefault: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { if (!isDefault) onRename() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(group.name)
                if (isDefault) {
                    Text(
                        "默认分组，不可删除或修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Row {
                if (!isDefault) {
                    TextButton(onClick = onRename) {
                        Text("改名")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}