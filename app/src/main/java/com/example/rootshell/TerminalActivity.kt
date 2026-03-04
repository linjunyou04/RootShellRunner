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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scriptPath = intent.getStringExtra("PATH") ?: ""
        val scriptName = intent.getStringExtra("NAME") ?: "终端"
        
        // 设置标题
        binding.tvTerminalTitle.text = "终端 - $scriptName"
        
        // 启动 Root Shell 并执行脚本
        startRootProcess(scriptPath)

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
    }

    private fun startRootProcess(initialScript: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                process = ProcessBuilder("su").redirectErrorStream(true).start()
                writer = process!!.outputStream.bufferedWriter()
                val reader = process!!.inputStream.bufferedReader()

                withContext(Dispatchers.Main) {
                    appendOutput("[System]: Root 权限已获取", "#4CAF50")
                }

                // 执行初始脚本
                writer?.write("sh $initialScript\n")
                writer?.flush()

                // 读取输出
                try {
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        withContext(Dispatchers.Main) {
                            appendOutput(line)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 脚本运行结束
                withContext(Dispatchers.Main) {
                    appendOutput("\n[System]: 脚本执行完毕", "#4CAF50")
                    binding.tvFinishHint.visibility = View.VISIBLE
                    binding.layoutInputBox.alpha = 0.5f // 变灰表示不可用
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    appendOutput("\n[Error]: ${e.message}", "#FF5252")
                    appendOutput("提示: 请确保设备已 Root 并授予了 Root 权限", "#FF9800")
                }
            }
        }
    }

    private fun sendCommandToShell(command: String) {
        // 在终端回显用户输入的内容
        appendOutput("\n$ $command", "#00FF00") // 用绿色显示输入行

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
        binding.tvOutput.append(text + "\n")
        // 自动滚动到底部
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
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

    override fun onDestroy() {
        super.onDestroy()
        process?.destroy()
    }
}
