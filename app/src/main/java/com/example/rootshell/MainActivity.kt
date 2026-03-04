package com.example.rootshell

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rootshell.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("scripts_v2", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedScripts()

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val path = binding.etPath.text.toString().trim()
            
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入脚本名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (path.isEmpty()) {
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

        // GitHub link click
        binding.tvGithubLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(binding.tvGithubLink.text.toString()))
            startActivity(intent)
        }
    }

    private fun saveScript(name: String, path: String) {
        prefs.edit().putString(name, path).apply()
        // Clear and reload to avoid duplicates
        binding.buttonContainer.removeAllViews()
        loadSavedScripts()
        binding.etName.text?.clear()
        binding.etPath.text?.clear()
        Toast.makeText(this, "脚本已保存: $name", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedScripts() {
        prefs.all.forEach { (name, path) ->
            createScriptCardButton(name, path.toString())
        }
    }

    private fun createScriptCardButton(name: String, path: String) {
        // Use Material Tonal Button for a modern flat look
        val btn = Button(this, null, com.google.android.material.R.attr.materialButtonTonalStyle).apply {
            text = name
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
            isAllCaps = false // Disable all caps
            
            setOnClickListener {
                val intent = Intent(this@MainActivity, TerminalActivity::class.java)
                intent.putExtra("SCRIPT_PATH", path)
                intent.putExtra("SCRIPT_NAME", name)
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
        binding.buttonContainer.removeAllViews()
        loadSavedScripts()
    }
}
