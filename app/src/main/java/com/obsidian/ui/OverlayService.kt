package com.obsidian.ui

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.obsidian.R

class OverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        val skullIcon = overlayView.findViewById<ImageView>(R.id.skull_icon)
        val statusBadge = overlayView.findViewById<TextView>(R.id.status_badge)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 20
        params.y = 100
        
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        overlayView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + deltaY.toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        openDashboard()
                    }
                    true
                }
                else -> false
            }
        }
        
        windowManager.addView(overlayView, params)
    }
    
    private fun openDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
