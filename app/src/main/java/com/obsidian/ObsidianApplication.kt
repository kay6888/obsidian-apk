package com.obsidian

import android.app.Application
import android.content.Context
import com.obsidian.utils.SelfDestruct

class ObsidianApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        val prefs = getSharedPreferences("obsidian_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("self_destruct_armed", false)) {
            SelfDestruct.arm(this)
            prefs.edit().putBoolean("self_destruct_armed", true).apply()
        }
    }
}
