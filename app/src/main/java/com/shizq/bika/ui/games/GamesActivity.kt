package com.shizq.bika.ui.games


import androidx.appcompat.app.AppCompatActivity

class GamesActivity : AppCompatActivity() {
//    private lateinit var mAdapter: GamesAdapter
//    private lateinit var skeletonScreen: ByRVItemSkeletonScreen
//
//    override fun initContentView(savedInstanceState: Bundle?): Int {
//        return R.layout.activity_games
//    }
//
//    override fun initVariableId(): Int {
//        return BR.viewModel
//    }
//
//    override fun initData() {
//        binding.gamesInclude.toolbar.title = "游戏区"
//        setSupportActionBar(binding.gamesInclude.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        mAdapter = GamesAdapter()
//        binding.gamesRv.layoutManager =GridLayoutManager(this,2)
//        skeletonScreen = BySkeleton
//            .bindItem(binding.gamesRv)
//            .adapter(mAdapter)
//            .load(R.layout.item_games_skeleton)
//            .duration(2000)
//            .count(10)
//            .show()
//
//        binding.gamesInclude2.loadLayout.isEnabled = false//加载时 view不可点击
//        viewModel.getGames()
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                finish()
//            }
//        }
//
//        return super.onOptionsItemSelected(item)
//    }
//
//    override fun initViewObservable() {
//        viewModel.liveData.observe(this) {
//            if (it.isOk) {
//                skeletonScreen.hide()
//                binding.gamesInclude2.loadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
//                mAdapter.addData(it.data.games.docs)
//                if (it.data.games.pages == it.data.games.page) {
//                    binding.gamesRv.loadMoreEnd()//没有更多数据
//                } else {
//                    binding.gamesRv.loadMoreComplete()//加载成功
//                }
//
//            } else {
//                if (viewModel.page <= 1) {
//                    //当第一页加载时出现网络错误
//                    if (it.data == null) {
//                        showProgressBar(false, "网络错误，点击重试")
//                    } else {
//                        showProgressBar(false, "网络错误，点击重试\n${it.message}")
//                    }
//                } else {
//                    binding.gamesInclude2.loadLayout.visibility = ViewGroup.GONE
//                    binding.gamesRv.loadMoreFail()
//                }
//            }
//        }
//
//        binding.gamesRv.setOnItemClickListener { v, position ->
//            val intent = Intent(this, GameInfoActivity::class.java)
//            intent.putExtra("gameId", mAdapter.getItemData(position)._id)
//            startActivity(intent)
//        }
//
//        binding.gamesRv.setOnLoadMoreListener {
//            viewModel.getGames()
//        }
//
//        //网络重试点击事件监听
//        binding.gamesInclude2.loadLayout.setOnClickListener {
//            skeletonScreen.show()
//            showProgressBar(true, "")
//            viewModel.getGames()
//        }
//    }
//
//    private fun showProgressBar(show: Boolean, string: String) {
//        binding.gamesInclude2.loadProgressBar.visibility =
//            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
//        binding.gamesInclude2.loadError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
//        binding.gamesInclude2.loadText.text = string
//        binding.gamesInclude2.loadLayout.isEnabled = !show
//    }
//}
}