package com.example.touchlock

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class TouchLockService : Service() {

    private lateinit var windowManager: WindowManager
    private var blockView: View? = null
    private var floatingButton: View? = null
    private var lastClickTime: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        setupOverlayViews()
    }

    private fun setupOverlayViews() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // A. 全螢幕透明阻擋層（攔截所有點擊與滑動）
        val blockParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        val blockLayout = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, _ -> true }
        }
        blockView = blockLayout
        windowManager.addView(blockLayout, blockParams)

        // B. 右下角的懸浮鎖頭小按鈕
        val btnParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 40
            y = 100
        }

        val lockBtn = TextView(this).apply {
            text = "🔒"
            textSize = 32f
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#66000000"))

            setOnClickListener {
                val currentTime = SystemClock.elapsedRealtime()
                if (currentTime - lastClickTime < 500) {
                    showUnlockDialog()
                } else {
                    Toast.makeText(this@TouchLockService, "連按兩下輸入密碼解鎖", Toast.LENGTH_SHORT).show()
                }
                lastClickTime = currentTime
            }
        }
        floatingButton = lockBtn
        windowManager.addView(lockBtn, btnParams)
    }

    private fun showUnlockDialog() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(60, 40, 60, 40)
        }

        val title = TextView(this).apply {
            text = "請輸入解鎖密碼"
            textSize = 20f
            setTextColor(Color.BLACK)
        }
        container.addView(title)

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTextColor(Color.BLACK)
        }
        container.addView(input)

        val btnConfirm = Button(this).apply {
            text = "解除鎖定"
        }
        container.addView(btnConfirm)

        windowManager.addView(container, dialogParams)

        btnConfirm.setOnClickListener {
            val sp = getSharedPreferences("TouchLockPref", Context.MODE_PRIVATE)
            val savedPass = sp.getString("LOCK_PASSWORD", "1234")

            if (input.text.toString() == savedPass) {
                Toast.makeText(this, "觸控鎖定已解除！", Toast.LENGTH_SHORT).show()
                windowManager.removeView(container)
                stopSelf()
            } else {
                Toast.makeText(this, "密碼錯誤！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "touch_lock_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "觸控鎖定服務",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("觸控鎖定運作中")
            .setContentText("雙擊螢幕小鎖頭即可輸入密碼解除")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        blockView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        floatingButton?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
    }
}
