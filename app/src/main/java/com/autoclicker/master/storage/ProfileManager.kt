package com.autoclicker.master.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)

    fun saveProfile(profileName: String, points: List<ClickPoint>) {
        val array = JSONArray()

        for (point in points) {
            val obj = JSONObject().apply {
                put("id", point.id)
                put("x", point.x)
                put("y", point.y)
                put("delayMs", point.delayMs)
                put("durationMs", point.durationMs)
                put("isBack", point.isBack)
            }
            array.put(obj)
        }

        prefs.edit().putString(profileName, array.toString()).apply()
    }

    fun loadProfile(profileName: String): List<ClickPoint> {
        val jsonStr = prefs.getString(profileName, null) ?: return emptyList()

        val list = mutableListOf<ClickPoint>()

        try {
            val array = JSONArray(jsonStr)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                list.add(
                    ClickPoint(
                        obj.getInt("id"),
                        obj.getInt("x"),
                        obj.getInt("y"),
                        obj.getLong("delayMs"),
                        obj.getLong("durationMs"),
                        obj.optBoolean("isBack", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    fun getSerializedProfile(points: List<ClickPoint>): String {
        val array = JSONArray()

        for (point in points) {
            val obj = JSONObject().apply {
                put("id", point.id)
                put("x", point.x)
                put("y", point.y)
                put("delayMs", point.delayMs)
                put("durationMs", point.durationMs)
                put("isBack", point.isBack)
            }
            array.put(obj)
        }

        return array.toString()
    }

    fun parseSerializedProfile(json: String): List<ClickPoint> {
        val list = mutableListOf<ClickPoint>()

        try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                list.add(
                    ClickPoint(
                        obj.getInt("id"),
                        obj.getInt("x"),
                        obj.getInt("y"),
                        obj.getLong("delayMs"),
                        obj.getLong("durationMs"),
                        obj.optBoolean("isBack", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }
}