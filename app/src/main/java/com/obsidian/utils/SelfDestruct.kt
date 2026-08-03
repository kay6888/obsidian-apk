package com.obsidian.utils

import android.content.Context
import android.os.Handler
import android.os.Looper

object SelfDestruct {
    
    private var isArmed = false
    private var triggerCount = 0
    private val maxAttempts = 3
    private var context: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    
    fun arm(context: Context) {
        this.context = context.applicationContext
        isArmed = true
        triggerCount = 0
        
        handler.postDelayed({
            if (isArmed) {
                wipe()
            }
        }, 7 * 24 * 60 * 60 * 1000L)
    }
    
    fun isTriggered(): Boolean = triggerCount >= maxAttempts
    
    fun recordAttempt() {
        if (!isArmed) return
        triggerCount++
        if (triggerCount >= maxAttempts) {
            wipe()
        }
    }
    
    fun trigger() {
        if (isArmed) {
            wipe()
        }
    }
    
    private fun wipe() {
        isArmed = false
        context?.let { ctx ->
            try {
                ctx.getSharedPreferences("obsidian_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                
                ctx.filesDir.listFiles()?.forEach { it.delete() }
                ctx.cacheDir.listFiles()?.forEach { it.delete() }
                
                val prefs = ctx.getSharedPreferences("obsidian_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("self_destructed", true).apply()
                
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            } catch (e: Exception) {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            }
        }
    }
    
    fun clean() {
        isArmed = false
        context = null
    }
}
