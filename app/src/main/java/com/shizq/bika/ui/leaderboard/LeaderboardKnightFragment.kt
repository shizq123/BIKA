package com.shizq.bika.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.KnightAdapter
import com.shizq.bika.base.BaseFragment
import com.shizq.bika.databinding.FragmentLeaderboardKnightBinding
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.StatusBarUtil
import me.jingbin.library.skeleton.ByRVItemSkeletonScreen
import me.jingbin.library.skeleton.BySkeleton

/**
 * 骑士榜
 */

class LeaderboardKnightFragment :
    BaseFragment<FragmentLeaderboardKnightBinding, LeaderboardKnightViewModel>() {
    private lateinit var mAdapter: KnightAdapter
    private lateinit var skeletonScreen : ByRVItemSkeletonScreen
    private lateinit var dia: AlertDialog

    private lateinit var v: View
    private lateinit var image_layout: View
    private lateinit var image: ImageView
    private lateinit var character: ImageView
    private lateinit var gender_level: TextView
    private lateinit var name: TextView
    private lateinit var title: TextView
    private lateinit var slogan: TextView

    private lateinit var popupView: View
    private lateinit var popupImage: ImageView
    private lateinit var mPopupWindow: PopupWindow


    override fun initContentView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): Int {
        return R.layout.fragment_leaderboard_knight
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        mAdapter = KnightAdapter()
        binding.leaderboardKnightRv.layoutManager = LinearLayoutManager(context)
        // 骨架图
        skeletonScreen = BySkeleton
            .bindItem(binding.leaderboardKnightRv)
            .adapter(mAdapter)// 必须设置adapter，且在此之前不要设置adapter
            .load(R.layout.item_knight_skeleton)// item骨架图
//            .frozen(false)
            .duration(2000)// 微光一次显示时间
            .count(8)// item个数
            .show()

        //dialog view id
        v = View.inflate(context, R.layout.view_dialog_user, null)
        image_layout = v.findViewById(R.id.view_user_image_layout)
        image = v.findViewById(R.id.view_user_image)
        character = v.findViewById(R.id.view_user_character)
        gender_level = v.findViewById(R.id.view_user_gender_level)
        name = v.findViewById(R.id.view_user_nickname)
        title = v.findViewById(R.id.view_user_title)
        slogan = v.findViewById(R.id.view_user_slogan)

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

        binding.leaderboardKnightLoadLayout.isEnabled = false//加载时 view不可点击
        viewModel.getKnight()
        initListener()
    }

    private fun initListener() {
        binding.leaderboardKnightRv.setOnItemClickListener { v, position ->
            val intent = Intent(context, ComicListActivity::class.java)
            intent.putExtra("tag", "knight")
            intent.putExtra("value", mAdapter.getItemData(position)._id)
            intent.putExtra("title", mAdapter.getItemData(position).name)
            startActivity(intent)
        }
        binding.leaderboardKnightRv.setOnItemChildClickListener { view, position ->
            val data = mAdapter.getItemData(position)

            gender_level.text = "${
                when (data.gender) {
                    "m" -> "(绅士)"
                    "f" -> "(淑女)"
                    else -> "(机器人)"
                }
            } Lv.${data.level}"
            name.text = data.name
            title.text = data.title
            if (data.slogan.isNullOrBlank()) {
                slogan.setText(R.string.slogan)
            } else {
                slogan.text = data.slogan
            }

            //头像
            GlideApp.with(view.context)
                .load(
                    if (data.avatar != null)
                        GlideUrlNewKey(
                            data.avatar.fileServer,
                            data.avatar.path
                        )
                    else
                        R.drawable.placeholder_avatar_2

                )
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(image)

            //头像框
            GlideApp.with(view.context)
                .load(
                    if (!data.character.isNullOrEmpty())
                    //https://pica-web.wakamoment.tk/ 网站失效 替换到能用的
                        data.character.replace(
                            "pica-web.wakamoment.tk",
                            "pica-pica.wikawika.xyz"
                        )
                    else ""
                )
                .into(character)

            dia = MaterialAlertDialogBuilder(view.context).setView(this@LeaderboardKnightFragment.v)
                .show()
            dia.setOnDismissListener {
                //用完必须销毁 不销毁报错
                (this@LeaderboardKnightFragment.v.parent as ViewGroup).removeView(
                    this@LeaderboardKnightFragment.v
                )
            }
            //dialog view 头像点击事件
            image_layout.setOnClickListener {
                if (data.avatar != null) {
                    GlideApp.with(it)
                        .load(
                            if (data.avatar != null) {
                                GlideUrlNewKey(
                                    data.avatar.fileServer,
                                    data.avatar.path
                                )
                            } else {
                                R.drawable.placeholder_avatar_2
                            }
                        )
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .into(popupImage)

                    StatusBarUtil.hide(activity)
                    mPopupWindow.showAtLocation(
                        activity?.window?.decorView,
                        Gravity.BOTTOM,
                        0,
                        0
                    )

                }
                dia.dismiss()
            }
        }
        //加了监听才能显示 显示底部布局
        binding.leaderboardKnightRv.setOnLoadMoreListener {  }

        binding.leaderboardKnightLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            skeletonScreen.show()
            viewModel.getKnight()
        }

        mPopupWindow.setOnDismissListener {
            //恢复状态栏
            StatusBarUtil.show(activity)
        }
        popupView.setOnClickListener {
            mPopupWindow.dismiss()
        }

    }

    override fun initViewObservable() {
        viewModel.liveData.observe(this) {
            skeletonScreen.hide()
            if (it.code == 200) {
                mAdapter.addData(it.data.users)
                binding.leaderboardKnightLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                binding.leaderboardKnightRv.loadMoreEnd()
            } else {
                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                )

            }
        }

    }

    private fun showProgressBar(show: Boolean, string: String) {
        binding.leaderboardKnightLoadProgressBar.visibility =
            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.leaderboardKnightLoadError.visibility =
            if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.leaderboardKnightLoadText.text = string
        binding.leaderboardKnightLoadLayout.isEnabled = !show
    }
}

