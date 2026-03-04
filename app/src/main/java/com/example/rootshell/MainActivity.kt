package com.example.rootshell

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.rootshell.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
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

        // 串联测试按钮
        binding.btnTestChain.setOnClickListener {
            val savedScripts = prefs.all.keys.toTypedArray()
            if (savedScripts.isEmpty()) {
                Toast.makeText(this, "请先添加脚本", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedItems = BooleanArray(savedScripts.size)
            
            AlertDialog.Builder(this)
                .setTitle("选择要串联的脚本 (按勾选顺序)")
                .setMultiChoiceItems(savedScripts, selectedItems) { _, which, isChecked ->
                    selectedItems[which] = isChecked
                }
                .setPositiveButton("执行") { _, _ ->
                    val pathsToRun = ArrayList<String>()
                    savedScripts.forEachIndexed { index, name ->
                        if (selectedItems[index]) {
                            val path = prefs.getString(name, "") ?: ""
                            if (path.isNotEmpty()) {
                                pathsToRun.add(path)
                            }
                        }
                    }
                    if (pathsToRun.isNotEmpty()) {
                        val intent = Intent(this@MainActivity, TerminalActivity::class.java)
                        intent.putStringArrayListExtra("CHAIN_PATHS", pathsToRun)
                        intent.putExtra("SCRIPT_NAME", "串联测试模式")
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "请至少选择一个脚本", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun saveScript(name: String, path: String) {
        prefs.edit().putString(name, path).apply()
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
        val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle).apply {
            text = name
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
            isAllCaps = false
            
            setOnClickListener {
                val intent = Intent(this@MainActivity, TerminalActivity::class.java)
                intent.putExtra("SCRIPT_PATH", path)
                intent.putExtra("SCRIPT_NAME", name)
                startActivity(intent)
            }
            
            setOnLongClickListener {
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
        binding.buttonContainer.removeAllViews()
        loadSavedScripts()
    }
}
