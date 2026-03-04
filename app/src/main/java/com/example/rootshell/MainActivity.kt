package com.example.rootshell

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rootshell.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("scripts_config", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedScripts()

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val path = binding.etPath.text.toString().trim()
            
            if (name.isBlank()) {
                Toast.makeText(this, "请输入脚本名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (path.isBlank()) {
                Toast.makeText(this, "请输入脚本路径", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if name already exists
            if (prefs.contains(name)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("覆盖确认")
                    .setMessage("脚本 \"$name\" 已存在，是否覆盖？")
                    .setPositiveButton("覆盖") { _, _ ->
                        saveScript(name, path)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                saveScript(name, path)
            }
        }
    }

    private fun saveScript(name: String, path: String) {
        prefs.edit().putString(name, path).apply()
        createScriptButton(name, path)
        binding.etName.text?.clear()
        binding.etPath.text?.clear()
        Toast.makeText(this, "脚本已保存: $name", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedScripts() {
        binding.buttonContainer.removeAllViews()
        prefs.all.forEach { (name, path) ->
            createScriptButton(name, path.toString())
        }
    }

    private fun createScriptButton(name: String, path: String) {
        // Remove existing button with same tag if any
        val existingButton = binding.buttonContainer.findViewWithTag<Button>(name)
        if (existingButton != null) {
            binding.buttonContainer.removeView(existingButton)
        }
        
        val btn = Button(this).apply {
            text = name
            tag = name
            setOnClickListener {
                val intent = Intent(this@MainActivity, TerminalActivity::class.java)
                intent.putExtra("SCRIPT_NAME", name)
                intent.putExtra("SCRIPT_PATH", path)
                startActivity(intent)
            }
            setOnLongClickListener {
                // Long press to delete
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("删除脚本")
                    .setMessage("确定要删除脚本 \"$name\" 吗？")
                    .setPositiveButton("删除") { _, _ ->
                        prefs.edit().remove(name).apply()
                        binding.buttonContainer.removeView(this)
                        Toast.makeText(this@MainActivity, "已删除: $name", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }
        }
        binding.buttonContainer.addView(btn)
    }

    override fun onResume() {
        super.onResume()
        // Reload scripts in case they were modified
        loadSavedScripts()
    }
}
