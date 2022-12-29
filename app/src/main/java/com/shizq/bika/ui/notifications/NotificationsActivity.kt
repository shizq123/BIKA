package com.shizq.bika.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.NotificationsAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityNotificationsBinding
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.widget.UserViewDialog

class NotificationsActivity : BaseActivity<ActivityNotificationsBinding, NotificationsViewModel>() {
    lateinit var mAdapter: NotificationsAdapter
    private lateinit var userViewDialog: UserViewDialog

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_notifications
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.notificationsInclude.toolbar.title = "我的消息"
        setSupportActionBar(binding.notificationsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.notificationsRv.layoutManager = LinearLayoutManager(this)
        mAdapter = NotificationsAdapter()
        binding.notificationsRv.adapter = mAdapter

        userViewDialog = UserViewDialog(this)

        binding.notificationsBottomInclude.loadLayout.isEnabled = false
        viewModel.getNotifications()
        initListener()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initListener() {
        //加载更多
        binding.notificationsRv.setOnLoadMoreListener {
            viewModel.getNotifications()
        }
        //网络重试点击事件监听
        binding.notificationsBottomInclude.loadLayout.setOnClickListener {
            binding.notificationsBottomInclude.loadLayout.isEnabled = false
            binding.notificationsBottomInclude.loadProgressBar.visibility = ViewGroup.VISIBLE
            binding.notificationsBottomInclude.loadError.visibility = ViewGroup.GONE
            binding.notificationsBottomInclude.loadText.text = ""
            viewModel.getNotifications()

        }

        binding.notificationsRv.setOnItemClickListener { view, position ->
            //服务没有返回具体的哪条评论，只返回了哪个漫画或着游戏的id
            val data = mAdapter.getItemData(position)
            if (data.redirectType == "comment") {
                val intentComments = Intent(this, CommentsActivity::class.java)
                intentComments.putExtra("id", data._redirectId)
                if (data.cover != null) {
                    //服务器没有返回哪里的评论，我是根据有没有封面判断的评论的类型，这个不确定对不对
                    //游戏评论
                    intentComments.putExtra("comics_games", "comics")
                } else {
                    //漫画评论
                    intentComments.putExtra("comics_games", "games")
                }
                startActivity(intentComments)
            }
        }

        binding.notificationsRv.setOnItemChildClickListener { view, position ->
            if (view.id == R.id.item_notifications_image_layout && mAdapter.getItemData(position).redirectType == "comment") {
                userViewDialog.showUserDialog(mAdapter.getItemData(position)._sender)
            }
        }
    }

    override fun initViewObservable() {
        viewModel.liveData.observe(this) {
            if (it.code == 200) {
                if (it.data.notifications.pages <= Integer.valueOf(it.data.notifications.page)) {
                    //总页数等于当前页数 显示后面没有数据
                    binding.notificationsRv.loadMoreEnd()
                } else {
                    //总页数不等于当前页数 回收加载布局
                    binding.notificationsRv.loadMoreComplete() //加载完成
                }

                binding.notificationsBottomInclude.loadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                mAdapter.addData(it.data.notifications.docs)
            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    binding.notificationsBottomInclude.loadProgressBar.visibility = ViewGroup.GONE
                    binding.notificationsBottomInclude.loadError.visibility = ViewGroup.VISIBLE
                    binding.notificationsBottomInclude.loadText.text =
                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                    binding.notificationsBottomInclude.loadLayout.isEnabled = true
                } else {
                    //当页面不是第一页时 网络错误可能是分页加载时出现的网络错误
                    binding.notificationsRv.loadMoreFail()
                }
            }
        }
    }
}