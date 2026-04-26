package com.example.midb.server

import com.example.midb.service.DatabaseManager
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Method
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MidbHttpServer(port: Int, private val context: android.content.Context) : NanoHTTPD(port) {
    
    private val dbManager = DatabaseManager(context)
    
    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.uri.startsWith("/sqlite/execute") -> handleSQLite(session)
                session.uri.startsWith("/kv/get") -> handleKVGet(session)
                session.uri.startsWith("/kv/set") -> handleKVSet(session)
                session.uri.startsWith("/admin/") -> serveAdminPage(session)
                session.uri == "/" -> newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "").apply { addHeader("Location", "/admin/") }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorJson = JSONObject().apply {
                put("success", false)
                put("message", e.message ?: "Internal server error")
            }.toString()
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", errorJson)
        }
    }
    
    private fun handleSQLite(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method not allowed")
        }
        
        val body = readBody(session)
        val json = JSONObject(body)
        val sql = json.getString("sql")
        val params = if (json.has("params")) {
            val arr = json.getJSONArray("params")
            Array(arr.length()) { i -> arr.get(i) }
        } else null
        
        val result = dbManager.sqliteService.execute(sql, params)
        val responseJson = JSONObject().apply {
            put("success", result.success)
            put("data", result.data)
            put("message", result.message)
            put("rows_affected", result.rows_affected)
        }
        
        return newFixedLengthResponse(Response.Status.OK, "application/json", responseJson.toString())
    }
    
    private fun handleKVGet(session: IHTTPSession): Response {
        val key = session.parms["key"] ?: ""
        if (key.isEmpty()) {
            val error = JSONObject().put("success", false).put("message", "Key cannot be empty").toString()
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", error)
        }
        
        val value = dbManager.levelDBService.get(key)
        val responseJson = JSONObject().apply {
            put("success", true)
            put("key", key)
            put("value", value)
            put("exists", value != null)
        }
        
        return newFixedLengthResponse(Response.Status.OK, "application/json", responseJson.toString())
    }
    
    private fun handleKVSet(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method not allowed")
        }
        
        val body = readBody(session)
        val json = JSONObject(body)
        val key = json.optString("key", "")
        val value = json.optString("value", "")
        
        if (key.isEmpty()) {
            val error = JSONObject().put("success", false).put("message", "Key and value are required").toString()
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", error)
        }
        
        dbManager.levelDBService.set(key, value)
        val responseJson = JSONObject().apply {
            put("success", true)
            put("key", key)
            put("message", "Key set successfully")
        }
        
        return newFixedLengthResponse(Response.Status.OK, "application/json", responseJson.toString())
    }
    
    private fun serveAdminPage(session: IHTTPSession): Response {
        val path = if (session.uri == "/admin/" || session.uri == "/admin") {
            "index.html"
        } else {
            session.uri.substringAfterLast("/")
        }
        
        return try {
            val inputStream = context.assets.open("admin/$path")
            val mimeType = when {
                path.endsWith(".html") -> "text/html"
                path.endsWith(".css") -> "text/css"
                path.endsWith(".js") -> "application/javascript"
                else -> "text/plain"
            }
            newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, inputStream.available().toLong())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found: $path")
        }
    }
    
    private fun readBody(session: IHTTPSession): String {
        val body = HashMap<String, String>()
        session.parseBody(body)
        return body["postData"] ?: body.values.firstOrNull() ?: ""
    }
    
    fun stopServer() {
        dbManager.closeAll()
        stop()
    }
}
