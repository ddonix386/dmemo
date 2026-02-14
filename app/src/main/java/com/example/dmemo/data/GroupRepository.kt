package com.example.dmemo.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.*
import java.util.*

// 分组数据类
data class MemoGroup(
    val name: String,
    val type: GroupType = GroupType.TEXT,
    val color: Int = 0xFFE0E0E0.toInt()
) {
    companion object {
        fun fromString(line: String): MemoGroup {
            val parts = line.split("|", limit = 3)
            return MemoGroup(
                name = parts.getOrNull(0) ?: "默认",
                type = GroupType.fromString(parts.getOrNull(1) ?: "text"),
                color = parts.getOrNull(2)?.toIntOrNull() ?: 0xFFE0E0E0.toInt()
            )
        }
    }
}

// 分组数据类型
enum class GroupType(val value: String, val label: String) {
    TEXT("text", "文本"),
    NUMBER("number", "数字");

    companion object {
        fun fromString(value: String): GroupType {
            return values().firstOrNull { it.value == value } ?: TEXT
        }
    }
}

class GroupRepository(private val context: Context) {
    private val groupFile = File(context.filesDir, "groups.txt")
    private val memoFile = File(context.filesDir, "memos.txt")
    
    var groups by mutableStateOf<List<MemoGroup>>(emptyList())
        private set
    
    init {
        loadGroups()
    }
    
    fun loadGroups() {
        groups = if (groupFile.exists()) {
            groupFile.readLines(charset = Charsets.UTF_8).map { line ->
                MemoGroup.fromString(line)
            }
        } else {
            // 默认分组
            listOf(
                MemoGroup("默认", type = GroupType.TEXT, color = 0xFFE0E0E0.toInt()),
                MemoGroup("工作", type = GroupType.TEXT, color = 0xFFBBDEFB.toInt()),
                MemoGroup("生活", type = GroupType.TEXT, color = 0xFFC8E6C9.toInt()),
                MemoGroup("学习", type = GroupType.TEXT, color = 0xFFF0F4C3.toInt()),
                MemoGroup("购物", type = GroupType.TEXT, color = 0xFFFFCDD2.toInt())
            )
        }
    }
    
    fun saveGroups() {
        try {
            FileWriter(groupFile, false).use { writer ->
                groups.forEach { group ->
                    writer.write("${group.name}|${group.type.value}|${group.color}\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun groupExists(name: String): Boolean {
        return groups.any { it.name.equals(name, ignoreCase = true) }
    }
    
    fun addGroup(name: String, type: GroupType = GroupType.TEXT) {
        if (name.isNotBlank() && !groupExists(name)) {
            groups = groups + MemoGroup(name, type = type)
            saveGroups()
        }
    }
    
    fun hasMemosInGroup(groupName: String): Boolean {
        if (!memoFile.exists()) return false
        return memoFile.readLines(charset = Charsets.UTF_8).any { line ->
            val parts = line.split("|", limit = 3)
            parts.getOrNull(1) == groupName
        }
    }
    
    fun deleteGroup(name: String) {
        if (name != "默认") {
            groups = groups.filter { it.name != name }
            saveGroups()
        }
    }
    
    fun deleteGroupAndMemos(name: String) {
        if (name != "默认") {
            // 删除该分组下的所有备忘录
            if (memoFile.exists()) {
                val lines = memoFile.readLines(charset = Charsets.UTF_8)
                val filteredLines = lines.filter { line ->
                    val parts = line.split("|", limit = 3)
                    parts.getOrNull(1) != name
                }
                try {
                    FileWriter(memoFile, false).use { writer ->
                        filteredLines.forEach { writer.write(it + "\n") }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            groups = groups.filter { it.name != name }
            saveGroups()
        }
    }
    
    fun updateGroup(oldName: String, newName: String) {
        if (oldName != "默认" && newName.isNotBlank() && !groupExists(newName)) {
            groups = groups.map { 
                if (it.name == oldName) it.copy(name = newName) 
                else it 
            }
            saveGroups()
        }
    }
}