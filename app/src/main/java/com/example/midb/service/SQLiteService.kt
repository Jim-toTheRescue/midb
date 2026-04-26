package com.example.midb.service

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.midb.model.ApiResponse
import org.json.JSONArray
import org.json.JSONObject

class SQLiteService(context: Context) {
    private val dbHelper = object : SQLiteOpenHelper(context, "midb.sqlite", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            // 可选: 创建默认表
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
    
    fun execute(sql: String, params: Array<Any?>? = null): ApiResponse {
        return try {
            val db = dbHelper.writableDatabase
            val trimmedSql = sql.trim().uppercase()
            
            if (trimmedSql.startsWith("SELECT") || trimmedSql.startsWith("PRAGMA")) {
                // 查询操作
                val cursor = if (params != null) {
                    db.rawQuery(sql, params.map { it?.toString() ?: "" }.toTypedArray())
                } else {
                    db.rawQuery(sql, null)
                }
                val result = cursorToJson(cursor)
                cursor.close()
                ApiResponse(true, result, "Query executed successfully")
            } else {
                // 更新操作
                if (params != null) {
                    db.execSQL(sql, params)
                } else {
                    db.execSQL(sql)
                }
                ApiResponse(true, null, "Executed successfully", 1)
            }
        } catch (e: Exception) {
            ApiResponse(false, null, e.message ?: "Unknown error")
        }
    }
    
    private fun cursorToJson(cursor: Cursor): JSONArray {
        val result = JSONArray()
        val columnNames = cursor.columnNames
        
        while (cursor.moveToNext()) {
            val row = JSONObject()
            for (i in 0 until cursor.columnCount) {
                val columnName = columnNames[i]
                val value = when (cursor.getType(i)) {
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                    Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                    Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                    Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(i)?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
                    else -> null
                }
                row.put(columnName, value)
            }
            result.put(row)
        }
        return result
    }
    
    fun close() {
        dbHelper.close()
    }
}
