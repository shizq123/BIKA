package com.shizq.bika.ui.comiclist

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ComicListAdapter
import com.shizq.bika.adapter.ComicListAdapter2
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityComiclistBinding
import com.shizq.bika.database.Search
import com.shizq.bika.network.Result
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import com.shizq.bika.ui.image.ImageActivity
import com.shizq.bika.utils.StatusBarUtil
import kotlinx.coroutines.launch
import me.jingbin.library.skeleton.ByRVItemSkeletonScreen
import me.jingbin.library.skeleton.BySkeleton
import kotlin.math.ceil

/**
 * 漫画列表
 */

class ComicListActivity : BaseActivity<ActivityComiclistBinding, ComicListViewModel>() {
    companion object {
        private var tag = arrayOf<CharSequence>(
            "全彩",
            "長篇",
            "同人",
            "短篇",
            "圓神領域",
            "碧藍幻想",
            "CG雜圖",
            "英語 ENG",
            "生肉",
            "純愛",
            "百合花園",
            "耽美花園",
            "偽娘哲學",
            "後宮閃光",
            "扶他樂園",
            "單行本",
            "姐姐系",
            "妹妹系",
            "SM",
            "性轉換",
            "足の恋",
            "人妻",
            "NTR",
            "強暴",
            "非人類",
            "艦隊收藏",
            "Love Live",
            "SAO 刀劍神域",
            "Fate",
            "東方",
            "WEBTOON",
            "禁書目錄",
            "歐美",
            "Cosplay",
            "重口地帶"
        )
        private var tagInitial = booleanArrayOf(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        )
    }

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
        viewModel.title = intent.getStringExtra("title")
        viewModel.value = intent.getStringExtra("value")

        //toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (viewModel.tag.equals("categories")
            || viewModel.tag.equals("search")
            || viewModel.tag.equals("favourite")
        ) {
            binding.comiclistSort.visibility = View.VISIBLE
        }

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
        val seals = ArrayList<CharSequence>()
        seals.addAll((tagInitial.indices).filter { tagInitial[it] }.map { tag[it] })
        mComicListAdapter.addSealData(seals)
        mComicListAdapter2.addSealData(seals)
        intiListener()
    }

    private fun intiListener() {
        binding.searchView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                //监听回车键
                val search = Search(binding.searchView.text.toString())
                viewModel.insertSearch(search)//添加搜索记录

                viewModel.value = binding.searchView.text.toString()
                viewModel.tag = "search"
                viewModel.page = 0
                viewModel.startpage = 0
                mComicListAdapter.clear()
                mComicListAdapter.notifyDataSetChanged()
                binding.comiclistRv.isEnabled = false//加载时不允许滑动，解决加载时滑动recyclerview报错
                binding.comiclistLoadLayout.visibility = ViewGroup.VISIBLE
                binding.comiclistLoadLayout.isEnabled = false
                showProgressBar(true, "")
                viewModel.getComicList()

            }
            false
        }

        binding.clearText.setOnClickListener {
            binding.searchView.setText("")
        }

        //排序方式
        binding.comiclistSort.setOnClickListener {
            val popupMenu = PopupMenu(this, it)
            if (viewModel.tag.equals("categories") || viewModel.tag.equals("search")) {
                popupMenu.menuInflater.inflate(R.menu.toolbar_menu_comiclist_sort, popupMenu.menu)
            }
            if (viewModel.tag.equals("favourite")) {
                popupMenu.menuInflater.inflate(R.menu.toolbar_menu_comiclist_sort2, popupMenu.menu)
            }
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.dd -> {
                        item.isChecked = true
                        setSort("dd", item.title.toString())
                    }

                    R.id.da -> {
                        item.isChecked = true
                        setSort("da", item.title.toString())
                    }

                    R.id.ld -> {
                        item.isChecked = true
                        setSort("ld", item.title.toString())
                    }

                    R.id.vd -> {
                        item.isChecked = true
                        setSort("vd", item.title.toString())
                    }
                }
                true
            }
            popupMenu.setOnDismissListener {

            }
        }

        //封印的标签 筛选
        binding.comiclistTag.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("封印")
                .setPositiveButton("确定") { dialog, which ->
                    val checkedItemPositions: SparseBooleanArray =
                        (dialog as AlertDialog).listView.checkedItemPositions

                    val result = ArrayList<CharSequence>()
                    for (i in tag.indices) {
                        if (checkedItemPositions.get(i)) {
                            tagInitial[i] = true//保存状态
                            result.add(tag[i])
                        } else {
                            tagInitial[i] = false//保存状态
                        }
                    }

                    setSeal(result)
                }
                .setNegativeButton("取消", null)
                .setMultiChoiceItems(tag, tagInitial, null)
                .show()
        }

        binding.comiclistPages.setOnClickListener {
            //修改页数点击没反应 扩大点击范围
            binding.comiclistPage.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.comiclistPage, 0)
        }
        //跳转页数
        binding.comiclistPage.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                //输入框回车键
                if (binding.comiclistPage.text.toString() != "") {
                    if (binding.comiclistPage.text.toString().toInt() == 0) {
                        binding.comiclistPage.setText("1")
                    }
                    var page = binding.comiclistPage.text.toString().toInt() - 1//因为网络请求时会加一，所以提前减一
                    if (page > viewModel.pages) {
                        //输入的页数大于当前页数时，修改成最大页数
                        page = viewModel.pages - 1 //网络请求时会加一
                        binding.comiclistPage.setText(viewModel.pages.toString())
                    }
                    if (viewModel.page != page) {
                        viewModel.startpage = page//起始页数
                        viewModel.page = page//当前页数
                        binding.comiclistRv.isEnabled = false//加载时不允许滑动，解决加载时滑动recyclerview报错
                        binding.comiclistLoadLayout.visibility = ViewGroup.VISIBLE
                        binding.comiclistLoadLayout.isEnabled = false
                        showProgressBar(true, "")

                        if (viewModel.tag.equals("random")) {
                            mComicListAdapter2.clear()
                            mComicListAdapter2.notifyDataSetChanged()
                            viewModel.getRandom()
                        } else {
                            mComicListAdapter.clear()
                            mComicListAdapter.notifyDataSetChanged()
                            viewModel.getComicList()
                        }
                    }
                }
            }
            false
        }

        binding.comiclistRv.setOnItemClickListener { v, position ->
            val intent = Intent(this@ComicListActivity, ComicInfoActivity::class.java)
            if (viewModel.tag.equals("random")) {
                val data = mComicListAdapter2.getItemData(position)
                intent.putExtra("id", data._id)
                intent.putExtra("fileserver", data.thumb.fileServer)
                intent.putExtra("imageurl", data.thumb.path)
                intent.putExtra("title", data.title)
                intent.putExtra("author", data.author)
                intent.putExtra("totalViews", data.totalViews.toString())

//                val imageView=v.findViewById<ImageView>(R.id.comiclist_item_image)
//                val titleView=v.findViewById<TextView>(R.id.comiclist_item_title)
//                val options = ActivityOptions.makeSceneTransitionAnimation(this,UtilPair.create(imageView, "image"), UtilPair.create(titleView, "title"))
//                startActivity(intent, options.toBundle())

            } else {
                val data = mComicListAdapter.getItemData(position)
                intent.putExtra("id", data._id)
                intent.putExtra("fileserver", data.thumb.fileServer)
                intent.putExtra("imageurl", data.thumb.path)
                intent.putExtra("title", data.title)
                intent.putExtra("author", data.author)
                intent.putExtra("totalViews", data.totalViews.toString())

//                val imageView=v.findViewById<ImageView>(R.id.comiclist_item_image)
//                val titleView=v.findViewById<TextView>(R.id.comiclist_item_title)
//                val options = ActivityOptions.makeSceneTransitionAnimation(this,UtilPair.create(imageView, "image"), UtilPair.create(titleView, "title"))
//                startActivity(intent, options.toBundle())

            }
            startActivity(intent)
        }
        binding.comiclistRv.setOnItemChildClickListener { view, position ->
            if (view.id == R.id.comiclist_item_image) {

                val intent = Intent(this@ComicListActivity, ImageActivity::class.java)
                if (viewModel.tag.equals("random")) {
                    val data = mComicListAdapter2.getItemData(position)
                    intent.putExtra("fileserver", data.thumb.fileServer)
                    intent.putExtra("imageurl", data.thumb.path)
                } else {
                    val data = mComicListAdapter.getItemData(position)
                    intent.putExtra("fileserver", data.thumb.fileServer)
                    intent.putExtra("imageurl", data.thumb.path)

                }
                val options = ActivityOptions.makeSceneTransitionAnimation(this, view, "image")
                startActivity(intent, options.toBundle())
            }
        }

        binding.comiclistRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val layoutManager = recyclerView.layoutManager
                if (layoutManager is LinearLayoutManager && !viewModel.tag.equals("random")) {
                    //获取最后一个可见item
                    val lastItemPosition = layoutManager.findLastVisibleItemPosition().toDouble()
                    //来显示当前页数
                    binding.comiclistPage.setText((ceil(lastItemPosition / viewModel.limit).toInt() + viewModel.startpage).toString())
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_comiclist, menu)

        //toolbar菜单创建完毕后更新ui
        if (viewModel.tag.equals("search")) {
            binding.toolbar.menu.findItem(R.id.action_search).isVisible = false
            binding.comiclistSearchLayout.isVisible = true
            binding.searchView.setText(viewModel.title)

        } else {
            if (viewModel.tag.equals("favourite")) {
                //我的收藏隐藏搜索图标
                binding.toolbar.menu.findItem(R.id.action_search).isVisible = false
            }
            binding.toolbar.title = viewModel.title
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }

            R.id.action_search -> {
                binding.toolbar.menu.findItem(R.id.action_search).isVisible = false
                binding.comiclistSearchLayout.isVisible = true
                //获取焦点 弹出软键盘
                binding.searchView.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchView, 0)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    //排序方式
    private fun setSort(sort: String, title: CharSequence) {
        binding.comiclistSort.text = title
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

    //筛选 封印
    private fun setSeal(seal: ArrayList<CharSequence>) {
        if (viewModel.tag.equals("random")) {
            mComicListAdapter2.addSealData(seal)
            mComicListAdapter2.notifyDataSetChanged()

        } else {
            mComicListAdapter.addSealData(seal)
            mComicListAdapter.notifyDataSetChanged()

        }
    }

    override fun initViewObservable() {
//        viewModel.liveData.observe(this) {
//            skeletonScreen.hide()
//            binding.comiclistRv.isEnabled = true
//            if (it.code == 200) {
//                binding.comiclistLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
//                mComicListAdapter.addData(it.data.comics.docs)
//                viewModel.pages = it.data.comics.pages//总页数
//                viewModel.limit = it.data.comics.limit//每页显示多少
//                binding.comiclistPages.text=" / ${it.data.comics.pages}页"//显示总页数
//                binding.comiclistPage.setText(it.data.comics.page.toString())//显示页数
//                if (it.data.comics.pages <= it.data.comics.page) {
//                    binding.comiclistRv.loadMoreEnd()//没有更多数据
//                } else {
//                    binding.comiclistRv.loadMoreComplete()//加载成功
//                }
//
//            } else {
//                if (viewModel.page <= 1) {//当首次加载时出现网络错误
//                    showProgressBar(
//                        false,
//                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
//                    )
//                } else {
//                    //当页面不是第一页时 网络错误可能是分页加载时出现的网络错误
//                    binding.comiclistRv.loadMoreFail()
//                }
//            }
//        }
        lifecycleScope.launch {
            viewModel.comicList.collect {
                skeletonScreen.hide()
                binding.comiclistRv.isEnabled = true
                when (it) {
                    is Result.Success -> {
                        binding.comiclistLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                        mComicListAdapter.addData(it.data.comics.docs)
                        viewModel.pages = it.data.comics.pages//总页数
                        viewModel.limit = it.data.comics.limit//每页显示多少
                        binding.comiclistPages.text = " / ${it.data.comics.pages}页"//显示总页数
                        binding.comiclistPage.setText(it.data.comics.page.toString())//显示页数
                        if (it.data.comics.pages <= it.data.comics.page) {
                            binding.comiclistRv.loadMoreEnd()//没有更多数据
                        } else {
                            binding.comiclistRv.loadMoreComplete()//加载成功
                        }
                    }

                    is Result.Error -> {
                        viewModel.page--
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

                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.comicList2.collect {
                skeletonScreen.hide()
                binding.comiclistRv.isEnabled = true
                when (it) {
                    is Result.Success -> {
                        mComicListAdapter2.addData(it.data.comics)
                        binding.comiclistLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                        binding.comiclistRv.loadMoreEnd()
                    }

                    is Result.Error -> {
                        showProgressBar(
                            false,
                            "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                        )
                    }

                    else -> {}
                }
            }
        }
//        viewModel.liveData2.observe(this) {
//            skeletonScreen.hide()
//            binding.comiclistRv.isEnabled = true
//            if (it.code == 200) {
//                mComicListAdapter2.addData(it.data.comics)
//                binding.comiclistLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
//                binding.comiclistRv.loadMoreEnd()
//            } else {
//
//                showProgressBar(
//                    false,
//                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
//                )
//            }
//        }


        binding.comiclistRv.setOnLoadMoreListener{
            viewModel.getComicList()
        }

        //网络重试点击事件监听
        binding.comiclistLoadLayout.setOnClickListener{
            skeletonScreen.show()
            showProgressBar(true, "")
            if (viewModel.tag.equals("random")) {
                viewModel.getRandom()
            } else {
                viewModel.getComicList()
            }

        }

        //大图PopupWindow
        mPopupWindow.setOnDismissListener{
            //恢复状态栏
            StatusBarUtil.show(this@ComicListActivity)
        }
        popupView.setOnClickListener{
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