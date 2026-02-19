package de.lalthueser.onyxmmkveditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.tencent.mmkv.MMKV
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: KeyValueAdapter
    private val mmkvService: MMKVAccessLocal by lazy { MMKVAccessLocal(this) }
    private var currentHandle: String? = null
    private var allItems: List<Pair<String, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensure storage permission / all-files access on Android 11+
        ensureAllFilesAccessIfNeeded()

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = KeyValueAdapter(emptyList()) { k, v -> showEditDialog(k, v) }
        rv.adapter = adapter

        val etFilter = findViewById<EditText>(R.id.etFilter)
        etFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val swShowAll = findViewById<SwitchMaterial>(R.id.swShowAll)
        swShowAll.setOnCheckedChangeListener { _, _ ->
            applyFilter()
        }

        // Try to open the system mmkv store (non-root)
        try {
            val handle = mmkvService.openSystem()
            if (handle == null) {
                Toast.makeText(this, "MMKV directory not found: ${MMKVAccessLocal.SYSTEM_PATH}\nEnsure the path exists and the app has the required permissions.", Toast.LENGTH_LONG).show()
            } else {
                currentHandle = handle
            }
        } catch (e: Exception) {
            Toast.makeText(this, "MMKV initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        loadAll()
    }

    private fun loadAll() {
        val kvList = mutableListOf<Pair<String, String>>()
        try {
            val handle = currentHandle
            if (handle == null) {
                Toast.makeText(this, "Cannot access MMKV system store", Toast.LENGTH_LONG).show()
                allItems = emptyList()
                adapter.update(allItems)
                return
            }
            val keys = mmkvService.allKeys(handle)
            for (k in keys) {
                val v = mmkvService.getString(handle, k) ?: ""
                kvList.add(k to v)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading MMKV: ${e.message}", Toast.LENGTH_LONG).show()
        }
        allItems = kvList
        applyFilter()
    }

    private fun applyFilter() {
        val query = findViewById<EditText>(R.id.etFilter).text.toString()
        val showAll = findViewById<SwitchMaterial>(R.id.swShowAll).isChecked

        var filtered = allItems

        // First filter by "eac_" if showAll is false
        if (!showAll) {
            filtered = filtered.filter { it.first.startsWith("eac_", ignoreCase = true) }
        }

        // Then filter by text query if present
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.first.contains(query, ignoreCase = true) }
        }

        adapter.update(filtered)
    }

    private fun getBackupFile(key: String): File {
        val backupDir = File(filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        // Use base64 to avoid invalid filename characters
        val fileName = Base64.encodeToString(key.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        return File(backupDir, "$fileName.txt")
    }

    private fun saveBackupIfMissing(key: String, value: String) {
        val file = getBackupFile(key)
        if (!file.exists()) {
            try {
                file.writeText(value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showEditDialog(key: String, value: String) {
        // Save original value as backup if it's the first time
        saveBackupIfMissing(key, value)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = (16 * resources.displayMetrics.density).toInt()
        lp.setMargins(margin, 8, margin, 8)

        val buttonContainer = LinearLayout(this)
        buttonContainer.orientation = LinearLayout.HORIZONTAL
        buttonContainer.layoutParams = lp

        val edit = EditText(this)
        edit.setText(value)
        edit.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        edit.layoutParams = lp

        val switchFormat = SwitchMaterial(this)
        switchFormat.text = "Format JSON"
        switchFormat.layoutParams = lp
        switchFormat.setOnCheckedChangeListener { _, isChecked ->
            try {
                val currentText = edit.text.toString().trim()
                if (isChecked) {
                    edit.setText(JSONObject(currentText).toString(4))
                } else {
                    edit.setText(JSONObject(currentText).toString())
                }
            } catch (e: Exception) {
                if (isChecked) {
                    Toast.makeText(this, "Invalid JSON object structure", Toast.LENGTH_SHORT).show()
                    switchFormat.isChecked = false
                }
            }
        }

        if (HandwritingAppKeys.AppKeys.contains(key)) {
            val btnPreset = Button(this)
            btnPreset.text = "Optimize Handwriting"
            btnPreset.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            btnPreset.setOnClickListener {
                try {
                    val currentText = edit.text.toString()
                    val json = if (currentText.isBlank()) JSONObject() else JSONObject(currentText)
                    val globalConfig = json.optJSONObject("globalActivityConfig") ?: JSONObject()
                    val presetJson = JSONObject(HandwritingAppKeys.presetNoteConfigForKey(key))
                    globalConfig.put("noteConfig", presetJson)
                    json.put("globalActivityConfig", globalConfig)
                    
                    if (switchFormat.isChecked) {
                        edit.setText(json.toString(4))
                    } else {
                        edit.setText(json.toString())
                    }
                    Toast.makeText(this, "Handwriting optimization settings added", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "JSON Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            buttonContainer.addView(btnPreset)
        }

        val backupFile = getBackupFile(key)
        if (backupFile.exists()) {
            val btnRestore = Button(this)
            btnRestore.text = "Restore Backup"
            btnRestore.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            btnRestore.setOnClickListener {
                try {
                    val original = backupFile.readText()
                    if (switchFormat.isChecked) {
                        try {
                            edit.setText(JSONObject(original).toString(4))
                        } catch (e: Exception) {
                            edit.setText(original)
                            switchFormat.isChecked = false
                        }
                    } else {
                        edit.setText(original)
                    }
                    Toast.makeText(this, "Backup restored to editor", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Restore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            buttonContainer.addView(btnRestore)
        }

        if (buttonContainer.childCount > 0) {
            container.addView(buttonContainer)
        }

        container.addView(switchFormat)
        container.addView(edit)

        val dlg = AlertDialog.Builder(this)
            .setTitle(key)
            .setView(container)
            .setPositiveButton("Save", null) // Handle manually to validate
            .setNeutralButton("Copy") { _, _ ->
                var textToCopy = edit.text.toString().trim()
                val isFormatted = switchFormat.isChecked
                
                if (!isFormatted) {
                    try {
                        textToCopy = JSONObject(textToCopy).toString()
                    } catch (e: Exception) {}
                }
                
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(key, textToCopy)
                clipboard.setPrimaryClip(clip)
                
                val toastMsg = if (isFormatted) "Copied to clipboard (formatted)" else "Copied to clipboard (minimized)"
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dlg.show()

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val input = edit.text.toString().trim()
            var finalValue = input
            var isValidJsonObject = false

            try {
                if (input.startsWith("{")) {
                    finalValue = JSONObject(input).toString()
                    isValidJsonObject = true
                }
            } catch (e: Exception) {
                // Not a valid JSON object
            }

            // Enforce that the entry MUST be a valid JSON object starting with {
            if (!isValidJsonObject) {
                Toast.makeText(this, "Error: The entry must be a valid JSON object starting with '{'.", Toast.LENGTH_LONG).show()
            } else {
                currentHandle?.let { h ->
                    mmkvService.putString(h, key, finalValue)
                    mmkvService.sync(h)
                }
                loadAll()
                dlg.dismiss()
                
                // Show success message
                AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Your changes have been saved to the MMKV store.\n\nIMPORTANT: You must RESTART your device now for the changes to take effect.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun ensureAllFilesAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Ask the user to grant All files access for this app
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback: open generic settings
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}
