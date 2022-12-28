package com.shizq.bika.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.NotificationsAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.bean.NotificationsBean
import com.shizq.bika.databinding.ActivityNotificationsBinding
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.StatusBarUtil

class NotificationsActivity : BaseActivity<ActivityNotificationsBinding, NotificationsViewModel>() {
    lateinit var mAdapter: NotificationsAdapter

    private lateinit var dia: AlertDialog
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

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_notifications
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }
    override fun initData() {
        binding.notificationsInclude.toolbar.title = "我的消息"
        setSupportActionBar(binding.notificationsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.notificationsRv.layoutManager = LinearLayoutManager(this)
        mAdapter = NotificationsAdapter()
        binding.notificationsRv.adapter = mAdapter

        //dialog 用户view id
        dialog_view = View.inflate(this, R.layout.view_dialog_user, null)
        dialog_image_layout = dialog_view.findViewById(R.id.view_user_image_layout)
        dialog_image = dialog_view.findViewById(R.id.view_user_image)
        dialog_character = dialog_view.findViewById(R.id.view_user_character)
        dialog_gender_level = dialog_view.findViewById(R.id.view_user_gender_level)
        dialog_name = dialog_view.findViewById(R.id.view_user_nickname)
        dialog_title = dialog_view.findViewById(R.id.view_user_title)
        dialog_slogan = dialog_view.findViewById(R.id.view_user_slogan)

        //PopupWindow显示大图片
        popupView = View.inflate(this, R.layout.view_popup_image, null)
        popupImage = popupView.findViewById(R.id.popup_image)
        mPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        mPopupWindow.isOutsideTouchable = true
        mPopupWindow.isClippingEnabled = false

        binding.notificationsBottomInclude.loadLayout.isEnabled = false
        viewModel.getNotifications()
        initListener()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }
    private fun initListener(){
        //加载更多
        binding.notificationsRv.setOnLoadMoreListener {
            viewModel.getNotifications()
        }
        //网络重试点击事件监听
        binding.notificationsBottomInclude.loadLayout.setOnClickListener {
            binding.notificationsBottomInclude.loadLayout.isEnabled = false
            binding.notificationsBottomInclude.loadProgressBar.visibility = ViewGroup.VISIBLE
            binding.notificationsBottomInclude.loadError.visibility = ViewGroup.GONE
            binding.notificationsBottomInclude.loadText.text = ""
            viewModel.getNotifications()

        }

        mPopupWindow.setOnDismissListener {
            //恢复状态栏
            StatusBarUtil.show(this)
        }
        popupView.setOnClickListener {
            mPopupWindow.dismiss()
        }

        binding.notificationsRv.setOnItemClickListener { view, position ->
            //服务没有返回具体的哪条评论，只返回了哪个漫画或着游戏的id
            val data=mAdapter.getItemData(position)
            if (data.redirectType == "comment") {
                val intentComments = Intent(this, CommentsActivity::class.java)
                intentComments.putExtra("id", data._redirectId)
                if (data.cover != null) {
                    //服务器没有返回哪里的评论，我是根据有没有封面判断的评论的类型，这个不确定对不对
                    //游戏评论
                    intentComments.putExtra("comics_games", "comics")
                } else {
                    //漫画评论
                    intentComments.putExtra("comics_games", "games")
                }
                startActivity(intentComments)
            }
        }

        binding.notificationsRv.setOnItemChildClickListener { view, position ->
            if (view.id == R.id.item_notifications_image_layout && mAdapter.getItemData(position).redirectType == "comment") {
                showUserDialog(mAdapter.getItemData(position))
            }
        }
    }
    override fun initViewObservable() {
        viewModel.liveData.observe(this) {
            if (it.code == 200) {
                if (it.data.notifications.pages == Integer.valueOf(it.data.notifications.page)) {
                    //总页数等于当前页数 显示后面没有数据
                    binding.notificationsRv.loadMoreEnd()
                } else {
                    //总页数不等于当前页数 回收加载布局
                    binding.notificationsRv.loadMoreComplete() //加载完成
                }

                binding.notificationsBottomInclude.loadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                mAdapter.addData(it.data.notifications.docs)
            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    binding.notificationsBottomInclude.loadProgressBar.visibility = ViewGroup.GONE
                    binding.notificationsBottomInclude.loadError.visibility = ViewGroup.VISIBLE
                    binding.notificationsBottomInclude.loadText.text =
                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                    binding.notificationsBottomInclude.loadLayout.isEnabled = true
                } else {
                    //当页面不是第一页时 网络错误可能是分页加载时出现的网络错误
                    binding.notificationsRv.loadMoreFail()
                }
            }
        }
    }

    fun showUserDialog(t: NotificationsBean.Notifications.Doc) {
        dialog_gender_level.text = "${
            when (t._sender.gender) {
                "m" -> "(绅士)"
                "f" -> "(淑女)"
                else -> "(机器人)"
            }
        } Lv.${t._sender.level}"
        dialog_name.text = t._sender.name
        dialog_title.text = t._sender.title
        if (t._sender.slogan.isNullOrBlank()) {
            dialog_slogan.setText(R.string.slogan)
        } else {
            dialog_slogan.text = t._sender.slogan
        }
        //头像
        GlideApp.with(this)
            .load(
                if (t._sender.avatar != null) {
                    GlideUrlNewKey(
                        t._sender.avatar.fileServer,
                        t._sender.avatar.path
                    )
                } else {
                    R.drawable.placeholder_avatar_2
                }
            )
            .placeholder(R.drawable.placeholder_transparent_low)
            .into(dialog_image)

        //头像框
        GlideApp.with(this)
            .load(if (t._sender.character.isNullOrEmpty()) "" else t._sender.character)
            .into(dialog_character)

        dia = MaterialAlertDialogBuilder(this).setView(dialog_view)
            .show()

        dia.setOnDismissListener {
            //用完必须销毁 不销毁报错
            (dialog_view.parent as ViewGroup).removeView(dialog_view)
        }

        //dialog view 头像点击事件
        dialog_image_layout.setOnClickListener {
            if (t._sender.avatar != null) {

                GlideApp.with(it)
                    .load(
                        if (t._sender.avatar != null) {
                            GlideUrlNewKey(
                                t._sender.avatar.fileServer,
                                t._sender.avatar.path
                            )
                        } else {
                            R.drawable.placeholder_avatar_2
                        }
                    )
                    .placeholder(R.drawable.placeholder_avatar_2)
                    .into(popupImage)

                StatusBarUtil.hide(this)
                mPopupWindow.showAtLocation(
                    this.window.decorView,
                    Gravity.BOTTOM,
                    0,
                    0
                )

            }
            dia.dismiss()
        }
    }
}