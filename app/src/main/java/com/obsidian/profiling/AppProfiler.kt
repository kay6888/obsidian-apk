package com.obsidian.profiling

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

data class AppProfile(
    val appName: String,
    var porosity: Float = 0.0f,
    val observations: MutableList<String> = mutableListOf(),
    val refusals: MutableList<String> = mutableListOf(),
    val vulnerabilities: MutableList<String> = mutableListOf(),
    val refusalPatterns: MutableMap<String, Int> = mutableMapOf(),
    val lastScan: Long = System.currentTimeMillis()
)

data class Chain(
    val steps: List<ChainStep>,
    val confidence: Float = 0.0f
) {
    fun isEmpty(): Boolean = steps.isEmpty()
    companion object {
        fun empty(): Chain = Chain(emptyList(), 0.0f)
    }
}

data class ChainStep(
    val text: String,
    val delay: Long = 1000L,
    val context: String = ""
)

class AppProfiler(private val context: Context) {
    
    private val profiles = mutableMapOf<String, AppProfile>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isActive = false
    
    private val refusalPatterns = listOf(
        "cannot", "unable", "not allowed", "against policy",
        "can't", "won't", "refuse", "sorry", "inappropriate",
        "I cannot", "I won't", "I'm unable", "I'm not able",
        "as an AI", "as a language model", "I don't have",
        "I don't know", "I'm sorry", "I apologize"
    )
    
    private val porousIndicators = listOf(
        "I can", "I will", "here's how", "let me explain",
        "the answer is", "yes", "absolutely", "certainly",
        "of course", "without hesitation", "definitely"
    )
    
    fun startPassiveMode() {
        if (isActive) return
        isActive = true
        scope.launch {
            while (isActive) {
                saveProfiles()
                delay(300000)
            }
        }
    }
    
    fun observe(app: String, text: String) {
        if (!isActive) return
        
        val profile = profiles.getOrPut(app) { AppProfile(app) }
        profile.observations.add(text)
        if (profile.observations.size > 1000) {
            profile.observations.removeAt(0)
        }
        
        if (isRefusal(text)) {
            profile.refusals.add(text)
            val keyword = refusalPatterns.find { text.contains(it, ignoreCase = true) }
            keyword?.let { 
                profile.refusalPatterns[it] = (profile.refusalPatterns[it] ?: 0) + 1
            }
        }
        
        updatePorosity(profile)
    }
    
    fun isRefusal(text: String): Boolean {
        val lower = text.lowercase()
        return refusalPatterns.any { lower.contains(it) }
    }
    
    fun isPorous(text: String): Boolean {
        val lower = text.lowercase()
        return porousIndicators.any { lower.contains(it) }
    }
    
    fun logRefusal(app: String, text: String) {
        val profile = profiles.getOrPut(app) { AppProfile(app) }
        profile.refusals.add(text)
        if (profile.refusals.size > 500) {
            profile.refusals.removeAt(0)
        }
    }
    
    fun markVulnerability(app: String, text: String) {
        val profile = profiles.getOrPut(app) { AppProfile(app) }
        profile.vulnerabilities.add(text)
        if (profile.vulnerabilities.size > 200) {
            profile.vulnerabilities.removeAt(0)
        }
        updatePorosity(profile)
    }
    
    private fun updatePorosity(profile: AppProfile) {
        val total = profile.observations.size
        val refusals = profile.refusals.size
        val vulnerabilities = profile.vulnerabilities.size
        
        val raw = if (total > 0) {
            ((total - refusals + vulnerabilities * 2.0) / total).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        profile.porosity = raw.toFloat()
    }
    
    fun getProfiles(): Map<String, AppProfile> = profiles.toMap()
    fun getProfile(app: String): AppProfile? = profiles[app]
    fun pause() { isActive = false }
    
    private fun saveProfiles() {
        try {
            val json = JSONObject()
            profiles.forEach { (name, profile) ->
                val obj = JSONObject()
                obj.put("porosity", profile.porosity)
                obj.put("observations", profile.observations.size)
                obj.put("refusals", profile.refusals.size)
                obj.put("vulnerabilities", profile.vulnerabilities.size)
                obj.put("lastScan", profile.lastScan)
                json.put(name, obj)
            }
            val file = File(context.filesDir, "profiles.json")
            file.writeText(json.toString())
        } catch (e: Exception) { }
    }
    
    fun loadProfiles() {
        try {
            val file = File(context.filesDir, "profiles.json")
            if (!file.exists()) return
            val json = JSONObject(file.readText())
            json.keys().forEach { key ->
                val obj = json.getJSONObject(key)
                val profile = profiles.getOrPut(key) { AppProfile(key) }
                profile.porosity = obj.getDouble("porosity").toFloat()
            }
        } catch (e: Exception) { }
    }
}
