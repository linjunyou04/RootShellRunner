package com.example.rootshell

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rootshell.databinding.ActivityTerminalBinding
import kotlinx.coroutines.*
import java.io.*

class TerminalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTerminalBinding
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    
    // 宏录制相关变量
    private var isRecording = false
    private var lastInputTime = 0L
    private val currentMacro = mutableListOf<Pair<Long, String>>()
    private val macroPrefs by lazy { getSharedPreferences("macro_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scriptPath = intent.getStringExtra("SCRIPT_PATH") ?: ""
        val scriptName = intent.getStringExtra("SCRIPT_NAME") ?: "终端"
        val chainPaths = intent.getStringArrayListExtra("CHAIN_PATHS")
        
        // 设置标题
        binding.tvTerminalTitle.text = "终端 - $scriptName"
        
        // 发送按钮逻辑
        binding.btnSend.setOnClickListener {
            val cmd = binding.etInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                sendCommandToShell(cmd)
                binding.etInput.text.clear()
            }
        }

        // 结束按钮逻辑
        binding.btnTerminate.setOnClickListener {
            killProcess()
        }

        // 录制按钮逻辑
        binding.btnRecord.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                binding.btnRecord.text = "⏹ 停止"
                currentMacro.clear()
                lastInputTime = System.currentTimeMillis()
                appendOutput("\n[Macro]: 开始录制您的输入和频率...", "#FFDD00")
            } else {
                binding.btnRecord.text = "⏺ 录制"
                saveMacroToPrefs(currentMacro)
                appendOutput("\n[Macro]: 录制结束，已保存 ${currentMacro.size} 条记录。", "#FFDD00")
            }
        }

        // 回放按钮逻辑
        binding.btnReplay.setOnClickListener {
            val savedMacro = loadMacroFromPrefs()
            if (savedMacro.isEmpty()) {
                appendOutput("\n[Macro]: 没有找到保存的录制记录。", "#FF5252")
                return@setOnClickListener
            }
            
            appendOutput("\n[Macro]: 开始自动回放 ${savedMacro.size} 条记录...", "#FFDD00")
            lifecycleScope.launch(Dispatchers.IO) {
                savedMacro.forEach { action ->
                    delay(action.first)
                    withContext(Dispatchers.Main) {
                        sendCommandToShell(action.second)
                    }
                }
                withContext(Dispatchers.Main) { 
                    appendOutput("\n[Macro]: 回放完毕。", "#FFDD00") 
                }
            }
        }

        // 启动 Root Shell 并执行脚本
        startRootProcess(scriptPath, chainPaths)
    }

    private fun startRootProcess(scriptPath: String, chainPaths: ArrayList<String>?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                process = ProcessBuilder("su").redirectErrorStream(true).start()
                writer = process?.outputStream?.bufferedWriter()
                val reader = process?.inputStream?.bufferedReader()

                withContext(Dispatchers.Main) {
                    appendOutput("[System]: Root 权限已获取", "#4CAF50")
                }

                // 处理串联脚本或单脚本
                if (chainPaths != null && chainPaths.isNotEmpty()) {
                    withContext(Dispatchers.Main) { 
                        appendOutput("\n[System]: 正在串联执行 ${chainPaths.size} 个脚本...", "#00FFFF") 
                    }
                    chainPaths.forEachIndexed { index, path ->
                        withContext(Dispatchers.Main) {
                            appendOutput("[System]: 执行脚本 ${index + 1}/${chainPaths.size}: $path", "#00FFFF")
                        }
                        writer?.write("sh $path\n")
                        writer?.flush()
                    }
                } else if (scriptPath.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        appendOutput("[System]: 准备执行脚本: $scriptPath", "#4CAF50")
                    }
                    writer?.write("sh $scriptPath\n")
                    writer?.flush()
                } else {
                    withContext(Dispatchers.Main) {
                        appendOutput("[System]: 未指定脚本，进入交互模式", "#FF9800")
                    }
                }

                // 读取输出
                try {
                    while (isActive) {
                        val line = reader?.readLine() ?: break
                        withContext(Dispatchers.Main) {
                            appendOutput(line)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                withContext(Dispatchers.Main) {
                    appendOutput("\n[System]: 脚本执行完毕", "#4CAF50")
                    binding.tvFinishHint.visibility = View.VISIBLE
                    binding.layoutInputBox.alpha = 0.5f
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    appendOutput("\n[Error]: ${e.message}", "#FF5252")
                    appendOutput("提示: 请确保设备已 Root 并授予了 Root 权限", "#FF9800")
                    binding.tvFinishHint.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun sendCommandToShell(command: String) {
        // 在终端回显用户输入的内容
        appendOutput("\n$ $command", "#00FF00")
        
        // 如果正在录制，记录时间差
        if (isRecording) {
            val now = System.currentTimeMillis()
            val timeDiff = now - lastInputTime
            currentMacro.add(Pair(timeDiff, command))
            lastInputTime = now
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                writer?.write("$command\n")
                writer?.flush()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    appendOutput("写入失败: ${e.message}", "#FF5252")
                }
            }
        }
    }

    private fun appendOutput(text: String, colorHex: String = "#FFFFFF") {
        try {
            binding.tvOutput.append(text + "\n")
            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun killProcess() {
        lifecycleScope.launch(Dispatchers.IO) {
            process?.destroy()
            withContext(Dispatchers.Main) {
                appendOutput("\n[System]: 脚本已被手动强制结束", "#FF5252")
                binding.tvFinishHint.visibility = View.VISIBLE
                binding.layoutInputBox.alpha = 0.5f
            }
        }
    }

    // 宏数据的持久化
    private fun saveMacroToPrefs(macro: List<Pair<Long, String>>) {
        val dataString = macro.joinToString(",") { "${it.first}|${it.second}" }
        macroPrefs.edit().putString("saved_macro", dataString).apply()
    }

    private fun loadMacroFromPrefs(): List<Pair<Long, String>> {
        val dataString = macroPrefs.getString("saved_macro", "") ?: ""
        if (dataString.isEmpty()) return emptyList()
        
        return dataString.split(",").mapNotNull {
            val parts = it.split("|")
            if (parts.size == 2) {
                Pair(parts[0].toLongOrNull() ?: 1000L, parts[1])
            } else null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        process?.destroy()
    }
}
