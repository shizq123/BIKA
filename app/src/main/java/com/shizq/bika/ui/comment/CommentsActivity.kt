package com.shizq.bika.ui.comment

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
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.databinding.ActivityCommentsBinding
import com.shizq.bika.utils.dp
import com.shizq.bika.widget.InputTextMsgDialog
import com.shizq.bika.widget.UserViewDialog
import me.jingbin.library.ByRecyclerView

/**
 * 评论
 */

class CommentsActivity : BaseActivity<ActivityCommentsBinding, CommentsViewModel>() {
    private lateinit var adapter_v2: CommentsAdapter
    private lateinit var adapter_sub: CommentsAdapter


    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var sub_comments_view: View
    private lateinit var sub_comments_back: View
    private lateinit var sub_comments_title: TextView
    private lateinit var sub_comments_rv: ByRecyclerView
    private lateinit var sub_comments_reply_layout: View

    private lateinit var userViewDialog: UserViewDialog

    private lateinit var dialog_send_comments: InputTextMsgDialog
    private lateinit var dialog_send_sub_comments: InputTextMsgDialog


    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_comments
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        viewModel.id = intent.getStringExtra("id")
        viewModel.comics_games = intent.getStringExtra("comics_games")

        binding.commentsInclude.toolbar.title = "评论"
        setSupportActionBar(binding.commentsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true);

        binding.commentsRv.isRefreshEnabled = false //禁止下拉刷新
        binding.commentsRv.layoutManager = LinearLayoutManager(this)
        adapter_v2 = CommentsAdapter()
        binding.commentsRv.adapter = adapter_v2

        userViewDialog = UserViewDialog(this)

        //子评论 bottomSheetDialog
        sub_comments_view =
            View.inflate(this@CommentsActivity, R.layout.view_bottom_sub_comments, null)
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
        sub_comments_rv.isRefreshEnabled = false //禁止下拉刷新
        sub_comments_rv.layoutManager = LinearLayoutManager(this)
        adapter_sub = CommentsAdapter()
        sub_comments_rv.adapter = adapter_sub

        dialog_send_comments = InputTextMsgDialog(this)
        dialog_send_sub_comments = InputTextMsgDialog(this)

        binding.commentsLoadLayout.isEnabled = false
        viewModel.requestComment()

        initListener()
    }

    private fun getWindowHeight(): Int {
        return resources.displayMetrics.heightPixels - 50.dp
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initListener() {
        binding.commentsRv.setOnItemClickListener { v, position ->
            if (adapter_v2.getItemData(position)._user != null) {
                viewModel.commentsId = adapter_v2.getItemData(position).id
                dialog_send_sub_comments.setTitleText("回复 " + adapter_v2.getItemData(position)._user.name)
                dialog_send_sub_comments.show()
            }
        }
        binding.commentsRv.setOnItemChildClickListener { view, position ->
            val id = view.id
            val data = adapter_v2.getItemData(position)
            if (id == R.id.comments_name || id == R.id.comments_image_layout) {
                userViewDialog.showUserDialog(data._user)
            }
            //点赞
            if (id == R.id.comments_like_layout) {
                viewModel.likePosition = position//保存当前要点赞的position
                viewModel.likeCommentsId = adapter_v2.getItemData(position).id//保存当前要点赞的position
                viewModel.commentsLike()
                binding.commentsProgressbar.visibility = View.VISIBLE
            }
            //显示更多评论
            if (id == R.id.comments_sub_layout) {
                //解决上一条子评论重复显示的问题
                adapter_sub.clear()
                val bean = CommentsBean.Comments.Doc(
                    data._id,
                    data._user,
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
                viewModel.data = bean
                viewModel.commentsId = data._id
                viewModel.likePosition = position//暂存打开哪个主评论的position
                viewModel.subPage = 0

                val list_sub_comments: ArrayList<CommentsBean.Comments.Doc> = ArrayList()
                list_sub_comments.add(bean)
                adapter_sub.addData(list_sub_comments)
                viewModel.requestSubComment()
                bottomSheetDialog.show()
            }
        }
        sub_comments_rv.setOnItemClickListener { v, position ->
            if (position == 0) {
                dialog_send_sub_comments.setTitleText("回复 " + adapter_sub.getItemData(position)._user.name)
                dialog_send_sub_comments.show()
            } else {
                userViewDialog.showUserDialog(adapter_sub.getItemData(position)._user, sub_comments_view)
            }
        }
        sub_comments_rv.setOnItemChildClickListener { view, position ->
            val id = view.id
            val data = adapter_sub.getItemData(position)
            if (id == R.id.comments_name || id == R.id.comments_image_layout) {
                userViewDialog.showUserDialog(data._user, sub_comments_view)
            }
            //点赞
            if (id == R.id.comments_like_layout) {
                viewModel.likeSubPosition = position//保存当前要点赞的position
                viewModel.likeSubCommentsId =
                    adapter_sub.getItemData(position)._id//保存当前要点赞的position
                viewModel.subCommentsLike()
            }

        }

        sub_comments_back.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        binding.commentsReplyLayout.setOnClickListener {
            //底部发布评论栏
            dialog_send_comments.setTitleText("发表评论")
            dialog_send_comments.show()
        }
        sub_comments_reply_layout.setOnClickListener {
            //底部回复评论栏
            dialog_send_sub_comments.setTitleText("回复 ${adapter_sub.data[0]._user.name}")
            dialog_send_sub_comments.show()
        }
        dialog_send_comments.setmOnTextSendListener {
            binding.commentsLoadLayout.isEnabled = false //显示进度条
            binding.commentsLoadProgressBar.visibility = ViewGroup.VISIBLE
            binding.commentsLoadError.visibility = ViewGroup.GONE
            binding.commentsLoadText.text = ""
            viewModel.seedComments(it)

        }
        dialog_send_sub_comments.setmOnTextSendListener {
            viewModel.seedSubComments(it)
        }

    }

    override fun initViewObservable() {
        viewModel.liveData_comments.observe(this) {
            if (it.code == 200) {
                if (it.data.comments.pages <= it.data.comments.page) {
                    //总页数等于当前页数 显示后面没有数据
                    binding.commentsRv.loadMoreEnd()
                } else {
                    //总页数不等于当前页数 回收加载布局
                    binding.commentsRv.loadMoreComplete() //加载完成
                }

                binding.commentsReplyLayout.visibility = ViewGroup.VISIBLE
                binding.commentsLoadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                adapter_v2.addData(it.data.comments.docs)


            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    binding.commentsLoadProgressBar.visibility = ViewGroup.GONE
                    binding.commentsLoadError.visibility = ViewGroup.VISIBLE
                    binding.commentsLoadText.text =
                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                    binding.commentsLoadLayout.isEnabled = true
                } else {
                    //当页面不是第一页时 网络错误可能是分页加载时出现的网络错误
                    binding.commentsRv.loadMoreFail()
                }
            }
        }

        //加载更多
        binding.commentsRv.setOnLoadMoreListener {
            viewModel.requestComment()
        }

        //网络重试点击事件监听
        binding.commentsLoadLayout.setOnClickListener {
            binding.commentsLoadLayout.isEnabled = false
            binding.commentsLoadProgressBar.visibility = ViewGroup.VISIBLE
            binding.commentsLoadError.visibility = ViewGroup.GONE
            binding.commentsLoadText.text = ""
            viewModel.requestComment()

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
        //发送评论
        viewModel.liveData_seed_comments.observe(this) {
            if (it.code == 200) {
                //成功
                adapter_v2.clear()
                viewModel.page = 0
                viewModel.requestComment()
            } else if (it.code == 400) {
                if (it.error == "1019") {
                    //cannot comment
                    Toast.makeText(this, "等级不够不能发送评论", Toast.LENGTH_SHORT).show()
                } else if (it.error == "1031") {
                    //higher level is required
                    Toast.makeText(this, "等级不够不能发送评论", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "评论发送失败 ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
        //发送子评论
        viewModel.liveData_seed_sub_comments.observe(this) {
            if (it.code == 200) {
                //成功
                adapter_sub.clear()

                val list_sub_comments: ArrayList<CommentsBean.Comments.Doc> = ArrayList()
                viewModel.data?.let { it1 ->
                    list_sub_comments.add(it1)
                    adapter_sub.addData(list_sub_comments)
                }

                viewModel.subPage = 0
                viewModel.requestSubComment()
            } else if (it.code == 400) {
                if (it.error == "1019") {
                    //cannot comment
                    Toast.makeText(this, "等级不够不能发送评论", Toast.LENGTH_SHORT).show()
                } else if (it.error == "1031") {
                    //higher level is required
                    Toast.makeText(this, "等级不够不能发送评论", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "评论回复发送失败", Toast.LENGTH_SHORT).show()

            }
        }

        //子评论 分页列表加载更多的接口
        sub_comments_rv.setOnLoadMoreListener { viewModel.requestSubComment() }

        //评论点赞
        viewModel.liveData_comments_like.observe(this) {
            binding.commentsProgressbar.visibility = View.GONE
            if (it.code == 200) {
                //  设置要局部刷新的position及payload
                adapter_v2.refreshNotifyItemChanged(
                    viewModel.likePosition,
                    it.data.action == "like"
                )
            } else {
                Toast.makeText(this, "点击爱心失败", Toast.LENGTH_SHORT).show()

            }
        }

        //子评论点赞
        viewModel.liveData_sub_comments_like.observe(this) {
            if (it.code == 200) {
                //  设置要局部刷新的position及payload
                adapter_sub.refreshNotifyItemChanged(
                    viewModel.likeSubPosition,
                    it.data.action == "like"
                )

                if (viewModel.commentsId == adapter_sub.data[viewModel.likeSubPosition]._id) {
                    //因为第一条数据是手动添加的，所以在这判断是否第一条数据 ，如果是就把主评论也更新
                    adapter_v2.refreshNotifyItemChanged(
                        viewModel.likePosition,
                        it.data.action == "like"
                    )
                }
            } else {
                Toast.makeText(this, "点击爱心失败", Toast.LENGTH_SHORT).show()

            }
        }

    }

}