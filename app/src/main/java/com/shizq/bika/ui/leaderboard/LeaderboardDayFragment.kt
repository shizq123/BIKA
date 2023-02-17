package com.shizq.bika.ui.leaderboard

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ComicListAdapter2
import com.shizq.bika.base.BaseFragment
import com.shizq.bika.databinding.FragmentLeaderboardDayBinding
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import com.shizq.bika.ui.image.ImageActivity
import me.jingbin.library.skeleton.ByRVItemSkeletonScreen
import me.jingbin.library.skeleton.BySkeleton

/**
 * 排行榜  日榜 周榜 月榜
 */

class LeaderboardDayFragment :
    BaseFragment<FragmentLeaderboardDayBinding, LeaderboardDayViewModel>() {
    private lateinit var adapter: ComicListAdapter2
    private lateinit var skeletonScreen: ByRVItemSkeletonScreen

    private lateinit var popupView: View
    private lateinit var popupImage: ImageView

    override fun initContentView(
        inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?
    ): Int {
        return R.layout.fragment_leaderboard_day
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        viewModel.tt = arguments?.getString("tt")

        adapter = ComicListAdapter2()
        binding.leaderboardDayRv.layoutManager = LinearLayoutManager(context)

        //PopupWindow 用来显示图片大图
        popupView = View.inflate(context, R.layout.view_popup_image, null)
        popupImage = popupView.findViewById(R.id.popup_image)

        skeletonScreen = BySkeleton
            .bindItem(binding.leaderboardDayRv)
            .adapter(adapter)// 必须设置adapter，且在此之前不要设置adapter
            .load(R.layout.item_comiclist_skeleton)// item骨架图
//            .frozen(false)
            .duration(2000)// 微光一次显示时间
            .count(4)// item个数
            .show()



        binding.leaderboardDayLoadLayout.isEnabled = false//加载时 view不可点击
        viewModel.getLeaderboard()
        initListener()
    }

    private fun initListener() {
        binding.leaderboardDayRv.setOnItemClickListener { _, position ->
            val data = adapter.getItemData(position)
            val intent = Intent(activity, ComicInfoActivity::class.java)
            intent.putExtra("id", data._id)
            intent.putExtra("fileserver", data.thumb.fileServer)
            intent.putExtra("imageurl", data.thumb.path)
            intent.putExtra("title", data.title)
            intent.putExtra("author", data.author)
            intent.putExtra("totalViews", data.totalViews.toString())
            startActivity(intent)
        }
        binding.leaderboardDayRv.setOnItemChildClickListener { view, position ->
            val data = adapter.getItemData(position)
            var fileServer = ""
            var path = ""
            if (data.thumb != null) {
                fileServer = data.thumb.fileServer
                path = data.thumb.path
            }
            val intent = Intent(activity, ImageActivity::class.java)
            intent.putExtra("fileserver", fileServer)
            intent.putExtra("imageurl", path)
            val options = ActivityOptions.makeSceneTransitionAnimation(activity, view, "image")
            startActivity(intent, options.toBundle())

        }

        //加了监听才能显示 显示底部布局
        binding.leaderboardDayRv.setOnLoadMoreListener { }

        //网络重试点击事件监听
        binding.leaderboardDayLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            skeletonScreen.show()
            viewModel.getLeaderboard()

        }

    }

    override fun initViewObservable() {
        viewModel.liveData.observe(this) {
            skeletonScreen.hide()
            binding.leaderboardDayRv.isEnabled = true
            if (it.code == 200) {
                adapter.addData(it.data.comics)
                binding.leaderboardDayLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                binding.leaderboardDayRv.loadMoreEnd()
            } else {
                showProgressBar(
                    false, "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                )
            }
        }
    }

    private fun showProgressBar(show: Boolean, string: String) {

        binding.leaderboardDayLoadProgressBar.visibility =
            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.leaderboardDayLoadError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.leaderboardDayLoadText.text = string
        binding.leaderboardDayLoadLayout.isEnabled = !show
    }

}

