package com.example.rootshell

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rootshell.databinding.ActivityTerminalBinding
import kotlinx.coroutines.*
import java.io.*

class TerminalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTerminalBinding
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var outputJob: Job? = null
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scriptName = intent.getStringExtra("SCRIPT_NAME") ?: "未知脚本"
        val scriptPath = intent.getStringExtra("SCRIPT_PATH")
        
        title = "终端 - $scriptName"
        
        if (scriptPath.isNullOrBlank()) {
            appendOutput("错误: 未指定脚本路径\n")
            return
        }
        
        appendOutput("准备执行脚本: $scriptPath\n")
        appendOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        
        startShell(scriptPath)

        binding.btnSend.setOnClickListener {
            val cmd = binding.etInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                writeToShell(cmd)
                binding.etInput.text?.clear()
            }
        }
        
        // Handle Enter key
        binding.etInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val cmd = binding.etInput.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    writeToShell(cmd)
                    binding.etInput.text?.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun appendOutput(text: String) {
        binding.tvOutput.append(text)
        // Scroll to bottom
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun startShell(scriptPath: String) {
        if (isRunning) {
            appendOutput("Shell 已在运行中\n")
            return
        }
        
        isRunning = true
        outputJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Start su process
                process = Runtime.getRuntime().exec("su")
                writer = process!!.outputStream.bufferedWriter()
                val reader = process!!.inputStream.bufferedReader()
                val errorReader = process!!.errorStream.bufferedReader()

                // Execute initial script
                appendOutput("正在获取 Root 权限...\n")
                writeToShellInternal("sh $scriptPath")
                appendOutput("脚本已提交执行\n")
                appendOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

                // Read stdout
                val outputScope = CoroutineScope(Dispatchers.IO)
                
                val stdoutJob = outputScope.launch {
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            withContext(Dispatchers.Main) {
                                appendOutput("$line\n")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Read stderr
                val stderrJob = outputScope.launch {
                    try {
                        while (isActive) {
                            val line = errorReader.readLine() ?: break
                            withContext(Dispatchers.Main) {
                                appendOutput("[错误] $line\n")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Wait for process
                stdoutJob.join()
                stderrJob.join()
                
                withContext(Dispatchers.Main) {
                    appendOutput("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                    appendOutput("脚本执行完成\n")
                    isRunning = false
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    appendOutput("\n错误: ${e.message}\n")
                    appendOutput("提示: 请确保设备已 Root 并授予了 Root 权限\n")
                    isRunning = false
                }
            }
        }
    }

    private fun writeToShell(command: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            writeToShellInternal(command)
        }
    }

    private fun writeToShellInternal(command: String) {
        try {
            writer?.apply {
                write(command + "\n")
                flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        outputJob?.cancel()
        try {
            writer?.close()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
