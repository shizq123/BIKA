package com.shizq.bika.ui.mycomments

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.CommentsAdapter
import com.shizq.bika.adapter.MyCommentsAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.databinding.ActivityMyCommentsBinding
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import com.shizq.bika.ui.games.GameInfoActivity
import com.shizq.bika.utils.*
import com.shizq.bika.widget.UserViewDialog
import me.jingbin.library.ByRecyclerView

class MyCommentsActivity : BaseActivity<ActivityMyCommentsBinding, MyCommentsViewModel>() {
    lateinit var mAdapter: MyCommentsAdapter
    lateinit var adapter_sub: CommentsAdapter

    private lateinit var userViewDialog: UserViewDialog

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var sub_comments_view: View
    private lateinit var sub_comments_back: View
    private lateinit var sub_comments_title: TextView
    private lateinit var sub_comments_rv: ByRecyclerView
    private lateinit var sub_comments_reply_layout: View

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_my_comments
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.myCommentsInclude.toolbar.title = "我的评论"
        setSupportActionBar(binding.myCommentsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.myCommentsRv.layoutManager = LinearLayoutManager(this)
        mAdapter = MyCommentsAdapter()
        binding.myCommentsRv.adapter = mAdapter

        userViewDialog= UserViewDialog(this)

        //        //子评论 bottomSheetDialog
        sub_comments_view = View.inflate(this, R.layout.view_bottom_sub_comments, null)
        bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(sub_comments_view)

        val dialog_layout = sub_comments_view.findViewById<ViewGroup>(R.id.sub_comments_layout)
        sub_comments_back = sub_comments_view.findViewById(R.id.sub_comments_head_back)
        sub_comments_title = sub_comments_view.findViewById(R.id.sub_comments_head_title)
        sub_comments_rv = sub_comments_view.findViewById(R.id.sub_comments_rv)
        sub_comments_reply_layout = sub_comments_view.findViewById(R.id.sub_comments_reply_layout)
        BottomSheetBehavior.from(sub_comments_view.parent as View).peekHeight = getWindowHeight()
        val params: ViewGroup.LayoutParams = dialog_layout.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = getWindowHeight()
        dialog_layout.layoutParams = params

        //子评论 recyclerview
        sub_comments_rv.layoutManager = LinearLayoutManager(this)
        adapter_sub = CommentsAdapter()
        sub_comments_rv.adapter = adapter_sub

        binding.myCommentsInclude2.loadLayout.isEnabled = false
        viewModel.requestComment()
        initListener()
    }

    private fun getWindowHeight(): Int {
        return resources.displayMetrics.heightPixels-50.dp
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
        binding.myCommentsRv.setOnItemChildClickListener { view, position ->
            val id= view.id
            val data =mAdapter.getItemData(position)
            //点赞
            if (id == R.id.item_my_comments_like_layout) {
                viewModel.likePosition=position//保存当前要点赞的position
                viewModel.likeCommentsId=mAdapter.getItemData(position)._id//保存当前要点赞的position
                viewModel.commentsLike()
                binding.myCommentsProgressbar.visibility=View.VISIBLE
            }
            //跳转
            if (id == R.id.item_my_comments_title_layout) {
                if (data._game == null) {
                    val intent = Intent(this, ComicInfoActivity::class.java)
                    intent.putExtra("id", data._comic._id)
                    intent.putExtra("fileserver", "")
                    intent.putExtra("imageurl", "")
                    intent.putExtra("title", data._comic.title)
                    intent.putExtra("author", "")
                    intent.putExtra("totalViews", "")
                    startActivity(intent)

                } else {
                    val intent = Intent(this, GameInfoActivity::class.java)
                    intent.putExtra("gameId", data._game._id)
                    startActivity(intent)
                }
            }
            //评论
            if (id == R.id.item_my_comments_sub_layout) {
                //解决上一条子评论重复显示的问题
                adapter_sub.clear()
                val bean = CommentsBean.Comments.Doc(
                    data._id,
                    CommentsBean.User(
                        "",
                        CommentsBean.User.Avatar(
                            fileServer = SPUtil.get(this, "user_fileServer", "") as String,
                            "",
                            path = SPUtil.get(this, "user_path", "") as String

                        ),
                        character = SPUtil.get(this, "user_character", "") as String,
                        emptyList(),
                        0,
                    gender =SPUtil.get(this, "user_gender", "") as String,
                        level =SPUtil.get(this, "user_level", 1) as Int,
                        name =SPUtil.get(this, "user_name", "") as String,
                        "",
                        "",
                        "",
                        false
                    ),
                    0,
                    data.content,
                    data.created_at,
                    false,
                    data.id,
                    data.isLiked,
                    false,
                    data.likesCount,
                    data.totalComments,
                    true
                )
                viewModel.likePosition=position//暂存打开哪个主评论的position
                viewModel.commentsId = data._id
                viewModel.subPage = 0

                val list_sub_comments: ArrayList<CommentsBean.Comments.Doc> = ArrayList()
                list_sub_comments.add(bean)
                adapter_sub.addData(list_sub_comments)
                viewModel.requestSubComment()
                bottomSheetDialog.show()
            }
        }

        //加载更多
        binding.myCommentsRv.setOnLoadMoreListener {
            viewModel.requestComment()
        }

        sub_comments_rv.setOnItemClickListener { v, position ->
            if (position != 0) {
                userViewDialog.showUserDialog(adapter_sub.getItemData(position)._user,sub_comments_view)
            }
        }
        sub_comments_rv.setOnItemChildClickListener { view, position ->
            val id= view.id
            val data =adapter_sub.getItemData(position)
            if (id == R.id.comments_name||id == R.id.comments_image_layout) {  userViewDialog.showUserDialog(data._user,sub_comments_view) }
            //点赞
            if (id == R.id.comments_like_layout) {
                viewModel.likeSubPosition=position//保存当前要点赞的position
                viewModel.likeSubCommentsId=adapter_sub.getItemData(position)._id//保存当前要点赞的position
                viewModel.subCommentsLike()
            }

        }

        //子评论 分页列表加载更多的接口
        sub_comments_rv.setOnLoadMoreListener{ viewModel.requestSubComment() }

        //网络重试点击事件监听
        binding.myCommentsInclude2.loadLayout.setOnClickListener {
            binding.myCommentsInclude2.loadLayout.isEnabled = false
            binding.myCommentsInclude2.loadProgressBar.visibility = ViewGroup.VISIBLE
            binding.myCommentsInclude2.loadError.visibility = ViewGroup.GONE
            binding.myCommentsInclude2.loadText.text = ""
            viewModel.requestComment()

        }

        sub_comments_back.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

    }

    override fun initViewObservable() {
        viewModel.liveData_comments.observe(this) {
            if (it.code == 200) {
                if (it.data.comments.pages == Integer.valueOf(it.data.comments.page)) {
                    //总页数等于当前页数 显示后面没有数据
                    binding.myCommentsRv.loadMoreEnd()
                } else {
                    //总页数不等于当前页数 回收加载布局
                    binding.myCommentsRv.loadMoreComplete() //加载完成
                }

                binding.myCommentsInclude2.loadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                mAdapter.addData(it.data.comments.docs)
            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    binding.myCommentsInclude2.loadProgressBar.visibility = ViewGroup.GONE
                    binding.myCommentsInclude2.loadError.visibility = ViewGroup.VISIBLE
                    binding.myCommentsInclude2.loadText.text =
                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                    binding.myCommentsInclude2.loadLayout.isEnabled = true
                } else {
                    //当页面不是第一页时 网络错误可能是分页加载时出现的网络错误
                    binding.myCommentsRv.loadMoreFail()
                }
            }
        }

        //子评论
        viewModel.liveData_sub_comments.observe(this) {
            if (it.code == 200) {

                adapter_sub.addData(it.data.comments.docs)
                if (it.data.comments.pages == it.data.comments.page) {
                    //总页数等于当前页数 显示后面没有数据
                    sub_comments_rv.loadMoreEnd()
                } else {
                    //总页数不等于当前页数 回收加载布局
                    sub_comments_rv.loadMoreComplete() //加载完成
                }
            } else {
                sub_comments_rv.loadMoreFail()

            }
        }

        //评论点赞
        viewModel.liveData_comments_like.observe(this) {
            binding.myCommentsProgressbar.visibility=View.GONE
            if (it.code == 200) {
                //  设置要局部刷新的position及payload
                mAdapter.refreshNotifyItemChanged(viewModel.likePosition, it.data.action=="like")
            } else {
                Toast.makeText(this,"点击爱心失败",Toast.LENGTH_SHORT).show()

            }
        }

        //子评论点赞
        viewModel.liveData_sub_comments_like.observe(this) {
            if (it.code == 200) {
                //  设置要局部刷新的position及payload
                adapter_sub.refreshNotifyItemChanged(viewModel.likeSubPosition, it.data.action=="like")

                if(viewModel.commentsId==adapter_sub.data[viewModel.likeSubPosition]._id){
                    //因为第一条数据是手动添加的，所以在这判断是否第一条数据 ，如果是就把主评论也更新
                    mAdapter.refreshNotifyItemChanged(viewModel.likePosition, it.data.action=="like")
                }
            } else {
                Toast.makeText(this,"点击爱心失败",Toast.LENGTH_SHORT).show()

            }
        }
    }
}