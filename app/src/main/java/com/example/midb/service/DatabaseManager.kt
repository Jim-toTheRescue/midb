package com.example.midb.service

import android.content.Context

class DatabaseManager(context: Context) {
    val sqliteService = SQLiteService(context)
    val levelDBService = LevelDBService(context)
    
    fun closeAll() {
        sqliteService.close()
        levelDBService.close()
    }
}
