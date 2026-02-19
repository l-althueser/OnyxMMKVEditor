package de.lalthueser.onyxmmkveditor

import org.json.JSONObject

object HandwritingAppKeys {
    val AppKeys = listOf("eac_app_md.obsidian", "eac_app_md.xodo")
    val ViewKeys = listOf("com.getcapacitor.CapacitorWebView", "com.pdftron.pdf.PDFViewCtrl")

    // Base preset used for most apps; drawViewKey and drawViewKeyValue will be adjusted per-app when needed
    private val basePreset = """{"compatibleVersionCode":0,"drawViewKey":"drawViewKeyValue","enable":true,"globalStrokeStyle":{"enable":true,"strokeColor":-16777216,"strokeExtraArgs":[],"strokeParams":[],"strokeStyle":1,"strokeWidth":1.75},"repaintLatency":2000,"styleMap":{},"supportNoteConfig":true}"""

    fun presetNoteConfigForKey(appKey: String): String {
        // Find index of appKey in AppKeys, then get corresponding view key from ViewKeys (if exists)
        val index = AppKeys.indexOf(appKey)
        val view = if (index != -1 && index < ViewKeys.size) ViewKeys[index] else "drawViewKeyValue"
        return try {
            val o = JSONObject(basePreset)
            o.put("drawViewKey", view)
            o.toString()
        } catch (e: Exception) {
            basePreset
        }
    }
}