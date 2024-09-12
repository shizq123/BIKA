package com.shizq.bika.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class SPUtil private constructor(context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     */
    fun put(key: String, value: Any) {
        sp.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> putString(key, value.toString())
            }
        }
    }

    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     */
    fun get(key: String, defaultObject: Any): Any {
        return when (defaultObject) {
            is String -> sp.getString(key, defaultObject) ?: defaultObject
            is Int -> sp.getInt(key, defaultObject)
            is Boolean -> sp.getBoolean(key, defaultObject)
            is Float -> sp.getFloat(key, defaultObject)
            is Long -> sp.getLong(key, defaultObject)
            else -> error("Unsupported Type")
        }
    }

    /**
     * 移除某个key值已经对应的值
     */
    fun remove(key: String) {
        sp.edit { remove(key) }
    }

    /**
     * 清除所有数据
     */
    fun clear() {
        sp.edit { clear() }
    }

    /**
     * 查询某个key是否已经存在
     */
    fun contains(key: String): Boolean {
        return sp.contains(key)
    }

    /**
     * 返回所有的键值对
     */
    fun getAll(): Map<String, *> {
        return sp.all
    }

    companion object {
        private lateinit var INSTANCE: SPUtil
        fun init(context: Context): SPUtil = SPUtil(context).also { INSTANCE = it }
        fun get(key: String, defaultObject: Any): Any = INSTANCE.get(key, defaultObject)

        fun put(key: String, defaultObject: Any) = INSTANCE.put(key, defaultObject)

        fun remove(key: String) = INSTANCE.remove(key)
    }
}
