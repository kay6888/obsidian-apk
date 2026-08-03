package com.obsidian.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.File

object GhostMode {
    
    private var isGhost = false
    private var tempDir: File? = null
    
    fun enable(context: Context) {
        if (isGhost) return
        isGhost = true
        
        val filesDir = context.filesDir
        val tempFiles = File(context.cacheDir, ".obsidian_ghost")
        if (!tempFiles.exists()) {
            tempFiles.mkdirs()
        }
        
        try {
            filesDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.copyRecursively(File(tempFiles, file.name))
                    file.deleteRecursively()
                } else if (!file.name.startsWith(".")) {
                    file.copyTo(File(tempFiles, file.name), overwrite = true)
                    file.delete()
                }
            }
        } catch (e: Exception) { }
        
        tempDir = tempFiles
    }
    
    fun disable(context: Context) {
        if (!isGhost) return
        isGhost = false
        
        tempDir?.let { temp ->
            try {
                val filesDir = context.filesDir
                temp.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        file.copyRecursively(File(filesDir, file.name))
                        file.deleteRecursively()
                    } else {
                        file.copyTo(File(filesDir, file.name), overwrite = true)
                        file.delete()
                    }
                }
                temp.delete()
            } catch (e: Exception) { }
        }
        tempDir = null
    }
    
    fun isHidden(): Boolean = isGhost
}
