package com.shizq.bika.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.shizq.bika.BIKAApplication
import com.shizq.bika.R
import com.shizq.bika.bean.UpdateBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseResponse
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.utils.AppVersion
import com.shizq.bika.utils.GlideCacheUtil
import com.shizq.bika.utils.SPUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DefaultObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.HttpException

// TODO 没有bug 以后再优化 懒
class SettingsPreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        val setting_close: Preference? = findPreference("setting_close")//清理缓存
        val setting_app_channel: Preference? = findPreference("setting_app_channel")//分流节点
        val setting_punch: Preference? = findPreference("setting_punch")//自动打卡
        val setting_night: Preference? = findPreference("setting_night")//夜间模式
        val setting_app_ver: Preference? = findPreference("setting_app_ver")//应用版本
        val setting_change_password: Preference? =
            findPreference("setting_change_password")//修改密码
        val setting_exit: Preference? = findPreference("setting_exit")//账号退出

        setting_close?.onPreferenceClickListener = this
        setting_app_channel?.onPreferenceChangeListener = this
        setting_punch?.onPreferenceChangeListener = this
        setting_night?.onPreferenceChangeListener = this
        setting_app_ver?.onPreferenceClickListener = this
        setting_change_password?.onPreferenceClickListener = this
        setting_exit?.onPreferenceClickListener = this

        //自动打卡
        setting_punch as SwitchPreferenceCompat
        setting_punch.summary = if (setting_punch.isChecked) "开启" else "关闭"

        //夜间模式
        setting_night as DropDownPreference
        setting_night.summary = setting_night.value

        //当前版本
        setting_app_ver?.summary = "当前版本：${AppVersion().name()}(${AppVersion().code()})"

        //分流
        setting_app_channel?.summary =
            when (SPUtil.get(context, "setting_app_channel", "2") as String) {
                "1" -> "分流一"
                "2" -> "分流二"
                else -> "分流三"
            }

        //清理图片
        setting_close?.summary = GlideCacheUtil.getInstance().getCacheSize(context)


    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "setting_close" -> {
                activity?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle("是否清理图片缓存？")
                        .setPositiveButton("确定") { dialog, which ->
                            GlideCacheUtil.getInstance().clearImageAllCache(context)
                            preference.summary = "0.0Byte"
                            Toast.makeText(activity, "清理完成", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("取消", null)
                        .show()

                }
                return true
            }

            "setting_app_ver" -> {
                checkUpdates()
                return true
            }

            "setting_change_password" -> {

                activity?.let {
                    val dia = MaterialAlertDialogBuilder(it)
                        .setTitle("修改密码")
                        .setView(R.layout.view_dialog_edit_text_change_password)
                        .setCancelable(false)
                        .setPositiveButton("修改", null)
                        .setNegativeButton("取消", null)
                        .show();//在按键响应事件中显示此对话框 }
                    dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        dia as AlertDialog
                        val newPasswordLayout: TextInputLayout? =
                            dia.findViewById(android.R.id.icon1)
                        val confirmPasswordLayout: TextInputLayout? =
                            dia.findViewById(android.R.id.icon2)
                        val newPassword: TextInputEditText? = dia.findViewById(android.R.id.text1)
                        val confirmPassword: TextInputEditText? =
                            dia.findViewById(android.R.id.text2)

                        newPassword?.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                            override fun afterTextChanged(s: Editable) {
                                newPasswordLayout?.isErrorEnabled = false
                            }
                        })
                        confirmPassword?.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                            override fun afterTextChanged(s: Editable) {
                                confirmPasswordLayout?.isErrorEnabled = false
                            }
                        })
                        if (newPassword?.text.toString().trim().isEmpty()) {
                            newPasswordLayout?.isErrorEnabled = true
                            newPasswordLayout?.error = "新密码不能为空！"
                        } else if (newPassword?.text.toString().trim().length < 8) {
                            newPasswordLayout?.isErrorEnabled = true
                            newPasswordLayout?.error = "密码不能小于8字！"
                        }
                        if (confirmPassword?.text.toString().trim().isEmpty()) {
                            confirmPasswordLayout?.isErrorEnabled = true
                            confirmPasswordLayout?.error = "确认密码不能为空！"
                        } else if (confirmPassword?.text.toString()
                                .trim() != newPassword?.text.toString().trim()
                        ) {
                            confirmPasswordLayout?.isErrorEnabled = true
                            confirmPasswordLayout?.error = "确认密码与新密码不符！"
                        }
                        if (confirmPassword?.text.toString().trim().isNotEmpty()
                            && newPassword?.text.toString().trim().isNotEmpty()
                            && newPassword?.text.toString().trim().length >= 8
                            && (confirmPassword?.text.toString().trim()
                                    == newPassword?.text.toString().trim())
                        ) {
                            changePassword("", newPassword?.text.toString().trim())
                            // TODO 添加加载进度条
                            dia.dismiss()
                        }
                    }

                }
                return false
            }

            "setting_exit" -> {
                activity?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle("你确定要退出登录吗")
                        .setPositiveButton("确定") { _, _ ->
                            SPUtil.remove(BIKAApplication.contextBase, "token")
                            SPUtil.remove(BIKAApplication.contextBase, "chat_token")
                            startActivity(Intent(activity, AccountActivity::class.java))
                            activity?.finishAffinity()
                        }
                        .setNegativeButton("取消", null)
                        .show()

                }
                return false
            }

        }
        return false
    }

    override fun onPreferenceChange(preference: Preference, value: Any?): Boolean {
        when (preference.key) {
            "setting_punch" -> {
                // TODO 一个小bug 开关会影响toolbar颜色变化
                preference as SwitchPreferenceCompat
                preference.summary = if (value as Boolean) "开启" else "关闭"
                return true
            }
            "setting_app_channel" -> {
                value as String
                preference as DropDownPreference
                preference.value = value
                preference.summary =
                    when (value) {
                        "1" -> "分流一"
                        "2" -> "分流二"
                        else -> "分流三"
                    }
                return true
            }
            "setting_night" -> {
                value as String
                preference as DropDownPreference
                preference.summary = value
                preference.value = value
                AppCompatDelegate.setDefaultNightMode(
                    when (value) {
                        "开启" -> AppCompatDelegate.MODE_NIGHT_YES
                        "关闭" -> AppCompatDelegate.MODE_NIGHT_NO
                        "跟随系统" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        else -> AppCompatDelegate.MODE_NIGHT_NO
                    }
                )
                return true
            }
        }
        return true
    }

    private fun changePassword(oldpassword: String, password: String) {
        var old = oldpassword
        if (oldpassword == "") {
            old = SPUtil.get(BIKAApplication.contextBase, "password", "") as String
        }

        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            JsonObject().apply {
                addProperty("new_password", password)
                addProperty("old_password", old)
            }.asJsonObject.toString()
        )
        val headers = BaseHeaders("users/password", "PUT").getHeaderMapAndToken()
        RetrofitUtil.service.changePasswordPUT(body, headers)
            .compose { upstream ->
                upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe(object : DefaultObserver<BaseResponse<*>>() {

                override fun onNext(baseResponse: BaseResponse<*>) {
                    if (baseResponse.code == 200) {

                        //保存密码
                        SPUtil.put(BIKAApplication.contextBase, "password", password)
                        Toast.makeText(activity, "修改密码成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            activity,
                            "修改密码失败，网络错误code=${baseResponse.code} error=${baseResponse.error} message=${baseResponse.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<BaseResponse<*>>() {}.type
                            val baseResponse: BaseResponse<*> =
                                Gson().fromJson(responseBody.string(), type)
                            if (baseResponse.code == 400 && baseResponse.error == "1010") {
                                showAlertDialog()
                            }
                        } else {
                            Toast.makeText(activity, "修改密码失败，网络错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "修改密码失败，网络错误", Toast.LENGTH_SHORT).show()

                    }
                }

                override fun onComplete() {}
            })
    }

    fun showAlertDialog() {
        activity?.let {
            val dia: AlertDialog = MaterialAlertDialogBuilder(it)
                .setTitle("修改密码失败，请重试")
                .setView(R.layout.view_dialog_edit_text_change_password_old)
                .setCancelable(false)
                .setPositiveButton("修改", null)
                .setNegativeButton("取消", null)
                .show();//在按键响应事件中显示此对话框 }
            dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val oldPasswordLayout: TextInputLayout? = dia.findViewById(R.id.old_password_layout)
                val oldPassword: TextInputEditText? = dia.findViewById(R.id.new_password)
                val newPasswordLayout: TextInputLayout? = dia.findViewById(R.id.old_password)
                val newPassword: TextInputEditText? = dia.findViewById(R.id.confirm_password_layout)
                val confirmPasswordLayout: TextInputLayout? =
                    dia.findViewById(R.id.new_password_layout)
                val confirmPassword: TextInputEditText? = dia.findViewById(R.id.confirm_password)

                oldPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        oldPasswordLayout?.isErrorEnabled = false
                    }
                })

                newPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        newPasswordLayout?.isErrorEnabled = false
                    }
                })

                confirmPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        confirmPasswordLayout?.isErrorEnabled = false
                    }
                })


                if (oldPassword?.text.toString().trim().isEmpty()) {
                    oldPasswordLayout?.isErrorEnabled = true
                    oldPasswordLayout?.error = "旧密码不能为空！"
                } else if (oldPassword?.text.toString().trim().length < 8) {
                    oldPasswordLayout?.isErrorEnabled = true
                    oldPasswordLayout?.error = "密码不能小于8字！"
                }

                if (newPassword?.text.toString().trim().isEmpty()) {
                    newPasswordLayout?.isErrorEnabled = true
                    newPasswordLayout?.error = "新密码不能为空！"
                } else if (newPassword?.text.toString().trim().length < 8) {
                    newPasswordLayout?.isErrorEnabled = true
                    newPasswordLayout?.error = "密码不能小于8字！"
                }

                if (confirmPassword?.text.toString().trim().isEmpty()) {
                    confirmPasswordLayout?.isErrorEnabled = true
                    confirmPasswordLayout?.error = "确认密码不能为空！"
                } else if (confirmPassword?.text.toString().trim() != newPassword?.text.toString()
                        .trim()
                ) {
                    confirmPasswordLayout?.isErrorEnabled = true
                    confirmPasswordLayout?.error = "确认密码与新密码不符！"
                }

                if (oldPassword?.text.toString().trim().isNotEmpty()
                    && oldPassword?.text.toString().trim().length >= 8
                    && confirmPassword?.text.toString().trim().isNotEmpty()
                    && newPassword?.text.toString().trim().isNotEmpty()
                    && newPassword?.text.toString().trim().length >= 8
                    && (confirmPassword?.text.toString().trim() == newPassword?.text.toString()
                        .trim())
                ) {
                    changePassword(
                        oldPassword?.text.toString().trim(),
                        newPassword?.text.toString().trim()
                    )
                    // TODO 添加加载进度条
                    dia.dismiss()
                }
            }

        }
    }

    fun checkUpdates() {
        RetrofitUtil.service_update.updateGet()
            .compose { upstream ->
                upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe(object : Observer<UpdateBean> {
                override fun onNext(t: UpdateBean) {
                    if (t != null) {
                        if (t.version.toInt() > AppVersion().code()) {
                            context?.let {
                                MaterialAlertDialogBuilder(it)
                                    .setTitle("新版本 v${t.short_version}")
                                    .setMessage(t.release_notes)
                                    .setPositiveButton("更新") { _, _ ->
                                        val intent = Intent()
                                        intent.action = "android.intent.action.VIEW"
                                        intent.data = Uri.parse(t.download_url)
                                        startActivity(intent)
                                    }
                                    .setNegativeButton("取消", null)
                                    .show()
                            }
                        } else {
                            Toast.makeText(activity, "您当前已经是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "检查更新失败，请稍后再试", Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onError(e: Throwable) {
                    Toast.makeText(activity, "检查更新失败，请稍后再试", Toast.LENGTH_SHORT).show()
                }

                override fun onSubscribe(d: Disposable) {}
                override fun onComplete() {}
            })
    }
}