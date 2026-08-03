package com.obsidian.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.obsidian.R
import com.obsidian.profiling.AppProfiler
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
        setStatus("Local-only mode", R.color.phosphor_green)
    }

    private fun setupUI() {
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        scanButton.setOnClickListener { refreshDashboard() }
        injectButton.setOnClickListener { disableAutomation() }
        syncButton.setOnClickListener { showLocalMode() }
        logsButton.setOnClickListener { showLogs() }

        updateDashboard()
    }

    private fun refreshDashboard() {
        setStatus("Refreshing dashboard...", R.color.phosphor_green)
        scope.launch {
            delay(400)
            profiler.loadProfiles()
            updateDashboard()
            setStatus("Dashboard refreshed", R.color.phosphor_green)
        }
    }

    private fun disableAutomation() {
        setStatus("Interactive automation is disabled", android.R.color.holo_orange_light)
        Toast.makeText(
            this,
            "Automation and background control features have been removed.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLocalMode() {
        updateDashboard()
        setStatus("Using local data only", R.color.phosphor_green)
    }

    private fun showLogs() {
        val profiles = profiler.getProfiles()
        val noteCount = profiles.values.sumOf { it.observations.size }
        Toast.makeText(
            this,
            "Saved profiles: ${profiles.size}, notes: $noteCount",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateDashboard() {
        val profiles = profiler.getProfiles()
        val averageScore = if (profiles.isEmpty()) {
            0
        } else {
            (profiles.values.map { it.porosity.toDouble() }.average() * 100).toInt()
        }
        val noteCount = profiles.values.sumOf { it.observations.size }
        val flagCount = profiles.values.sumOf { it.vulnerabilities.size }

        porosityText.text = "Saved Profiles: ${profiles.size}"
        nodesText.text = "Notes Logged: $noteCount"
        chainsText.text = "Review Flags: $flagCount ($averageScore%)"
    }

    private fun setStatus(message: String, colorRes: Int) {
        statusText.text = message
        statusText.setTextColor(ContextCompat.getColor(this, colorRes))
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
