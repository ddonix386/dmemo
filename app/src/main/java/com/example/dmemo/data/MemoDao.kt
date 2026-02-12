package com.example.dmemo.data

import androidx.room.*

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY timestamp DESC")
    suspend fun getAllMemos(): List<MemoEntity>

    @Query("SELECT * FROM memos ORDER BY timestamp ASC")
    suspend fun getAllMemosAscending(): List<MemoEntity>

    @Insert
    suspend fun insertMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("DELETE FROM memos WHERE id IN (:ids)")
    suspend fun deleteMemosByIds(ids: List<Long>)
}

@Database(
    entities = [MemoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: MemoDatabase? = null

        fun getDatabase(context: android.content.Context): MemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java,
                    "memo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}