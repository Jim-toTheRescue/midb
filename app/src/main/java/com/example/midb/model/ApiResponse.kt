package com.example.midb.model

data class ApiResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String = "",
    val rows_affected: Int = 0
)

data class SQLiteRequest(
    val sql: String,
    val params: List<Any>? = null
)

data class KVRequest(
    val key: String,
    val value: String
)
