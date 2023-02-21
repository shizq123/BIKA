package com.shizq.bika.widget

import android.app.ActivityOptions
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.R
import com.shizq.bika.bean.*
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import com.shizq.bika.ui.image.ImageActivity
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.StatusBarUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class UserViewDialog(val context: AppCompatActivity) {
    private lateinit var dia: AlertDialog
    private var progress: ProgressDialog? = null
    private lateinit var dialog_view: View
    private lateinit var dialog_image_layout: View
    private lateinit var dialog_image: ImageView
    private lateinit var dialog_character: ImageView
    private lateinit var dialog_gender_level: TextView
    private lateinit var dialog_name: TextView
    private lateinit var dialog_title: TextView
    private lateinit var dialog_slogan: TextView

    private lateinit var popupView: View
    private lateinit var popupImage: ImageView
    private lateinit var mPopupWindow: PopupWindow

    val liveData_profile: MutableLiveData<BaseResponse<ProfileBean>> by lazy {
        MutableLiveData<BaseResponse<ProfileBean>>()
    }

    init {
        initview()
        initListener()
    }

    private fun initview() {
        dialog_view = View.inflate(context, R.layout.view_dialog_user, null)
        dialog_image_layout = dialog_view.findViewById(R.id.view_user_image_layout)
        dialog_image = dialog_view.findViewById(R.id.view_user_image)
        dialog_character = dialog_view.findViewById(R.id.view_user_character)
        dialog_gender_level = dialog_view.findViewById(R.id.view_user_gender_level)
        dialog_name = dialog_view.findViewById(R.id.view_user_nickname)
        dialog_title = dialog_view.findViewById(R.id.view_user_title)
        dialog_slogan = dialog_view.findViewById(R.id.view_user_slogan)

        //PopupWindow显示大图片
        popupView = View.inflate(context, R.layout.view_popup_image, null)
        popupImage = popupView.findViewById(R.id.popup_image)
        mPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        mPopupWindow.isOutsideTouchable = true
        mPopupWindow.isClippingEnabled = false

    }

    private fun initListener() {
        mPopupWindow.setOnDismissListener {
            //恢复状态栏
            StatusBarUtil.show(context)
        }
        popupView.setOnClickListener {
            mPopupWindow.dismiss()
        }

        liveData_profile.observe(context) {
            if (it.code == 200) {
                progress?.dismiss()
                progress = null
                val t = it.data.user
                // 请求成功
                userDialog(
                    t.name,
                    t.title,
                    t.gender,
                    t.level,
                    t.slogan,
                    { if (t.avatar != null) t.avatar.fileServer else "" },
                    { if (t.avatar != null) t.avatar.path else "" },
                    t.character,
                    context.window.decorView
                )
            } else {
                dia.dismiss()
                Toast.makeText(context, "网络请求失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showUserDialog(t: CommentsBean.User) {
        if (t == null) {
            return
        }
        showUserDialog(t, context.window.decorView)
    }

    fun showUserDialog(t: CommentsBean.User, parentView: View) {
        if (t == null) {
            return
        }
        userDialog(
            t.name,
            t.title,
            t.gender,
            t.level,
            t.slogan,
            { if (t.avatar != null) t.avatar.fileServer else "" },
            { if (t.avatar != null) t.avatar.path else "" },
            t.character,
            parentView
        )
    }

    fun showUserDialog(t: ComicInfoBean.Comic.Creator) {
        if (t == null) {
            return
        }
        userDialog(
            t.name,
            t.title,
            t.gender,
            t.level,
            t.slogan,
            { if (t.avatar != null) t.avatar.fileServer else "" },
            { if (t.avatar != null) t.avatar.path else "" },
            t.character,
            context.window.decorView
        )
    }

    fun showUserDialog(t: NotificationsBean.Notifications.Doc.Sender) {
        if (t == null) {
            return
        }
        userDialog(
            t.name,
            t.title,
            t.gender,
            t.level,
            t.slogan,
            { if (t.avatar != null) t.avatar.fileServer else "" },
            { if (t.avatar != null) t.avatar.path else "" },
            t.character,
            context.window.decorView
        )
    }

    fun showUserDialog(t: KnightBean.Users) {
        if (t == null) {
            return
        }
        userDialog(
            t.name,
            t.title,
            t.gender,
            t.level,
            t.slogan,
            { if (t.avatar != null) t.avatar.fileServer else "" },
            { if (t.avatar != null) t.avatar.path else "" },
            t.character,
            context.window.decorView
        )
    }

    fun showUserDialog(t: ChatMessage2Bean.Data.Profile) {
        if (t == null) {
            return
        }

        var fileServer = ""
        var path = ""

        if (t.avatarUrl != null && t.avatarUrl != "") {
            val i: Int = t.avatarUrl.indexOf("/static/")
            if (i > 0) {
                fileServer = t.avatarUrl.substring(0, i)
                path = t.avatarUrl.substring(i + 8)
            } else {
                path = t.avatarUrl
            }
        }
        userDialog(
            t.name,
            t.title,
            t.gender,
            t.level,
            t.slogan,
            { fileServer },
            { path },
            "",
            context.window.decorView
        )
    }

    fun showUserDialog(userId: String) {
        progress =  ProgressDialog.show(context, null, "加载用户信息...", true)
        getProfile(userId)
    }

    private fun userDialog(
        name: String,
        title: String,
        gender: String,
        level: Int,
        slogan: String,
        fileServer: () -> String,
        path: () -> String,
        character: String,
        parentView: View
    ) {
        dialog_name.text = name
        dialog_title.text = title
        dialog_gender_level.text = "${
            when (gender) {
                "m" -> "(绅士)"
                "f" -> "(淑女)"
                else -> "(机器人)"
            }
        } Lv.$level"

        if (slogan.isNullOrEmpty()) {
            dialog_slogan.setText(R.string.slogan)
        } else {
            dialog_slogan.text = slogan
        }
        //头像
        GlideApp.with(context)
            .load(
                if (path() != "") {
                    GlideUrlNewKey(fileServer(), path())
                } else {
                    R.drawable.placeholder_avatar_2
                }
            )
            .placeholder(R.drawable.placeholder_transparent_low)
            .into(dialog_image)

        //头像框
        GlideApp.with(context)
            .load(if (character.isNullOrEmpty()) "" else character)
            .into(dialog_character)

        dia = MaterialAlertDialogBuilder(context).setView(dialog_view).show()

        dia.setOnDismissListener {
            //用完必须销毁 不销毁报错
            progress?.dismiss()
            progress = null
            (dialog_view.parent as ViewGroup).removeView(dialog_view)
        }

        //dialog view 头像点击事件
        dialog_image_layout.setOnClickListener {
            dia.dismiss()
            PopupWindow(fileServer(), path(), parentView)
        }
    }

    fun PopupWindow(fileServer: String, path: String, parentView: View) {
        if (path != "") {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putExtra("fileserver", fileServer)
            intent.putExtra("imageurl", path)
            val options = ActivityOptions.makeSceneTransitionAnimation(context)
            context.startActivity(intent, options.toBundle())

        }
    }

    fun PopupWindow(bitmap: Bitmap) {
        popupImage.setImageBitmap(bitmap)
        StatusBarUtil.hide(context)
        //PopupWindow会被BottomSheetDialog的view覆盖 解决办法用BottomSheetDialog的view替换this.window.decorView
        mPopupWindow.showAtLocation(
            context.window.decorView,
            Gravity.BOTTOM,
            0,
            0
        )
    }

    private fun getProfile(userId: String) {
        RetrofitUtil.service.userProfileGet(
            userId,
            BaseHeaders("users/$userId/profile", "GET").getHeaderMapAndToken()
        )
            .compose { upstream ->
                upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe(object : BaseObserver<ProfileBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ProfileBean>) {
                    liveData_profile.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<ProfileBean>) {
                    liveData_profile.postValue(baseResponse)
                }

            })
    }
}