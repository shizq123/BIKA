package com.shizq.bika.ui.main

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.CategoriesAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityMainBinding
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.ui.apps.AppsActivity
import com.shizq.bika.ui.chatroom.current.chatroomlist.ChatRoomListActivity
import com.shizq.bika.ui.collections.CollectionsActivity
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.games.GamesActivity
import com.shizq.bika.ui.history.HistoryActivity
import com.shizq.bika.ui.image.ImageActivity
import com.shizq.bika.ui.leaderboard.LeaderboardActivity
import com.shizq.bika.ui.mycomments.MyCommentsActivity
import com.shizq.bika.ui.notifications.NotificationsActivity
import com.shizq.bika.ui.search.SearchActivity
import com.shizq.bika.ui.settings.SettingsActivity
import com.shizq.bika.ui.user.UserActivity
import com.shizq.bika.utils.*
import me.jingbin.library.skeleton.ByRVItemSkeletonScreen
import me.jingbin.library.skeleton.BySkeleton


class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {
    private lateinit var adapter_categories: CategoriesAdapter
    private lateinit var skeletonScreen: ByRVItemSkeletonScreen

    private val ERROR_PROFILE = 0
    private val ERROR_CATEGORIES = 1
    private var ERROR = this.ERROR_PROFILE //这里标记哪个网络请求异常

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.toolbar.title = "哔咔"
        setSupportActionBar(binding.toolbar)

        adapter_categories = CategoriesAdapter()
        binding.mainRv.layoutManager = GridLayoutManager(
            this@MainActivity,
            if (getWindowWidth() > getWindowHeight()) 6 else 3
        )//初步适配平板（没卵用
        skeletonScreen = BySkeleton
            .bindItem(binding.mainRv)
            .adapter(adapter_categories)// 必须设置adapter，且在此之前不要设置adapter
            .load(R.layout.item_categories_skeleton)// item骨架图
            .duration(2000)// 微光一次显示时间
            .count(18)// item个数
            .show()

        initListener()

        viewModel.getUpdate()

        if (adapter_categories.data.size < 1) {
            //防止重复加载
            showProgressBar(true, "检查账号信息...")
            viewModel.getProfile() //先获得个人信息
        }
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        //页面显示时调用
        if (isTopResumedActivity) {
            initProfile()//显示用户信息
        }
    }

    //显示用户信息
    private fun initProfile() {
        viewModel.fileServer = SPUtil.get(this, "user_fileServer", "") as String
        viewModel.path = SPUtil.get(this, "user_path", "") as String
        val character = SPUtil.get(this, "user_character", "") as String
        val name = SPUtil.get(this, "user_name", "") as String
        val gender = SPUtil.get(this, "user_gender", "") as String
        val level = SPUtil.get(this, "user_level", 1) as Int
        val exp = SPUtil.get(this, "user_exp", 0) as Int
        val slogan = SPUtil.get(this, "user_slogan", "") as String

        //头像
        GlideApp.with(this@MainActivity)
            .load(GlideUrlNewKey(viewModel.fileServer, viewModel.path))
            .centerCrop()
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(
                binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_imageView) as ImageView
            )
        //头像框
        GlideApp.with(this@MainActivity)
            .load(character)
            .into(
                binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_character) as ImageView
            )
        //用户名
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_name) as TextView).text = name
        //等级
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_user_ver) as TextView).text =
            "Lv.$level($exp/${exp(level)})"
        //性别
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_gender) as TextView).text =
            when (gender) {
                "m" -> "(绅士)"
                "f" -> "(淑女)"
                else -> "(机器人)"
            }
        //title
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_title) as TextView).text =
            SPUtil.get(this, "user_title", "萌新") as String
        //自我介绍 签名
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_slogan) as TextView).text =
            if (slogan == "") resources.getString(R.string.slogan) else slogan
    }

    override fun onResume() {
        super.onResume()
        binding.mainNavView.setCheckedItem(R.id.drawer_menu_home)
    }

    //toolbar菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                startActivity(SearchActivity::class.java)
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun initListener() {
        //侧滑
        binding.drawerLayout.addDrawerListener(
            ActionBarDrawerToggle(
                this@MainActivity,
                binding.drawerLayout,
                binding.toolbar,
                R.string.drawer_show,
                R.string.drawer_hide
            )
        )
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_modify) as TextView).setOnClickListener {
            startActivity(Intent(this@MainActivity, UserActivity::class.java))
        }
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_punch_in) as TextView).setOnClickListener {
            (binding.mainNavView.getHeaderView(0)
                .findViewById(R.id.main_drawer_punch_in) as TextView).visibility=View.GONE
            viewModel.punch_In()
        }
        (binding.mainNavView.getHeaderView(0)
            .findViewById(R.id.main_drawer_character) as ImageView).setOnClickListener {
            if (viewModel.userId != "" && viewModel.fileServer != "") {
                //判断用户是否登录是否有头像，是就查看头像大图
                val intent = Intent(this, ImageActivity::class.java)
                intent.putExtra("fileserver", viewModel.fileServer)
                intent.putExtra("imageurl", viewModel.path)
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    this,
                    (binding.mainNavView.getHeaderView(0)
                        .findViewById(R.id.main_drawer_imageView) as ImageView),
                    "image"
                )
                startActivity(intent, options.toBundle())
            }
        }
        binding.mainNavView.setNavigationItemSelectedListener {
            binding.mainNavView.setCheckedItem(it)
            when (it.itemId) {
                R.id.drawer_menu_history -> {
                    startActivity(HistoryActivity::class.java)
                }
                R.id.drawer_menu_bookmark -> {
                    val intent = Intent(this@MainActivity, ComicListActivity::class.java)
                    intent.putExtra("tag", "favourite")
                    intent.putExtra("title", "我的收藏")
                    intent.putExtra("value", "我的收藏")
                    startActivity(intent)

                }
                R.id.drawer_menu_mail -> {
                    startActivity(NotificationsActivity::class.java)

                }
                R.id.drawer_menu_chat -> {
                    startActivity(MyCommentsActivity::class.java)

                }
                R.id.drawer_menu_settings -> {
                    startActivity(SettingsActivity::class.java)
                }

            }
            true
        }

        binding.mainRv.setOnItemClickListener { v, position ->
            val intent = Intent(this, ComicListActivity::class.java)
            val datas = adapter_categories.getItemData(position)
            when (position) {
                0 -> {
                    startActivity(Intent(this, CollectionsActivity::class.java))
                }
                1 -> {
                    startActivity(Intent(this, LeaderboardActivity::class.java))
                }
                2 -> {
                    startActivity(Intent(this, GamesActivity::class.java))
                }
                3 -> {
                    startActivity(Intent(this, AppsActivity::class.java))
                }
                4 -> {
                    startActivity(Intent(this, ChatRoomListActivity::class.java))
                }
                5 -> {
                    val intentComments = Intent(this, CommentsActivity::class.java)
                    intentComments.putExtra("id", "5822a6e3ad7ede654696e482")
                    intentComments.putExtra("comics_games", "comics")
                    startActivity(intentComments)
                }
                6 -> {
                    intent.putExtra("tag", "latest")
                    intent.putExtra("title", datas.title)
                    intent.putExtra("value", datas.title)
                    startActivity(intent)
                }
                7 -> {
                    intent.putExtra("tag", "random")
                    intent.putExtra("title", datas.title)
                    intent.putExtra("value", datas.title)
                    startActivity(intent)
                }
                else -> {
                    if (datas.isWeb) {
                        val intent = Intent()
                        intent.action = "android.intent.action.VIEW"
                        intent.data = Uri.parse(
                            "${datas.link}/?token=${
                                SPUtil.get(
                                    this,
                                    "token",
                                    ""
                                )
                            }&secret=pb6XkQ94iBBny1WUAxY0dY5fksexw0dt"
                        )
                        startActivity(intent)
                    } else {
                        intent.putExtra("tag", "categories")
                        intent.putExtra("title", datas.title)
                        intent.putExtra("value", datas.title)
                        startActivity(intent)
                    }
                }
            }

        }
        //网络重试点击事件监听
        binding.mainLoadLayout.setOnClickListener {
            skeletonScreen.show()
            if (ERROR == ERROR_PROFILE) {
                showProgressBar(true, "检查账号信息...")
                viewModel.getProfile()
            } else {
                showProgressBar(true, "获取主页信息...")
                viewModel.getCategories()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initViewObservable() {
        //user信息
        viewModel.liveData_profile.observe(this) {
            if (it.code == 200) {
                var fileServer = ""
                var path = ""
                var character = ""
                viewModel.userId = it.data.user._id

                if (it.data.user.avatar != null) {//头像
                    fileServer = it.data.user.avatar.fileServer
                    path = it.data.user.avatar.path
                    GlideApp.with(this@MainActivity)
                        .load(
                            GlideUrlNewKey(
                                fileServer,
                                path
                            )
                        )
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .into(
                            binding.mainNavView.getHeaderView(0)
                                .findViewById(R.id.main_drawer_imageView) as ImageView
                        )
                }
                if (it.data.user.character != null) { //头像框 新用户没有

                    character = it.data.user.character
                    GlideApp.with(this@MainActivity)
                        .load(character)
                        .into(
                            binding.mainNavView.getHeaderView(0)
                                .findViewById(R.id.main_drawer_character) as ImageView
                        )
                }

                val name = it.data.user.name
                (binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_name) as TextView).text = name //用户名

                val level = it.data.user.level//等级
                (binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_user_ver) as TextView).text =
                    "Lv.${it.data.user.level}(${it.data.user.exp}/${exp(it.data.user.level)})"//等级

                (binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_title) as TextView).text = it.data.user.title//称号

                //性别
                val gender = it.data.user.gender
                (binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_gender) as TextView).text =
                    when (it.data.user.gender) {
                        "m" -> "(绅士)"
                        "f" -> "(淑女)"
                        else -> "(机器人)"
                    }

                //用户签名
                val slogan = if (it.data.user.slogan.isNullOrBlank()) "" else it.data.user.slogan
                (binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_slogan) as TextView).text =
                    if (slogan == "") resources.getString(R.string.slogan) else slogan

                if (!it.data.user.isPunched) {//当前用户未打卡时
                    //是否设置自动打卡
                    if (SPUtil.get(this, "setting_punch", true) as Boolean) {
                        viewModel.punch_In()
                    } else {
                        (binding.mainNavView.getHeaderView(0)
                            .findViewById(R.id.main_drawer_punch_in) as TextView).visibility=View.VISIBLE
                    }
                }

                //存一下当前用户信息 用于显示个人评论
                SPUtil.put(this, "user_fileServer", fileServer)
                SPUtil.put(this, "user_path", path)
                SPUtil.put(this, "user_character", character)
                SPUtil.put(this, "user_name", name)
                SPUtil.put(this, "user_birthday", it.data.user.birthday)
                SPUtil.put(this, "user_created_at", it.data.user.created_at)
                SPUtil.put(this, "user_gender", gender)
                SPUtil.put(this, "user_level", level)
                SPUtil.put(this, "user_exp", it.data.user.exp)
                SPUtil.put(this, "user_title", it.data.user.title)
                SPUtil.put(this, "user_slogan", slogan)
                SPUtil.put(this, "user_id", it.data.user._id)
                SPUtil.put(this, "user_verified", it.data.user.verified)

                if (viewModel.cList().size <= 10) {
                    //更换头像会重新加载个人信息 防止重复加载
                    showProgressBar(true, "获取主页信息...")
                    viewModel.getCategories() //获得主页信息
                }

            } else if (it.code == 401) {
                if (it.error == "1005") {
                    //token 过期 进行自动登录
                    showProgressBar(true, "账号信息已过期，进行自动登录...")
                    viewModel.getSignIn()//自动登录 重新获取token
                }
            } else {
                ERROR = this.ERROR_PROFILE
                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                )
            }
        }

        //自动登录 只要是网络错误请求失败全跳转到登录页
        viewModel.liveData_signin.observe(this) {
            if (it.code == 200) {
                //登录成功 保存token
//                MmkvUtils.putSet("token", bean.data.token)
                SPUtil.put(this, "token", it.data.token)
                showProgressBar(true, "登录成功，检查账号信息...")
                viewModel.getProfile() //重新加载数据

            } else if (it.code == 400) {
                if (it.error == "1004") {
                    //登录失败 账号或密码错误 跳转到登录页面AccountActivity
                    Toast.makeText(this, "自动登录失败 账号或密码错误", Toast.LENGTH_SHORT).show()
                    startActivity(AccountActivity::class.java)
                    finish()
                }
            } else {
                //网络错误 登录失败 跳转到登录页面AccountActivity
                Toast.makeText(
                    this,
                    "自动登录失败code=${it.code} error=${it.error} message=${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(AccountActivity::class.java)
                finish()
            }
        }


        //加载主页
        viewModel.liveData.observe(this) {
            skeletonScreen.hide()
            if (it.code == 200) {
                binding.mainRv.loadMoreEnd()
                binding.mainLoadLayout.visibility = ViewGroup.GONE
                viewModel.categoriesList = viewModel.cList()
                viewModel.categoriesList.addAll(it.data.categories)
                if (adapter_categories.data.size < 1) {
                    //防止重复添加
                    adapter_categories.addData(viewModel.categoriesList)
                }
            } else {
                ERROR = this.ERROR_CATEGORIES
                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                )
            }

        }


        //打卡签到
        viewModel.liveData_punch_in.observe(this) {
            if (it.code == 200) {
                //打卡成功 手动加经验来保存
                var exp = SPUtil.get(this, "user_exp", 0) as Int
                var level = SPUtil.get(this, "user_level", 1) as Int
                exp += 10
                if (exp > exp(level)) {
                    level += 1
                }
                //保存
                SPUtil.put(this, "user_level", level)
                SPUtil.put(this, "user_exp", exp)
                initProfile()
                Toast.makeText(this, "自动打卡成功", Toast.LENGTH_SHORT).show()
            } else {
                (binding.mainNavView.getHeaderView(0)
                    .findViewById(R.id.main_drawer_punch_in) as TextView).visibility=View.VISIBLE
                Toast.makeText(
                    this,
                    "打卡失败message=${it.message} code=${it.code} error=${it.error}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //更新
        viewModel.liveData_update.observe(this) {
            if (it != null && it.version.toInt() > AppVersion().code()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("新版本 v${it.short_version}")
                    .setMessage(it.release_notes)
                    .setPositiveButton("更新") { _, _ ->
                        val intent = Intent()
                        intent.action = "android.intent.action.VIEW"
                        intent.data = Uri.parse(it.download_url)
                        startActivity(intent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }


    }

    private fun showProgressBar(show: Boolean, string: String) {
        binding.mainLoadProgressBar.visibility = if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.loadCategoriesError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.mainLoadText.text = string
        binding.mainLoadLayout.isEnabled = !show
    }

    private fun exp(i: Int): Int {
        //等级计算是反编译源码找到的
        return 100 * i * i + (100 * i)
    }

    private fun getWindowHeight(): Int {
        return resources.displayMetrics.heightPixels
    }

    private fun getWindowWidth(): Int {
        return resources.displayMetrics.widthPixels
    }
}