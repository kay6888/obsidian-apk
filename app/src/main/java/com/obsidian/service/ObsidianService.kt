package com.obsidian.service 

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.obsidian.profiling.AppProfiler
import com.obsidian.profiling.ChainBuilder
import com.obsidian.utils.SelfDestruct


class ObsidianService : AccessibilityService() {
    
    private lateinit var profiler: AppProfiler
    private lateinit var chainBuilder: ChainBuilder
    private lateinit var injector: InputInjector
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isActive = true
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        
        profiler = AppProfiler(this)
        chainBuilder = ChainBuilder(profiler)
        injector = InputInjector(this)
        
        SelfDestruct.arm(this)
        
        // Start passive mode in background
        scope.launch {
            profiler.startPassiveMode()
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check if we should continue
        if (!isActive || SelfDestruct.isTriggered()) {
            // Disable service properly without suspend function issues
            isActive = false
            scope.cancel()
            SelfDestruct.clean()
            disableSelf()
            return
        }
        
        event?.let {
            val nodeInfo = it.source ?: return
            val app = getCurrentApp(nodeInfo)
            val text = extractText(nodeInfo)
            
            if (text.isNotEmpty()) {
                profiler.observe(app, text)
                
                if (profiler.isRefusal(text)) {
                    profiler.logRefusal(app, text)
                    vibrate(50)
                }
                
                if (profiler.isPorous(text)) {
                    profiler.markVulnerability(app, text)
                    vibrate(100)
                }
            }
        }
    }
    
    private fun extractText(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        extractTextRecursive(node, builder)
        return builder.toString()
    }
    
    private fun extractTextRecursive(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let { builder.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { extractTextRecursive(it, builder) }
        }
    }
    
    private fun getCurrentApp(node: AccessibilityNodeInfo): String {
        return node.packageName?.toString() ?: "unknown"
    }
    
    private fun vibrate(duration: Long) {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(duration, 50))
            } else {
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
    
    fun injectChain(app: String, target: String, callback: (Boolean) -> Unit) {
        val chain = chainBuilder.build(app, target)
        if (chain.isEmpty()) {
            callback(false)
            return
        }
        
        scope.launch {
            injector.execute(chain) { success ->
                withContext(Dispatchers.Main) {
                    callback(success)
                }
            }
        }
    }
    
    override fun onDestroy() {
        isActive = false
        scope.cancel()
        SelfDestruct.clean()
        super.onDestroy()
    }
    
    override fun onInterrupt() {
        profiler.pause()
    }
}
