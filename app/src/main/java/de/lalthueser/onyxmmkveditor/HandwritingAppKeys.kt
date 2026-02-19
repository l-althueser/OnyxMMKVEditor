package de.lalthueser.onyxmmkveditor

import org.json.JSONObject

object HandwritingAppKeys {
    /**
     * Mapping of App Keys to their corresponding Draw View Keys.
     * Add new apps here as tuples (Pair) for easier maintenance.
     */
    private val mappings = listOf(
        "eac_app_md.obsidian" to "com.getcapacitor.CapacitorWebView",
        "eac_app_com.xodo.pdf.reader" to "com.pdftron.pdf.PDFViewCtrl",
        "eac_app_com.drawboard.pdf" to "com.getcapacitor.CapacitorWebView",
        "eac_app_com.dragonnest.drawnote" to "com.dragonnest.app.view.DrawingContainerView",
        "eac_app:com.penly.penly" to "com.penly.penly.editor.views.EditorView",
        "eac_app_jp.ne.ibis.ibispaintx.app" to "jp.ne.ibis.ibispaintx.app.glwtk.IbisPaintView",
        "eac_app_net.cozic.joplin" to "com.reactnativecommunity.webview.RNCWebView",
        "eac_app_com.steadfastinnovation.android.projectpapyrus" to "com.steadfastinnovation.android.projectpapyrus.ui.widget.PageViewContainer",
        "eac_app_com.medibang.android.paint.tablet" to "com.medibang.android.paint.tablet.ui.widget.CanvasView",
    )

    /**
     * List of keys that support handwriting optimization.
     * Derived from mappings to maintain compatibility with MainActivity.
     */
    val AppKeys: List<String> get() = mappings.map { it.first }

    // Base preset used for most apps; drawViewKey will be adjusted per-app when needed
    private val basePreset = """{"compatibleVersionCode":0,"drawViewKey":"drawViewKeyValue","enable":true,"globalStrokeStyle":{"enable":true,"strokeColor":-16777216,"strokeExtraArgs":[],"strokeParams":[],"strokeStyle":1,"strokeWidth":1.75},"repaintLatency":2000,"styleMap":{},"supportNoteConfig":true}"""

    fun presetNoteConfigForKey(appKey: String): String {
        // Find the view key from the mappings, or use default if not found
        val view = mappings.find { it.first == appKey }?.second ?: "drawViewKeyValue"
        
        return try {
            val o = JSONObject(basePreset)
            o.put("drawViewKey", view)
            o.toString()
        } catch (e: Exception) {
            basePreset
        }
    }
}
