package com.lifereset.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuestEntity::class, CompletionEntity::class, SettingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LifeResetDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
    abstract fun completionDao(): CompletionDao
    abstract fun settingDao(): SettingDao

    companion object {
        fun create(context: Context): LifeResetDatabase =
            Room.databaseBuilder(context, LifeResetDatabase::class.java, "life-reset.db")
                // 버전을 올릴 땐 여기에 .addMigrations(...) 를 추가한다.
                // 기록은 서버에도 있으므로, 최악의 경우 '서버에서 복원'으로 되살릴 수 있다.
                .build()
    }
}
