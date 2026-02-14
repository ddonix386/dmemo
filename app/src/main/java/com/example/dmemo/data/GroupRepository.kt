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
    val color: Int = 0xFFE0E0E0.toInt()
) {
    companion object {
        fun fromString(line: String): MemoGroup {
            val parts = line.split("|", limit = 2)
            return MemoGroup(
                name = parts.getOrNull(0) ?: "默认",
                color = parts.getOrNull(1)?.toIntOrNull() ?: 0xFFE0E0E0.toInt()
            )
        }
    }
}

class GroupRepository(private val context: Context) {
    private val groupFile = File(context.filesDir, "groups.txt")
    
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
                MemoGroup("默认", 0xFFE0E0E0.toInt()),
                MemoGroup("工作", 0xFFBBDEFB.toInt()),
                MemoGroup("生活", 0xFFC8E6C9.toInt()),
                MemoGroup("学习", 0xFFF0F4C3.toInt()),
                MemoGroup("购物", 0xFFFFCDD2.toInt())
            )
        }
    }
    
    fun saveGroups() {
        try {
            FileWriter(groupFile, false).use { writer ->
                groups.forEach { group ->
                    writer.write("${group.name}|${group.color}\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun groupExists(name: String): Boolean {
        return groups.any { it.name.equals(name, ignoreCase = true) }
    }
    
    fun addGroup(name: String) {
        if (name.isNotBlank() && !groupExists(name)) {
            groups = groups + MemoGroup(name)
            saveGroups()
        }
    }
    
    fun deleteGroup(name: String) {
        if (name != "默认") {
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