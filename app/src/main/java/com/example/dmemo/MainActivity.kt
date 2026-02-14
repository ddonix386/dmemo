package com.example.dmemo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import com.example.dmemo.data.GroupRepository
import com.example.dmemo.data.GroupType
import com.example.dmemo.ui.screens.GroupManagementScreen
import com.example.dmemo.ui.theme.DmemoTheme
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DmemoTheme {
                DmemoApp()
            }
        }
    }
}

// 备忘录数据类
data class MemoItemData(
    val id: Long,
    val content: String,
    val group: String,
    val timestamp: Long
)

@Composable
fun DmemoApp() {
    val context = LocalContext.current
    val groupRepository = remember { GroupRepository(context) }
    
    var currentScreen by remember { mutableStateOf("main") }
    
    when (currentScreen) {
        "main" -> MainScreen(
            modifier = Modifier.fillMaxSize(),
            groupRepository = groupRepository,
            onNavigateToGroups = { currentScreen = "groups" }
        )
        "groups" -> GroupManagementScreen(
            modifier = Modifier.fillMaxSize(),
            onNavigateBack = { currentScreen = "main" },
            groupRepository = groupRepository
        )
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    groupRepository: GroupRepository,
    onNavigateToGroups: () -> Unit
) {
    val context = LocalContext.current
    
    // 使用 mutableStateOf 来存储备忘录列表
    val memos = remember { mutableStateListOf<MemoItemData>() }
    var newMemoText by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(groupRepository.groups.firstOrNull()?.name ?: "默认") }
    var showNewestFirst by remember { mutableStateOf(true) }
    var isSelecting by remember { mutableStateOf(false) }
    val selectedIndexes = remember { mutableStateListOf<Int>() }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showNumberError by remember { mutableStateOf(false) }
    
    // 获取当前分组的数据类型
    val currentGroupType = groupRepository.groups.find { it.name == selectedGroup }?.type ?: GroupType.TEXT
    
    // 当组件首次显示时加载备忘录
    LaunchedEffect(Unit) {
        memos.clear()
        memos.addAll(loadMemos(context))
    }
    
    // 添加备忘录的函数
    fun addMemo() {
        if (newMemoText.isNotBlank()) {
            // 如果分组是数字类型，检查输入是否为数字
            if (currentGroupType == GroupType.NUMBER) {
                try {
                    newMemoText.toDouble()
                    saveMemo(context, newMemoText, selectedGroup)
                    // 重新加载备忘录列表
                    memos.clear()
                    memos.addAll(loadMemos(context))
                    newMemoText = ""
                    showNumberError = false
                } catch (e: Exception) {
                    showNumberError = true
                }
            } else {
                saveMemo(context, newMemoText, selectedGroup)
                // 重新加载备忘录列表
                memos.clear()
                memos.addAll(loadMemos(context))
                newMemoText = ""
                showNumberError = false
            }
        }
    }
    
    // 全选/取消全选
    fun toggleSelectAll() {
        if (selectedIndexes.size == memos.size) {
            selectedIndexes.clear()
        } else {
            selectedIndexes.clear()
            selectedIndexes.addAll(memos.indices.toList())
        }
    }
    
    // 删除选中的备忘录
    fun deleteSelected() {
        fun saveAllMemos(context: Context, memoList: List<MemoItemData>) {
            val memoFile = File(context.filesDir, "memos.txt")
            try {
                FileWriter(memoFile, false).use { writer ->
                    memoList.forEach { memo ->
                        writer.write("${memo.timestamp}|${memo.group}|${memo.content}\n")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (selectedIndexes.isNotEmpty()) {
            val sortedIndexes = selectedIndexes.sortedDescending()
            sortedIndexes.forEach { index ->
                if (index >= 0 && index < memos.size) {
                    memos.removeAt(index)
                }
            }
            saveAllMemos(context, memos.toList())
            memos.clear()
            memos.addAll(loadMemos(context))
            selectedIndexes.clear()
            isSelecting = false
        }
    }
    
    Column(
        modifier = modifier
            .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("冬冬备忘", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onNavigateToGroups) {
                Text("分组管理")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // 选择模式的操作栏
        if (isSelecting) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    isSelecting = false
                    selectedIndexes.clear()
                }) {
                    Text("取消")
                }
                Checkbox(
                    checked = selectedIndexes.size == memos.size && memos.isNotEmpty(),
                    onCheckedChange = { toggleSelectAll() }
                )
                Text("全选")
                Button(onClick = {
                    deleteSelected()
                }) {
                    Text("删除")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 添加备忘录区域
        Column {
            // 分组选择器
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                TextField(
                    value = selectedGroup,
                    onValueChange = { },
                    label = { Text("分组") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(bottom = 8.dp),
                    readOnly = true
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    groupRepository.groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                selectedGroup = group.name
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMemoText,
                    onValueChange = { newMemoText = it },
                    label = { Text("添加备忘录") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            addMemo()
                        }
                    ),
                    isError = showNumberError
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { addMemo() }) {
                    Text("添加")
                }
            }
            
            // 数字格式错误提示
            if (showNumberError && currentGroupType == GroupType.NUMBER) {
                Text(
                    "请输入数字格式",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示顺序切换按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                showNewestFirst = !showNewestFirst
            }) {
                Text(
                    if (showNewestFirst) "↓ 最新在上" else "↑ 最新在下"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (memos.isEmpty()) {
            Text("暂无备忘录", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        } else {
            val sortedMemos = if (showNewestFirst) {
                memos.asReversed()
            } else {
                memos.toList()
            }
            
            LazyColumn {
                items(sortedMemos.size) { index ->
                    val actualIndex = if (showNewestFirst) {
                        memos.size - 1 - index
                    } else {
                        index
                    }
                    MemoItem(
                        memo = sortedMemos[index],
                        isSelected = selectedIndexes.contains(actualIndex),
                        isSelecting = isSelecting,
                        onClick = {
                            // 单击不执行任何操作
                        },
                        onLongClick = {
                            // 长按进入选择模式
                            isSelecting = true
                            selectedIndexes.clear()
                            selectedIndexes.add(actualIndex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MemoItem(
    memo: MemoItemData,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var clicked by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(memo.id) {
                detectTapGestures(
                    onTap = { clicked = true },
                    onLongPress = {
                        onLongClick()
                        clicked = false
                    }
                )
            }
            .clickable(
                enabled = !isSelecting,
                onClick = { 
                    if (!clicked) {
                        onClick()
                    }
                    clicked = false
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = if (isSelected) Arrangement.SpaceBetween else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null
                )
            }
            Column {
                Text(memo.content)
                Text(
                    "${memo.group} | ${SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(memo.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun loadMemos(context: Context): List<MemoItemData> {
    val memoFile = File(context.filesDir, "memos.txt")
    return if (memoFile.exists()) {
        memoFile.readLines(charset = Charsets.UTF_8).map { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size >= 3) {
                MemoItemData(
                    id = System.currentTimeMillis(),
                    content = parts[2],
                    group = parts[1],
                    timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis()
                )
            } else {
                MemoItemData(
                    id = System.currentTimeMillis(),
                    content = line,
                    group = "默认",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    } else {
        emptyList()
    }
}

fun saveMemo(context: Context, memo: String, group: String) {
    val memoFile = File(context.filesDir, "memos.txt")
    FileWriter(memoFile, true).use { writer ->
        writer.write("${System.currentTimeMillis()}|$group|$memo\n")
    }
}