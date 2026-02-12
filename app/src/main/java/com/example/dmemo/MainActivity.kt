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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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

@Composable
fun DmemoApp() {
    val context = LocalContext.current
    // 使用 mutableStateOf 来存储备忘录列表，这样添加后会自动刷新
    val memos = remember { mutableStateListOf<String>() }
    var newMemoText by remember { mutableStateOf("") }
    var showNewestFirst by remember { mutableStateOf(true) } // 默认最新的在最上面
    var isSelecting by remember { mutableStateOf(false) } // 是否在选择模式
    val selectedIndexes = remember { mutableStateListOf<Int>() } // 选中的索引列表
    
    // 当组件首次显示时加载备忘录
    LaunchedEffect(Unit) {
        memos.clear()
        memos.addAll(loadMemos(context))
    }
    
    // 添加备忘录的函数
    fun addMemo() {
        if (newMemoText.isNotBlank()) {
            saveMemo(context, newMemoText)
            // 重新加载备忘录列表
            memos.clear()
            memos.addAll(loadMemos(context))
            newMemoText = ""
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
        // 保存所有备忘录
        fun saveAllMemos(context: Context, memoList: List<String>) {
            val memoFile = File(context.filesDir, "memos.txt")
            FileWriter(memoFile, false).use { writer ->
                memoList.forEach { memo ->
                    writer.write("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} - $memo\n")
                }
            }
        }
        
        if (selectedIndexes.isNotEmpty()) {
            // 按索引从大到小排序，避免删除时索引错乱
            val sortedIndexes = selectedIndexes.sortedDescending()
            // 删除对应的备忘录
            sortedIndexes.forEach { index ->
                if (index >= 0 && index < memos.size) {
                    memos.removeAt(index)
                }
            }
            // 清空选中列表
            selectedIndexes.clear()
            // 保存到文件
            saveAllMemos(context, memos.toList())
            // 重新加载备忘录列表以确保 UI 更新
            memos.clear()
            memos.addAll(loadMemos(context))
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text("冬冬备忘", style = MaterialTheme.typography.headlineMedium)
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
                    // 显示确认对话框
                    selectedIndexes.clear()
                    isSelecting = false
                    deleteSelected()
                }) {
                    Text("删除")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { addMemo() }) {
                Text("添加")
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
            // 根据显示顺序决定排序方式
            val sortedMemos = if (showNewestFirst) {
                memos.asReversed() // 最新的在最上面
            } else {
                memos.toList() // 最旧的在最上面
            }
            
            LazyColumn {
                items(sortedMemos.size) { index ->
                    // 获取实际的索引
                    val actualIndex = if (showNewestFirst) {
                        memos.size - 1 - index
                    } else {
                        index
                    }
                    MemoItem(
                        memo = sortedMemos[index],
                        isSelected = selectedIndexes.contains(actualIndex),
                        onSelect = {
                            if (isSelecting) {
                                if (selectedIndexes.contains(actualIndex)) {
                                    selectedIndexes.remove(actualIndex)
                                } else {
                                    selectedIndexes.add(actualIndex)
                                }
                            } else {
                                // 进入选择模式
                                isSelecting = true
                                selectedIndexes.clear()
                                selectedIndexes.add(actualIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MemoItem(
    memo: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSelect),
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
                    onCheckedChange = null // 只能通过点击卡片来切换
                )
            }
            Text(memo)
        }
    }
}

fun loadMemos(context: Context): List<String> {
    val memoFile = File(context.filesDir, "memos.txt")
    return if (memoFile.exists()) {
        memoFile.readLines(charset = Charsets.UTF_8)
    } else {
        emptyList()
    }
}

fun saveMemo(context: Context, memo: String) {
    val memoFile = File(context.filesDir, "memos.txt")
    FileWriter(memoFile, true).use { writer ->
        writer.write("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} - $memo\n")
    }
}