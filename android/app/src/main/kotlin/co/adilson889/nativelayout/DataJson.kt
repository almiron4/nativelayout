/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout

import org.json.JSONArray
import org.json.JSONObject

object DataJson {

    fun parse(text: String): Map<String, Any?> {
        if (text.isBlank()) return emptyMap()
        return toMap(JSONObject(text))
    }

    private fun toMap(o: JSONObject): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = convert(o.get(key))
        }
        return map
    }

    private fun toList(a: JSONArray): List<Any?> {
        val list = ArrayList<Any?>()
        for (i in 0 until a.length()) list.add(convert(a.get(i)))
        return list
    }

    private fun convert(v: Any?): Any? {
        if (v == null || v == JSONObject.NULL) return null
        if (v is JSONObject) return toMap(v)
        if (v is JSONArray) return toList(v)
        return v
    }
}
