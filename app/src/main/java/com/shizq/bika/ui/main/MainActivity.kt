package com.shizq.bika.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DashboardScreen()
        }
    }

    @Composable
    fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
        val dashboardSections by viewModel.sectionsFlow.collectAsStateWithLifecycle()
        DashboardContent(dashboardSections)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardContent(dashboardState: DashboardUiState) {
        val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DashboardDrawerContent()
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("哔咔") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
//                                context.startActivity(Intent(context, SearchActivity::class.java))
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                when (dashboardState) {
                    is DashboardUiState.Error -> {}
                    DashboardUiState.Loading -> {}
                    is DashboardUiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            items(dashboardState.sections) { section ->
                                when (section) {
                                    is Section.Local -> FeatureEntry(
                                        section.imageResId,
                                        section.titleResId
                                    ) {

                                    }

                                    is Section.Remote -> FeatureEntry(
                                        section.imageUrl,
                                        section.title,
                                    ) {

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DashboardDrawerContent() {

    }

//    private lateinit var adapter_categories: CategoriesAdapter
//    private lateinit var skeletonScreen: ByRVItemSkeletonScreen
//
//    private val ERROR_PROFILE = 0
//    private val ERROR_CATEGORIES = 1
//    private var ERROR = this.ERROR_PROFILE //这里标记哪个网络请求异常
//
//    override fun initContentView(savedInstanceState: Bundle?): Int {
//        return R.layout.activity_main
//    }
//
//    override fun initVariableId(): Int {
//        return BR.viewModel
//    }
//
//    override fun initData() {
//        binding.toolbar.title = "哔咔"
//        setSupportActionBar(binding.toolbar)
//
//        adapter_categories = CategoriesAdapter()
//        binding.mainRv.layoutManager = GridLayoutManager(
//            this@MainActivity,
//            if (getWindowWidth() > getWindowHeight()) 6 else 3
//        )//初步适配平板（没卵用
//        skeletonScreen = BySkeleton
//            .bindItem(binding.mainRv)
//            .adapter(adapter_categories)// 必须设置adapter，且在此之前不要设置adapter
//            .load(R.layout.item_categories_skeleton)// item骨架图
//            .duration(2000)// 微光一次显示时间
//            .count(18)// item个数
//            .show()
//
//        initListener()
//
//        if (adapter_categories.data.size < 1) {
//            //防止重复加载
//            showProgressBar(true, "检查账号信息...")
//            viewModel.getProfile() //先获得个人信息
//        }
//    }
//
//    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
//        super.onTopResumedActivityChanged(isTopResumedActivity)
//        //页面显示时调用
//        if (isTopResumedActivity) {
//            initProfile()//显示用户信息
//        }
//    }
//
//    //显示用户信息
//    private fun initProfile() {
//        viewModel.fileServer = SPUtil.get("user_fileServer", "") as String
//        viewModel.path = SPUtil.get("user_path", "") as String
//        val character = SPUtil.get("user_character", "") as String
//        val name = SPUtil.get("user_name", "") as String
//        val gender = SPUtil.get("user_gender", "") as String
//        val level = SPUtil.get("user_level", 1) as Int
//        val exp = SPUtil.get("user_exp", 0) as Int
//        val slogan = SPUtil.get("user_slogan", "") as String
//
//        //头像
//        Glide.with(this@MainActivity)
//            .load(GlideUrlNewKey(viewModel.fileServer, viewModel.path))
//            .centerCrop()
//            .placeholder(R.drawable.placeholder_avatar_2)
//            .into(
//                binding.mainNavView.getHeaderView(0)
//                    .findViewById<ImageView>(R.id.main_drawer_imageView)!!
//            )
//        //头像框
//        Glide.with(this@MainActivity)
//            .load(character)
//            .into(
//                binding.mainNavView.getHeaderView(0)
//                    .findViewById<ImageView>(R.id.main_drawer_character)!!
//            )
//        //用户名
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_name)!!).text = name
//        //等级
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_user_ver)!!).text =
//            "Lv.$level($exp/${exp(level)})"
//        //性别
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_gender)!!).text =
//            when (gender) {
//                "m" -> "(绅士)"
//                "f" -> "(淑女)"
//                else -> "(机器人)"
//            }
//        //title
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_title)!!).text =
//            SPUtil.get("user_title", "萌新") as String
//        //自我介绍 签名
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_slogan)!!).text =
//            if (slogan == "") resources.getString(R.string.slogan) else slogan
//    }
//
//    override fun onResume() {
//        super.onResume()
//        binding.mainNavView.setCheckedItem(R.id.drawer_menu_home)
//    }
//
//    //toolbar菜单
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.toolbar_menu_main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.action_search -> {
//                startActivity(SearchActivity::class.java)
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//
//    private fun initListener() {
//        //侧滑
//        binding.drawerLayout.addDrawerListener(
//            ActionBarDrawerToggle(
//                this@MainActivity,
//                binding.drawerLayout,
//                binding.toolbar,
//                R.string.drawer_show,
//                R.string.drawer_hide
//            )
//        )
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_modify)!!).setOnClickListener {
//            startActivity(Intent(this@MainActivity, UserActivity::class.java))
//        }
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_punch_in)!!).setOnClickListener {
//            (binding.mainNavView.getHeaderView(0)
//                .findViewById<TextView>(R.id.main_drawer_punch_in)!!).visibility=View.GONE
//            viewModel.punch_In()
//        }
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<ImageView>(R.id.main_drawer_character)!!).setOnClickListener {
//            if (viewModel.userId != "" && viewModel.fileServer != "") {
//                //判断用户是否登录是否有头像，是就查看头像大图
//                val intent = Intent(this, ImageActivity::class.java)
//                intent.putExtra("fileserver", viewModel.fileServer)
//                intent.putExtra("imageurl", viewModel.path)
//                val options = ActivityOptions.makeSceneTransitionAnimation(
//                    this,
//                    (binding.mainNavView.getHeaderView(0)
//                        .findViewById<ImageView>(R.id.main_drawer_imageView)!!),
//                    "image"
//                )
//                startActivity(intent, options.toBundle())
//            }
//        }
//        binding.mainNavView.setNavigationItemSelectedListener {
//            binding.mainNavView.setCheckedItem(it)
//            when (it.itemId) {
//                R.id.drawer_menu_history -> {
//                    startActivity(HistoryActivity::class.java)
//                }
//                R.id.drawer_menu_bookmark -> {
//                    val intent = Intent(this@MainActivity, ComicListActivity::class.java)
//                    intent.putExtra("tag", "favourite")
//                    intent.putExtra("title", "我的收藏")
//                    intent.putExtra("value", "我的收藏")
//                    startActivity(intent)
//
//                }
//                R.id.drawer_menu_mail -> {
//                    startActivity(NotificationsActivity::class.java)
//
//                }
//                R.id.drawer_menu_chat -> {
//                    startActivity(MyCommentsActivity::class.java)
//
//                }
//                R.id.drawer_menu_settings -> {
//                    startActivity(SettingsActivity::class.java)
//                }
//
//            }
//            true
//        }
//
//        binding.mainRv.setOnItemClickListener { v, position ->
//            val intent = Intent(this, ComicListActivity::class.java)
//            val datas = adapter_categories.getItemData(position)
//            when (datas.imageRes) {
//                //根据ResId来判断 以后改
//                R.drawable.bika -> {
//                    startActivity(Intent(this, CollectionsActivity::class.java))
//                }
//                R.drawable.cat_leaderboard -> {
//                    startActivity(Intent(this, LeaderboardActivity::class.java))
//                }
//                R.drawable.cat_game -> {
//                    startActivity(Intent(this, GamesActivity::class.java))
//                }
//                R.drawable.cat_love_pica -> {
//                    startActivity(Intent(this, AppsActivity::class.java))
//                }
//                R.drawable.ic_chat -> {
//                    startActivity(Intent(this, ChatRoomListActivity::class.java))
//                }
//                R.drawable.cat_forum -> {
//                    val intentComments = Intent(this, CommentsActivity::class.java)
//                    intentComments.putExtra("id", "5822a6e3ad7ede654696e482")
//                    intentComments.putExtra("comics_games", "comics")
//                    startActivity(intentComments)
//                }
//                R.drawable.cat_latest -> {
//                    intent.putExtra("tag", "latest")
//                    intent.putExtra("title", datas.title)
//                    intent.putExtra("value", datas.title)
//                    startActivity(intent)
//                }
//                R.drawable.cat_random -> {
//                    intent.putExtra("tag", "random")
//                    intent.putExtra("title", datas.title)
//                    intent.putExtra("value", datas.title)
//                    startActivity(intent)
//                }
//                else -> {
//                    if (datas.isWeb) {
//                        val intent = Intent()
//                        intent.action = "android.intent.action.VIEW"
//                        intent.data = Uri.parse(
//                            "${datas.link}/?token=${
//                                SPUtil.get(
//                                    "token",
//                                    ""
//                                )
//                            }&secret=pb6XkQ94iBBny1WUAxY0dY5fksexw0dt"
//                        )
//                        startActivity(intent)
//                    } else {
//                        intent.putExtra("tag", "categories")
//                        intent.putExtra("title", datas.title)
//                        intent.putExtra("value", datas.title)
//                        startActivity(intent)
//                    }
//                }
//            }
//
//        }
//        //网络重试点击事件监听
//        binding.mainLoadLayout.setOnClickListener {
//            skeletonScreen.show()
//            if (ERROR == ERROR_PROFILE) {
//                showProgressBar(true, "检查账号信息...")
//                viewModel.getProfile()
//            } else {
//                showProgressBar(true, "获取主页信息...")
//                viewModel.getCategories()
//            }
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    override fun initViewObservable() {
//        //user信息
//        viewModel.liveData_profile.observe(this) {
//            if (it.code == 200) {
//                var fileServer = ""
//                var path = ""
//                var character = ""
//                viewModel.userId = it.data.user._id
//
//                if (it.data.user.avatar != null) {//头像
//                    fileServer = it.data.user.avatar.fileServer
//                    path = it.data.user.avatar.path
//                    Glide.with(this@MainActivity)
//                        .load(
//                            GlideUrlNewKey(
//                                fileServer,
//                                path
//                            )
//                        )
//                        .centerCrop()
//                        .placeholder(R.drawable.placeholder_avatar_2)
//                        .into(
//                            binding.mainNavView.getHeaderView(0)
//                                .findViewById<ImageView>(R.id.main_drawer_imageView)!!
//                        )
//                }
//                if (it.data.user.character != null) { //头像框 新用户没有
//
//                    character = it.data.user.character
//                    Glide.with(this@MainActivity)
//                        .load(character)
//                        .into(
//                            binding.mainNavView.getHeaderView(0)
//                                .findViewById<ImageView>(R.id.main_drawer_character)!!
//                        )
//                }
//
//                val name = it.data.user.name
//                (binding.mainNavView.getHeaderView(0)
//                    .findViewById<TextView>(R.id.main_drawer_name)!!).text = name //用户名
//
//                val level = it.data.user.level//等级
//                (binding.mainNavView.getHeaderView(0)
//                    .findViewById<TextView>(R.id.main_drawer_user_ver)!!).text =
//                    "Lv.${it.data.user.level}(${it.data.user.exp}/${exp(it.data.user.level)})"//等级
//
//                (binding.mainNavView.getHeaderView(0)
//                    .findViewById<TextView>(R.id.main_drawer_title)!!).text = it.data.user.title//称号
//
//                //性别
//                val gender = it.data.user.gender
//                (binding.mainNavView.getHeaderView(0)
//                    .findViewById<TextView>(R.id.main_drawer_gender)!!).text =
//                    when (it.data.user.gender) {
//                        "m" -> "(绅士)"
//                        "f" -> "(淑女)"
//                        else -> "(机器人)"
//                    }
//
//                //用户签名
//                val slogan = if (it.data.user.slogan.isNullOrBlank()) "" else it.data.user.slogan
//                (binding.mainNavView.getHeaderView(0)
//                    .findViewById<TextView>(R.id.main_drawer_slogan)!!).text =
//                    if (slogan == "") resources.getString(R.string.slogan) else slogan
//
//                if (!it.data.user.isPunched) {//当前用户未打卡时
//                    //是否设置自动打卡
//                    if (SPUtil.get("setting_punch", true) as Boolean) {
//                        viewModel.punch_In()
//                    } else {
//                        (binding.mainNavView.getHeaderView(0)
//                            .findViewById<TextView>(R.id.main_drawer_punch_in)!!).visibility=View.VISIBLE
//                    }
//                }
//
//                //存一下当前用户信息 用于显示个人评论
//                SPUtil.put("user_fileServer", fileServer)
//                SPUtil.put("user_path", path)
//                SPUtil.put("user_character", character)
//                SPUtil.put("user_name", name)
//                SPUtil.put("user_birthday", it.data.user.birthday)
//                SPUtil.put("user_created_at", it.data.user.created_at)
//                SPUtil.put("user_gender", gender)
//                SPUtil.put("user_level", level)
//                SPUtil.put("user_exp", it.data.user.exp)
//                SPUtil.put("user_title", it.data.user.title)
//                SPUtil.put("user_slogan", slogan)
//                SPUtil.put("user_id", it.data.user._id)
//                SPUtil.put("user_verified", it.data.user.verified)
//
//                if (viewModel.cList().size <= 10) {
//                    //更换头像会重新加载个人信息 防止重复加载
//                    showProgressBar(true, "获取主页信息...")
//                    viewModel.getCategories() //获得主页信息
//                }
//
//            } else if (it.code == 401) {
//                if (it.error == "1005") {
//                    //token 过期 进行自动登录
//                    showProgressBar(true, "账号信息已过期，进行自动登录...")
//                    viewModel.getSignIn()//自动登录 重新获取token
//                }
//            } else {
//                ERROR = this.ERROR_PROFILE
//                showProgressBar(
//                    false,
//                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
//                )
//            }
//        }
//
//        //自动登录 只要是网络错误请求失败全跳转到登录页
//        viewModel.liveData_signin.observe(this) {
//            if (it.code == 200) {
//                //登录成功 保存token
////                MmkvUtils.putSet("token", bean.data.token)
//                SPUtil.put("token", it.data.token)
//                showProgressBar(true, "登录成功，检查账号信息...")
//                viewModel.getProfile() //重新加载数据
//
//            } else if (it.code == 400) {
//                if (it.error == "1004") {
//                    //登录失败 账号或密码错误 跳转到登录页面AccountActivity
//                    Toast.makeText(this, "自动登录失败 账号或密码错误", Toast.LENGTH_SHORT).show()
//                    startActivity(AccountActivity::class.java)
//                    finish()
//                }
//            } else {
//                //网络错误 登录失败 跳转到登录页面AccountActivity
//                Toast.makeText(
//                    this,
//                    "自动登录失败code=${it.code} error=${it.error} message=${it.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//                startActivity(AccountActivity::class.java)
//                finish()
//            }
//        }
//
//
//        //加载主页
//        viewModel.liveData.observe(this) {
//            skeletonScreen.hide()
//            if (it.code == 200) {
//                binding.mainRv.loadMoreEnd()
//                binding.mainLoadLayout.visibility = ViewGroup.GONE
//                viewModel.categoriesList = viewModel.cList()
//                viewModel.categoriesList.addAll(it.data.categories)
//                if (adapter_categories.data.size < 1) {
//                    //防止重复添加
//                    adapter_categories.addData(viewModel.categoriesList)
//                }
//            } else {
//                ERROR = this.ERROR_CATEGORIES
//                showProgressBar(
//                    false,
//                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
//                )
//            }
//
//        }
//
//
//        //打卡签到
//        viewModel.liveData_punch_in.observe(this) {
//            if (it.code == 200) {
//                //打卡成功 手动加经验来保存
//                var exp = SPUtil.get("user_exp", 0) as Int
//                var level = SPUtil.get("user_level", 1) as Int
//                exp += 10
//                if (exp > exp(level)) {
//                    level += 1
//                }
//                //保存
//                SPUtil.put("user_level", level)
//                SPUtil.put("user_exp", exp)
//                initProfile()
//                Toast.makeText(this, "自动打卡成功", Toast.LENGTH_SHORT).show()
//            } else {
//                (binding.mainNavView.getHeaderView(0)
//                    .findViewById<TextView>(R.id.main_drawer_punch_in)!!).visibility=View.VISIBLE
//                Toast.makeText(
//                    this,
//                    "打卡失败message=${it.message} code=${it.code} error=${it.error}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//    }
//
//    private fun showProgressBar(show: Boolean, string: String) {
//        binding.mainLoadProgressBar.visibility = if (show) ViewGroup.VISIBLE else ViewGroup.GONE
//        binding.loadCategoriesError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
//        binding.mainLoadText.text = string
//        binding.mainLoadLayout.isEnabled = !show
//    }
//
//    private fun exp(i: Int): Int {
//        //等级计算是反编译源码找到的
//        return 100 * i * i + (100 * i)
//    }
//
//    private fun getWindowHeight(): Int {
//        return resources.displayMetrics.heightPixels
//    }
//
//    private fun getWindowWidth(): Int {
//        return resources.displayMetrics.widthPixels
//    }
}