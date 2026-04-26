package com.example.midb.service

import android.content.Context
import java.io.File

class LevelDBService(context: Context) {
    private val dbPath = File(context.filesDir, "leveldb_data").absolutePath
    private var levelDB: com.github.hf.leveldb.LevelDB? = null
    
    init {
        try {
            val dir = File(context.filesDir, "leveldb_data")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            levelDB = com.github.hf.leveldb.LevelDB.open(
                dir.absolutePath,
                com.github.hf.leveldb.LevelDB.configure().createIfMissing(true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun get(key: String): String? {
        return try {
            levelDB?.get(key.toByteArray())?.let { String(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun set(key: String, value: String) {
        try {
            levelDB?.put(key.toByteArray(), value.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun delete(key: String) {
        try {
            levelDB?.delete(key.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun close() {
        try {
            levelDB?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
