package com.shizq.bika.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.KnightAdapter
import com.shizq.bika.base.BaseFragment
import com.shizq.bika.databinding.FragmentLeaderboardKnightBinding
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.widget.UserViewDialog
import me.jingbin.library.skeleton.ByRVItemSkeletonScreen
import me.jingbin.library.skeleton.BySkeleton

/**
 * 骑士榜
 */

class LeaderboardKnightFragment :
    BaseFragment<FragmentLeaderboardKnightBinding, LeaderboardKnightViewModel>() {
    private lateinit var mAdapter: KnightAdapter
    private lateinit var skeletonScreen: ByRVItemSkeletonScreen
    private lateinit var userViewDialog: UserViewDialog

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

        userViewDialog = UserViewDialog(activity as AppCompatActivity)

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
            userViewDialog.showUserDialog(data)

        }
        //加了监听才能显示 显示底部布局
        binding.leaderboardKnightRv.setOnLoadMoreListener { }

        binding.leaderboardKnightLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            skeletonScreen.show()
            viewModel.getKnight()
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

