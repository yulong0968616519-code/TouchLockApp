package com.example.touchlock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnStartLock = findViewById<Button>(R.id.btnStartLock)

        btnStartLock.setOnClickListener {
            // 檢查懸浮窗權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "請先開啟「在其他應用程式上層顯示」權限", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return@setOnClickListener
            }

            // 儲存密碼
            val pass = etPassword.text.toString().ifEmpty { "1234" }
            val sp = getSharedPreferences("TouchLockPref", Context.MODE_PRIVATE)
            sp.edit().putString("LOCK_PASSWORD", pass).apply()

            // 啟動鎖定服務
            val serviceIntent = Intent(this, TouchLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // 返回桌面
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }
}
