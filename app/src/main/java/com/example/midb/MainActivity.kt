package com.example.midb

import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.midb.server.MidbHttpServer

class MainActivity : AppCompatActivity() {
    
    private var server: MidbHttpServer? = null
    private var isRunning = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val btnStartStop = findViewById<Button>(R.id.btnStartStop)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvIpAddress = findViewById<TextView>(R.id.tvIpAddress)
        
        val ip = getLocalIpAddress()
        val port = 8086
        tvIpAddress.text = "设备IP: ${ip ?: "未知"}\n管理端: http://${ip ?: "localhost"}:$port/admin/"
        
        btnStartStop.setOnClickListener {
            if (isRunning) {
                stopServer()
                btnStartStop.text = "启动服务"
                tvStatus.text = "服务已停止"
                isRunning = false
            } else {
                startServer(port)
                btnStartStop.text = "停止服务"
                tvStatus.text = "服务运行中\n端口: $port"
                isRunning = true
            }
        }
    }
    
    private fun startServer(port: Int) {
        Thread {
            try {
                server = MidbHttpServer(port, this)
                server?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun stopServer() {
        server?.stopServer()
        server = null
    }
    
    private fun getLocalIpAddress(): String? {
        try {
            val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
            val ip = android.text.format.Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            return ip
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }
}
