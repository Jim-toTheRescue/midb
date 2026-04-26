package com.example.midb

import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.midb.server.MidbHttpServer
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    
    private var server: MidbHttpServer? = null
    private var isRunning = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val btnStartStop = findViewById<Button>(R.id.btnStartStop)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvIpAddress = findViewById<TextView>(R.id.tvIpAddress)
        
        // 显示IP地址
        val ip = getLocalIpAddress()
        tvIpAddress.text = "设备IP: ${ip ?: "未知"}\n管理端: http://${ip ?: "localhost"}:8080/admin/"
        
        btnStartStop.setOnClickListener {
            if (isRunning) {
                stopServer()
                btnStartStop.text = "启动服务"
                tvStatus.text = "服务已停止"
                isRunning = false
            } else {
                startServer()
                btnStartStop.text = "停止服务"
                tvStatus.text = "服务运行中\n访问: http://${ip ?: "localhost"}:8080/admin/"
                isRunning = true
            }
        }
    }
    
    private fun startServer() {
        Thread {
            try {
                server = MidbHttpServer(8080, this)
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
            val ip = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
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
