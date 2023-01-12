package com.shizq.bika.ui.comiclist

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ComicListAdapter
import com.shizq.bika.adapter.ComicListAdapter2
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityComiclistBinding
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.StatusBarUtil
import me.jingbin.library.skeleton.ByRVItemSkeletonScreen
import me.jingbin.library.skeleton.BySkeleton

/**
 * 漫画列表
 */

class ComicListActivity : BaseActivity<ActivityComiclistBinding, ComicListViewModel>() {
    private lateinit var mComicListAdapter: ComicListAdapter
    private lateinit var mComicListAdapter2: ComicListAdapter2
    private lateinit var skeletonScreen: ByRVItemSkeletonScreen

    private lateinit var popupView: View
    private lateinit var popupImage: ImageView
    private lateinit var mPopupWindow: PopupWindow

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_comiclist
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {

        //接收传递的信息
        viewModel.tag = intent.getStringExtra("tag")
        val title = intent.getStringExtra("title")
        viewModel.value = intent.getStringExtra("value")

        //toolbar
        if (viewModel.tag.equals("search")) {
            binding.comiclistInclude.toolbar.title = "搜索：${title}"
        } else {
            binding.comiclistInclude.toolbar.title = title
        }
        setSupportActionBar(binding.comiclistInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //PopupWindow 用来显示图片大图
        popupView = View.inflate(this@ComicListActivity, R.layout.view_popup_image, null)
        popupImage = popupView.findViewById(R.id.popup_image)
        mPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        mPopupWindow.isOutsideTouchable = true
        mPopupWindow.isClippingEnabled = false

        //不同的返回数据 展示效果相同 所以用两种adapter
        mComicListAdapter = ComicListAdapter()
        mComicListAdapter2 = ComicListAdapter2()
        binding.comiclistRv.layoutManager = LinearLayoutManager(this)
        if (viewModel.tag.equals("random")) {
            skeletonScreen = BySkeleton
                .bindItem(binding.comiclistRv)
                .adapter(mComicListAdapter2)// 必须设置adapter，且在此之前不要设置adapter
                .load(R.layout.item_comiclist_skeleton)// item骨架图
                .duration(2000)// 微光一次显示时间
                .count(4)// item个数
                .show()
            viewModel.getRandom()
        } else {
            skeletonScreen = BySkeleton
                .bindItem(binding.comiclistRv)
                .adapter(mComicListAdapter)
                .load(R.layout.item_comiclist_skeleton)
                .duration(2000)
                .count(4)
                .show()
            viewModel.getComicList()
        }

        binding.comiclistLoadLayout.isEnabled = false//加载时 view不可点击
        intiListener()
    }

    private fun intiListener() {
        binding.comiclistRv.setOnItemClickListener { v, position ->
            if (viewModel.tag.equals("random")) {
                val data = mComicListAdapter2.getItemData(position)
                val intent = Intent(this@ComicListActivity, ComicInfoActivity::class.java)
                intent.putExtra("id", data._id)
                intent.putExtra("fileserver", data.thumb.fileServer)
                intent.putExtra("imageurl", data.thumb.path)
                intent.putExtra("title", data.title)
                intent.putExtra("author", data.author)
                intent.putExtra("totalViews", data.totalViews.toString())
                startActivity(intent)
            } else {
                val data = mComicListAdapter.getItemData(position)
                val intent = Intent(this@ComicListActivity, ComicInfoActivity::class.java)
                intent.putExtra("id", data._id)
                intent.putExtra("fileserver", data.thumb.fileServer)
                intent.putExtra("imageurl", data.thumb.path)
                intent.putExtra("title", data.title)
                intent.putExtra("author", data.author)
                intent.putExtra("totalViews", data.totalViews.toString())
                startActivity(intent)
            }
        }
        binding.comiclistRv.setOnItemChildClickListener { view, position ->
            if (viewModel.tag.equals("random")) {
                val data = mComicListAdapter2.getItemData(position)
                GlideApp.with(view.context).load(
                    if (data.thumb != null) {
                        GlideUrlNewKey(data.thumb.fileServer, data.thumb.path)
                    } else {
                        R.drawable.placeholder_avatar_2
                    }
                ).placeholder(R.drawable.placeholder_transparent_low).into(popupImage)

                StatusBarUtil.hide(this@ComicListActivity)
                mPopupWindow.showAtLocation(
                    this@ComicListActivity.window.decorView,
                    Gravity.BOTTOM,
                    0,
                    0
                )
            } else {
                val data = mComicListAdapter.getItemData(position)
                GlideApp.with(view.context).load(
                    if (data.thumb != null) {
                        GlideUrlNewKey(
                            data.thumb.fileServer,
                            (data.thumb.path)
                        )
                    } else {
                        R.drawable.placeholder_avatar_2
                    }
                ).placeholder(R.drawable.placeholder_transparent_low).into(popupImage)

                StatusBarUtil.hide(this@ComicListActivity)
                mPopupWindow.showAtLocation(
                    this@ComicListActivity.window.decorView,
                    Gravity.BOTTOM,
                    0,
                    0
                )
            }
        }
    }

    //toolbar菜单 这方法是在onCreate之后执行 根据判断是否是随机页面来显示排序菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (viewModel.tag.equals("categories") || viewModel.tag.equals("search")) {
            menuInflater.inflate(R.menu.toolbar_menu_comiclist, menu)
        }
        if (viewModel.tag.equals("favourite")) {
            menuInflater.inflate(R.menu.toolbar_menu_comiclist2, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.dd -> {
                item.isChecked = true
                setSort("dd")
            }
            R.id.da -> {
                item.isChecked = true
                setSort("da")
            }
            R.id.ld -> {
                item.isChecked = true
                setSort("ld")
            }
            R.id.vd -> {
                item.isChecked = true
                setSort("vd")
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setSort(sort: String) {
        if (viewModel.sort != sort) {
            viewModel.page = 0
            viewModel.sort = sort
            mComicListAdapter.clear()
            mComicListAdapter.notifyDataSetChanged()
            binding.comiclistRv.isEnabled = false//加载时不允许滑动，解决加载时滑动recyclerview报错
            binding.comiclistLoadLayout.visibility = ViewGroup.VISIBLE
            binding.comiclistLoadLayout.isEnabled = false
            showProgressBar(true, "")
            viewModel.getComicList()
        }
    }

    override fun initViewObservable() {
        viewModel.liveData.observe(this) {
            skeletonScreen.hide()
            binding.comiclistRv.isEnabled = true
            if (it.code == 200) {
                binding.comiclistLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                mComicListAdapter.addData(it.data.comics.docs)
                if (it.data.comics.pages <= it.data.comics.page) {
                    binding.comiclistRv.loadMoreEnd()//没有更多数据
                } else {
                    binding.comiclistRv.loadMoreComplete()//加载成功
                }

            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    showProgressBar(
                        false,
                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                    )
                } else {
                    //当页面不是第一页时 网络错误可能是分页加载时出现的网络错误
                    binding.comiclistRv.loadMoreFail()
                }
            }
        }

        viewModel.liveData2.observe(this) {
            skeletonScreen.hide()
            binding.comiclistRv.isEnabled = true
            if (it.code == 200) {
                mComicListAdapter2.addData(it.data.comics)
                binding.comiclistLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                binding.comiclistRv.loadMoreEnd()
            } else {

                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                )
            }
        }


        binding.comiclistRv.setOnLoadMoreListener {
            viewModel.getComicList()
        }

        //网络重试点击事件监听
        binding.comiclistLoadLayout.setOnClickListener {
            skeletonScreen.show()
            showProgressBar(true, "")
            if (viewModel.tag.equals("random")) {
                viewModel.getRandom()
            } else {
                viewModel.getComicList()
            }

        }

        //大图PopupWindow
        mPopupWindow.setOnDismissListener {
            //恢复状态栏
            StatusBarUtil.show(this@ComicListActivity)
        }
        popupView.setOnClickListener {
            mPopupWindow.dismiss()
        }
    }

    private fun showProgressBar(show: Boolean, string: String) {
        binding.comiclistLoadProgressBar.visibility =
            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.comiclistLoadError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.comiclistLoadText.text = string
        binding.comiclistLoadLayout.isEnabled = !show
    }
}