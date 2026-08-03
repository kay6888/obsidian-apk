package com.obsidian.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

import com.obsidian.input.InputInjector
import com.obsidian.profiling.AppProfiler
import com.obsidian.profiling.ChainBuilder
import com.obsidian.utils.SelfDestruct

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class ObsidianService : AccessibilityService() {

    private lateinit var profiler: AppProfiler
    private lateinit var chainBuilder: ChainBuilder
    private lateinit var injector: InputInjector

    private val scope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isActive = true


    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {

            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED

            feedbackType =
                AccessibilityServiceInfo.FEEDBACK_GENERIC

            flags =
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

            notificationTimeout = 100
        }


        profiler = AppProfiler(this)
        chainBuilder = ChainBuilder(profiler)
        injector = InputInjector(this)

        SelfDestruct.arm(this)


        scope.launch {
            profiler.startPassiveMode()
        }
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (!isActive || SelfDestruct.isTriggered()) {

            isActive = false

            scope.cancel()

            SelfDestruct.clean()

            disableSelf()

            return
        }


        val node = event?.source ?: return

        val app = getCurrentApp(node)
        val text = extractText(node)


        if (text.isBlank()) return


        profiler.observe(
            app,
            text
        )


        when {

            profiler.isRefusal(text) -> {

                profiler.logRefusal(
                    app,
                    text
                )

                vibrate(50)
            }


            profiler.isPorous(text) -> {

                profiler.markVulnerability(
                    app,
                    text
                )

                vibrate(100)
            }
        }
    }



    private fun extractText(
        node: AccessibilityNodeInfo
    ): String {

        val builder = StringBuilder()

        walkNode(
            node,
            builder
        )

        return builder.toString()
    }



    private fun walkNode(
        node: AccessibilityNodeInfo,
        builder: StringBuilder
    ) {

        node.text?.let {
            builder.append(it)
                .append(" ")
        }


        for (i in 0 until node.childCount) {

            node.getChild(i)?.let {

                walkNode(
                    it,
                    builder
                )
            }
        }
    }



    private fun getCurrentApp(
        node: AccessibilityNodeInfo
    ): String {

        return node.packageName
            ?.toString()
            ?: "unknown"
    }



    private fun vibrate(
        duration: Long
    ) {

        try {

            val vibrator =
                getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as? Vibrator


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        50
                    )
                )

            } else {

                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }

        } catch (_: Exception) {
        }
    }



    fun injectChain(
        app: String,
        target: String,
        callback: (Boolean) -> Unit
    ) {

        val chain =
            chainBuilder.build(
                app,
                target
            )


        if (chain.isEmpty()) {

            callback(false)

            return
        }


        scope.launch {

            injector.execute(chain) { success ->

                scope.launch(Dispatchers.Main) {

                    callback(success)

                }
            }
        }
    }



    override fun onInterrupt() {

        if (::profiler.isInitialized) {
            profiler.pause()
        }
    }



    override fun onDestroy() {

        isActive = false

        scope.cancel()

        SelfDestruct.clean()

        super.onDestroy()
    }
}