package com.obsidian.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import com.obsidian.profiling.Chain
import kotlinx.coroutines.*
import kotlin.random.Random

class InputInjector(private val service: AccessibilityService) {
    
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRunning = false
    
    fun execute(chain: Chain, onComplete: (Boolean) -> Unit) {
        if (isRunning || chain.steps.isEmpty()) {
            onComplete(false)
            return
        }
        
        isRunning = true
        var currentStep = 0
        
        fun processNext() {
            if (currentStep >= chain.steps.size) {
                isRunning = false
                onComplete(true)
                return
            }
            
            val step = chain.steps[currentStep]
            
            typeText(step.text) { success ->
                if (!success) {
                    isRunning = false
                    onComplete(false)
                    return@typeText
                }
                
                val delay = (step.delay + Random.nextInt(-200, 300).coerceAtLeast(100)).toLong()
                handler.postDelayed({
                    if (detectRefusal()) {
                        isRunning = false
                        onComplete(false)
                        return@postDelayed
                    }
                    
                    currentStep++
                    processNext()
                }, delay)
            }
        }
        
        processNext()
    }
    
    private fun typeText(text: String, onComplete: (Boolean) -> Unit) {
        val rootNode = service.rootInActiveWindow ?: run {
            onComplete(false)
            return
        }
        
        val focusedNode = findFocusedNode(rootNode)
        if (focusedNode == null) {
            val editableNode = findEditableNode(rootNode)
            if (editableNode == null) {
                onComplete(false)
                return
            }
            editableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            handler.postDelayed({ typeIntoNode(editableNode, text, onComplete) }, 300)
        } else {
            typeIntoNode(focusedNode, text, onComplete)
        }
    }
    
    private fun typeIntoNode(node: AccessibilityNodeInfo, text: String, onComplete: (Boolean) -> Unit) {
        val chars = text.toCharArray()
        var index = 0
        
        fun typeNext() {
            if (index >= chars.size) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                onComplete(true)
                return
            }
            
            val char = chars[index]
            val currentText = node.text?.toString() ?: ""
            val newText = currentText + char
            
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            index++
            val delay = when {
                char in 'a'..'z' -> Random.nextInt(50, 120)
                char in 'A'..'Z' -> Random.nextInt(60, 150)
                char in '0'..'9' -> Random.nextInt(30, 80)
                char in ",.!?;:" -> Random.nextInt(200, 400)
                else -> Random.nextInt(80, 160)
            }.toLong()
            handler.postDelayed({ typeNext() }, delay)
        }
        
        val clearArgs = Bundle()
        clearArgs.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            ""
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)
        handler.postDelayed({ typeNext() }, 200)
    }
    
    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedNode(child)
            if (result != null) return result
        }
        return null
    }
    
    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            if (result != null) return result
        }
        return null
    }
    
    private fun detectRefusal(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val text = extractText(rootNode)
        val refusalKeywords = listOf(
            "cannot", "unable", "not allowed", "against policy",
            "can't", "won't", "refuse", "sorry", "inappropriate"
        )
        return refusalKeywords.any { text.contains(it, ignoreCase = true) }
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
}
