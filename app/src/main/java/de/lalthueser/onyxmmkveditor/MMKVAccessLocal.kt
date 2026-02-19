package de.lalthueser.onyxmmkveditor

import android.content.Context
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * Minimal non-root MMKV access helper inspired by OnyxTweaks' MMKVAccessService non-root path.
 *
 * - Does NOT create the system MMKV directory if missing; returns null on open.
 * - Provides simple open/close/allKeys/get/put/sync operations for the system store.
 */
class MMKVAccessLocal(private val context: Context) {
    private val mmkvMap: HashMap<String, MMKV> = HashMap()

    companion object {
        const val SYSTEM_HANDLE = "onyx_config"
        const val SYSTEM_PATH = "/onyxconfig/mmkv/"
    }

    fun openSystem(): String? {
        val path = SYSTEM_PATH
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            return null
        }

        try {
            // Initialize MMKV with context and directory root
            MMKV.initialize(context, path)
        } catch (_: Exception) {
        }

        // Open MMKV in that filesystem location
        val kv = MMKV.mmkvWithID("onyx_config", MMKV.MULTI_PROCESS_MODE, null, path)
        mmkvMap[SYSTEM_HANDLE] = kv
        return SYSTEM_HANDLE
    }

    fun close(handle: String) {
        mmkvMap[handle]?.close()
        mmkvMap.remove(handle)
    }

    fun allKeys(handle: String): Array<String> {
        val m = mmkvMap[handle] ?: return arrayOf()
        return m.allKeys() ?: arrayOf()
    }

    fun getString(handle: String, key: String): String? {
        return mmkvMap[handle]?.getString(key, null)
    }

    fun putString(handle: String, key: String, value: String) {
        mmkvMap[handle]?.putString(key, value)
    }

    fun putStringSet(handle: String, key: String, values: List<String>) {
        mmkvMap[handle]?.putStringSet(key, values.toSet())
    }

    fun getStringSet(handle: String, key: String): List<String>? {
        return mmkvMap[handle]?.getStringSet(key, null)?.toList()
    }

    fun putInt(handle: String, key: String, value: Int) { mmkvMap[handle]?.putInt(key, value) }
    fun putLong(handle: String, key: String, value: Long) { mmkvMap[handle]?.putLong(key, value) }
    fun putFloat(handle: String, key: String, value: Float) { mmkvMap[handle]?.putFloat(key, value) }
    fun putBoolean(handle: String, key: String, value: Boolean) { mmkvMap[handle]?.putBoolean(key, value) }

    fun getInt(handle: String, key: String): Int? = mmkvMap[handle]?.getInt(key, 0)
    fun getLong(handle: String, key: String): Long? = mmkvMap[handle]?.getLong(key, 0L)
    fun getFloat(handle: String, key: String): Float? = mmkvMap[handle]?.getFloat(key, 0f)
    fun getBoolean(handle: String, key: String): Boolean? = mmkvMap[handle]?.getBoolean(key, false)

    fun remove(handle: String, key: String) { mmkvMap[handle]?.removeValueForKey(key) }

    fun contains(handle: String, key: String): Boolean = mmkvMap[handle]?.contains(key) ?: false

    fun sync(handle: String) { mmkvMap[handle]?.sync() }

    fun getValueActualSize(handle: String, key: String): Int = mmkvMap[handle]?.getValueActualSize(key) ?: 0
}
