package com.shizq.bika.ui.games

import androidx.appcompat.app.AppCompatActivity

class GameInfoActivity : AppCompatActivity() {
//    private lateinit var mAdapter: GameScreenshotAdapter
//    var gameLink: String = ""
//    var videoLink: String = ""
//
//    private lateinit var popupView: View
//    private lateinit var popupImage: ImageView
//    private lateinit var mPopupWindow: PopupWindow
//
//    override fun initContentView(savedInstanceState: Bundle?): Int {
//        return R.layout.activity_game_info
//    }
//
//    override fun initVariableId(): Int {
//        return BR.viewModel
//    }
//
//    override fun initData() {
//        viewModel.gameId = intent.getStringExtra("gameId")
//
//        //点击事件
//        binding.clickListener = ClickListener()
//
//        binding.gameinfoInclude.toolbar.title = "游戏介绍"
//        setSupportActionBar(binding.gameinfoInclude.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        val lm = LinearLayoutManager(this)
//        lm.orientation = LinearLayoutManager.HORIZONTAL
//        binding.gameImageList.layoutManager = lm
//        mAdapter = GameScreenshotAdapter(this)
//        binding.gameImageList.adapter = mAdapter
//
//        //PopupWindow显示大图片
//        popupView = View.inflate(this, R.layout.view_popup_image, null)
//        popupImage = popupView.findViewById(R.id.popup_image)
//        mPopupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
//        mPopupWindow.isOutsideTouchable = true
//        mPopupWindow.isClippingEnabled = false
//
//        viewModel.getGameInfo()
//
//        mAdapter.setOnItemClickListener { v, data ->
//
//            val intent = Intent(this, ImageActivity::class.java)
//            intent.putExtra("fileserver", data.fileServer)
//            intent.putExtra("imageurl", data.path)
//            val options = ActivityOptions.makeSceneTransitionAnimation(this, v, "image")
//            startActivity(intent, options.toBundle())
//        }
//
//        mPopupWindow.setOnDismissListener {
//            //恢复状态栏
//            StatusBarUtil.show(this)
//        }
//        popupView.setOnClickListener {
//            mPopupWindow.dismiss()
//        }
//
//    }
//
//    //toolbar菜单
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.toolbar_menu_gameinfo, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                finish()
//            }
//            R.id.action_video -> {
//                if (videoLink != "") {
//                    val intent = Intent()
//                    intent.action = "android.intent.action.VIEW"
//                    intent.data = Uri.parse(videoLink)
//                    startActivity(intent)
//                } else {
//                    Toast.makeText(this,"没有视频介绍",Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//        return super.onOptionsItemSelected(item)
//    }
//
//    inner class ClickListener {
//        fun Comment() {//评论
//            val intentComments = Intent(this@GameInfoActivity, CommentsActivity::class.java)
//            intentComments.putExtra("id", viewModel.gameId)
//            intentComments.putExtra("comics_games", "games")
//            startActivity(intentComments)
//        }
//
//        fun Like() {
//            //喜欢
//            binding.gameinfoProgressbar.visibility = View.VISIBLE
//            viewModel.getLike()
//        }
//
//        fun Download() {
//            //开始阅读 默认从第一话开始 以后加历史记录
//            val intent = Intent()
//            intent.action = "android.intent.action.VIEW"
//            intent.data = Uri.parse(gameLink)
//            startActivity(intent)
//        }
//    }
//
//    override fun initViewObservable() {
//        super.initViewObservable()
//        viewModel.liveData.observe(this) {
//            binding.gameinfoProgressbar.visibility = View.GONE
//            if (it.code == 200) {
//                binding.gameImageList.addItemDecoration(
//                    SpacesItemDecoration(
//                        6.dp,
//                        it.data.game.screenshots
//                    )
//                )
//                gameLink = it.data.game.androidLinks[0]
//                binding.gameBtnDownload.visibility = View.VISIBLE
//
//                videoLink= if(it.data.game.videoLink.isNullOrEmpty()) "" else it.data.game.videoLink
//                binding.gameinfoInclude.toolbar.menu.findItem(R.id.action_video).isVisible = !it.data.game.videoLink.isNullOrEmpty()
//
//                //标题
//                binding.gameTitle.text = it.data.game.title
//                binding.gameTitle.paint.isFakeBoldText = true
//
//                //游戏icon
//                Glide.with(this)
//                    .load(GlideUrlNewKey(it.data.game.icon.fileServer, it.data.game.icon.path))
//                    .placeholder(R.drawable.placeholder_transparent)
//                    .into(binding.gameIcon)
//
//                //开发者
//                binding.gamePublisher.text = it.data.game.publisher
//
//                mAdapter.addData(it.data.game)
//                //标签
//                val game_tag = StringBuilder()
//                if (it.data.game.suggest) {
//                    game_tag.append("哔咔推荐 ")
//                }
//                if (it.data.game.adult) {
//                    game_tag.append("18+ ")
//                }
//                if (it.data.game.android) {
//                    game_tag.append("android ")
//                }
//                if (it.data.game.ios) {
//                    game_tag.append("ios ")
//                }
//                binding.gameTag.setText(game_tag)
//
//                //安装包大小
//                binding.gameSize.text = "${it.data.game.androidSize}MB"
//                //下载数
//                binding.gameDownloadsCount.text = "${it.data.game.downloadsCount}下载"
//
//                //喜欢数
//                binding.gameLikeText.text = "${it.data.game.likesCount}人喜欢"
//
//                //是否喜欢
//                binding.gameLikeImage.setImageResource(if (it.data.game.isLiked) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24)
//
//                //评论数
//                binding.gameCommentText.text = "${it.data.game.commentsCount}条评论"
//
//                val game_updatever = StringBuilder()
//                game_updatever.append("最近更新 ")
//                if (!it.data.game.version.isNullOrBlank()){game_updatever.append("v${it.data.game.version}")}
//                binding.gameUpdateVer.text = game_updatever
//                binding.gameUpdateContent.text = it.data.game.updateContent
//                binding.gameDescription.text = it.data.game.description
//            } else {
//                //加载失败
//                MaterialAlertDialogBuilder(this)
//                    .setTitle("网络错误")
//                    .setMessage("网络错误code=${it.code} error=${it.error} message=${it.message}")
//                    .setPositiveButton("重试") { _, _ ->
//                        binding.gameinfoProgressbar.visibility = View.VISIBLE
//                        viewModel.getGameInfo()
//                    }
//                    .setNegativeButton("退出") { _, _ -> finish() }
//                    .show()
//            }
//        }
//
//        //like
//        viewModel.liveData_like.observe(this) {
//            binding.gameinfoProgressbar.visibility = View.GONE
//            if (it.code == 200) {
//                if (it.data.action == "like") {
//                    binding.gameLikeImage.setImageResource(R.drawable.ic_favorite_24)
//                } else {
//                    binding.gameLikeImage.setImageResource(R.drawable.ic_favorite_border_24)
//                }
//            } else {
//                //失败
//                Toast.makeText(
//                    this,
//                    "网络错误，点击爱心失败code=${it.code} error=${it.error} message=${it.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }
//
//
//}
}