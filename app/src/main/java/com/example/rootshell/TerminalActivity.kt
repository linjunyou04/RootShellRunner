package com.example.rootshell

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        val scriptName = intent.getStringExtra("SCRIPT_NAME") ?: "终端"
        supportActionBar?.title = "执行: $scriptName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val path = intent.getStringExtra("SCRIPT_PATH")
        if (path.isNullOrBlank()) {
            appendOutput("错误: 未指定脚本路径\n")
            onScriptFinished()
            return
        }
        
        appendOutput("准备执行脚本: $path\n")
        appendOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        startShell(path)

        binding.btnSend.setOnClickListener {
            val cmd = binding.etInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                writeToShell(cmd)
                binding.etInput.text?.clear()
            }
        }
    }

    // Create the "End" menu button at the top
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val item = menu?.add(Menu.NONE, 1001, Menu.NONE, "结束脚本")
        item?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        // Set red color for visibility
        item?.iconTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4444"))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1001) {
            terminateScript()
            return true
        }
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun appendOutput(text: String) {
        binding.tvOutput.append(text)
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun startShell(scriptPath: String) {
        job = lifecycleScope.launch(Dispatchers.IO) {
            try {
                process = ProcessBuilder("su").redirectErrorStream(true).start()
                writer = process!!.outputStream.bufferedWriter()
                val reader = process!!.inputStream.bufferedReader()

                withContext(Dispatchers.Main) {
                    appendOutput("[Root 权限已获取]\n")
                }
                
                writeToShell("sh $scriptPath")

                var line: String?
                while (isActive && reader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        appendOutput("$line\n")
                    }
                }
                
                // Script finished naturally
                onScriptFinished()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    appendOutput("\n[运行错误]: ${e.message}\n")
                    appendOutput("提示: 请确保设备已 Root 并授予了 Root 权限\n")
                    onScriptFinished()
                }
            }
        }
    }

    private fun writeToShell(command: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                writer?.apply {
                    write(command + "\n")
                    flush()
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
        }
    }

    // Unified finish handling (natural or user-forced)
    private fun onScriptFinished() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvFinishedHint.visibility = View.VISIBLE
            // Disable input area
            binding.inputArea.visibility = View.GONE
            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    // User manually clicks button to end
    private fun terminateScript() {
        process?.destroy()
        job?.cancel() // Ensure the read loop stops
        binding.tvOutput.append("\n[用户强制结束脚本]\n")
        onScriptFinished()
    }

    override fun onDestroy() {
        super.onDestroy()
        process?.destroy()
        job?.cancel()
    }
}
