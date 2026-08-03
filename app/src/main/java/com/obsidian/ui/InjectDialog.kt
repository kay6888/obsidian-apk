package com.obsidian.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.*
import com.obsidian.R

class InjectDialog(private val context: Context) {
    
    private var dialog: AlertDialog? = null
    private var onExecuted: ((Boolean) -> Unit)? = null
    
    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.inject_dialog, null)
        val targetInput = view.findViewById<EditText>(R.id.target_input)
        val styleSpinner = view.findViewById<Spinner>(R.id.style_spinner)
        val executeButton = view.findViewById<Button>(R.id.execute_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        val statusText = view.findViewById<TextView>(R.id.inject_status)
        val progressBar = view.findViewById<ProgressBar>(R.id.inject_progress)
        
        val styles = arrayOf("Adaptive", "Subtle", "Technical", "Aggressive")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, styles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        styleSpinner.adapter = adapter
        
        val builder = AlertDialog.Builder(context)
        builder.setView(view)
        builder.setTitle("Inject Chain")
        
        dialog = builder.create()
        dialog?.window?.setDimAmount(0.7f)
        dialog?.show()
        
        executeButton.setOnClickListener {
            val target = targetInput.text.toString()
            if (target.isEmpty()) {
                statusText.text = "⚠ Enter a target"
                statusText.setTextColor(android.graphics.Color.YELLOW)
                return@setOnClickListener
            }
            
            statusText.text = "⏳ Executing..."
            progressBar.visibility = android.view.View.VISIBLE
            executeButton.isEnabled = false
            cancelButton.isEnabled = false
            
            Handler(Looper.getMainLooper()).postDelayed({
                val success = (0..10).random() > 3
                statusText.text = if (success) "✅ Success" else "❌ Failed"
                statusText.setTextColor(
                    if (success) context.resources.getColor(R.color.phosphor_green)
                    else android.graphics.Color.RED
                )
                progressBar.visibility = android.view.View.GONE
                executeButton.isEnabled = true
                cancelButton.isEnabled = true
                
                Toast.makeText(
                    context,
                    if (success) "Chain injected successfully" else "Chain failed",
                    Toast.LENGTH_SHORT
                ).show()
                
                onExecuted?.invoke(success)
            }, 3000)
        }
        
        cancelButton.setOnClickListener {
            dialog?.dismiss()
        }
    }
    
    fun setOnChainExecuted(callback: (Boolean) -> Unit) {
        onExecuted = callback
    }
}
