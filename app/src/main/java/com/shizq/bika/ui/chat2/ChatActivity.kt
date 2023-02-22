package com.shizq.bika.ui.chat2

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatMessageMultiAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.bean.UserMention
import com.shizq.bika.databinding.ActivityChat2Binding
import com.shizq.bika.service.Chat2WebSocketService
import com.shizq.bika.ui.chatblacklist.ChatBlacklistActivity
import com.shizq.bika.ui.image.ImageActivity
import com.shizq.bika.utils.*
import com.shizq.bika.widget.UserViewDialog
import com.yalantis.ucrop.UCrop

//新聊天室
//消息是websocket实现，消息是实时，不会留记录,网络不好会丢失消息
class ChatActivity : BaseActivity<ActivityChat2Binding, ChatViewModel>() {
    private lateinit var adapter: ChatMessageMultiAdapter
    private lateinit var userViewDialog: UserViewDialog
    var chatRvBottom = false//false表示底部
    private val atUser = ArrayList<UserMention>() //@的用户名

    private lateinit var mService: Chat2WebSocketService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as Chat2WebSocketService.ChatBinder
            mService = binder.getService()
            viewModel.roomId = intent.getStringExtra("id").toString()
            mService.webSocket(viewModel.roomId)
            initObservable()
        }

        override fun onServiceDisconnected(className: ComponentName) {}
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat2
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    @SuppressLint("ResourceType")
    override fun initData() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)//屏幕常亮
        AndroidBug5497Workaround.assistActivity(this)

        val intentService = Intent(this, Chat2WebSocketService::class.java)
        bindService(intentService, connection, BIND_AUTO_CREATE)

        binding.chatInclude.toolbar.title = intent.getStringExtra("title").toString()
        setSupportActionBar(binding.chatInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ChatMessageMultiAdapter()
        binding.chatRv.layoutManager = LinearLayoutManager(this)
        binding.chatRv.adapter = adapter

        userViewDialog = UserViewDialog(this)

        binding.chatProgressbar.show()
        initListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_chat2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }

            R.id.action_blacklist -> {
                startActivity(Intent(this, ChatBlacklistActivity::class.java))
            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun initListener() {
        //显示 跳转到底部的按钮
        binding.chatRv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                chatRvBottom = recyclerView.canScrollVertically(1)//判断是否到底部 false是底部

                if (!chatRvBottom) {
                    binding.chatRvBottomBtn.visibility = View.GONE
                } else {
                    binding.chatRvBottomBtn.visibility = View.VISIBLE
                }

            }
        })

        //跳转到底部的按钮监听
        binding.chatRvBottomBtn.setOnClickListener {
            chatRvBottom = false
            binding.chatRvBottomBtn.visibility = View.GONE
            binding.chatRv.scrollToPosition(adapter.data.size - 1)
        }

        //item子布局监听
        binding.chatRv.setOnItemChildClickListener { view, position ->
//
            val id = view.id
            val data = adapter.getItemData(position)
            //点击头像 查看用户信息
            if (id == R.id.chat_avatar_layout_l) {
                userViewDialog.showUserDialog(data.data.profile)
            }
            //点击名字 用于 @
            if (id == R.id.chat_name_l) {
                initChipGroup(data.data.profile.id, data.data.profile.name)
                //需要弹出键盘
                showKeyboard()
            }

            //点击回复的消息 查看消息
            if (id == R.id.chat_reply_layout || id == R.id.chat_reply_layout_r) {
                //查看回复的消息
                when (data.data.reply.type) {
                    "TEXT_MESSAGE" -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(data.data.reply.name)
                            .setMessage(data.data.reply.message)
                            .show()
                    }
                    "IMAGE_MESSAGE" -> {
                        //TODO 简单实现效果 后面添加到view中 加入复制
                        val image = ImageView(this)
                        image.adjustViewBounds = true
//                        image.setPadding(24.dp)
                        GlideApp.with(this)
                            .load(data.data.reply.image)
                            .placeholder(R.drawable.placeholder_avatar_2)
                            .into(image)
                        image.setOnClickListener {
                            val intent = Intent(this, ImageActivity::class.java)
                            intent.putExtra("fileserver", "")
                            intent.putExtra("imageurl", data.data.reply.image)
                            val options =
                                ActivityOptions.makeSceneTransitionAnimation(this, it, "image")
                            startActivity(intent, options.toBundle())
                        }
                        MaterialAlertDialogBuilder(this)
                            .setTitle(data.data.reply.name)
                            .setView(image)
                            .show()
                    }
                    else -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(data.data.reply.name)
                            .setMessage("[该消息类型不支持查看]")
                            .show()
                    }
                }

            }

            //点击消息 收到的消息
            if (id == R.id.chat_message_layout_l) {
                //消息点击事件 用于 回复
                //判断 消息类型
                when (data.type) {
                    "TEXT_MESSAGE" -> {
                        //文本消息 点击回复
                        binding.chatSendContentReplyLayout.visibility = View.VISIBLE
                        viewModel.replyMessage = data.data.message.message
                        viewModel.replyName = data.data.profile.name
                        viewModel.replyId = data.id
                        viewModel.replyMessageType = data.type
                        binding.chatSendContentReply.text =
                            data.data.profile.name + "：" + data.data.message.message
                        //需要弹出键盘
                        showKeyboard()
                    }
                    "IMAGE_MESSAGE" -> {
                        //图片消息 点击查看 //后面优化 添加回复
                        val intent = Intent(this, ImageActivity::class.java)
                        intent.putExtra("fileserver", "")
                        intent.putExtra("imageurl", data.data.message.medias[0])
                        val imageview = view.findViewById<ImageView>(R.id.chat_content_image_l)
                        val options =
                            ActivityOptions.makeSceneTransitionAnimation(this, imageview, "image")
                        startActivity(intent, options.toBundle())
                    }
                    else -> {
                        //其他类型 不操作
                    }
                }
            }

            //点击消息 自己发送的消息
            if (id == R.id.chat_message_layout_r) {
                //判断 消息类型
                when (data.type) {
                    //文本消息 点击回复
                    "TEXT_MESSAGE" -> {
                        binding.chatSendContentReplyLayout.visibility = View.VISIBLE
                        viewModel.replyMessage = data.data.message.message
                        viewModel.replyName = data.data.profile.name
                        viewModel.replyId = data.id
                        viewModel.replyMessageType = data.type
                        binding.chatSendContentReply.text =
                            data.data.profile.name + "：" + data.data.message.message
                        //需要弹出键盘
                        showKeyboard()
                    }
                    "IMAGE_MESSAGE" -> {
                        //图片消息 点击查看 //后面优化 添加回复
                        val intent = Intent(this, ImageActivity::class.java)
                        intent.putExtra("fileserver", "")
                        intent.putExtra("imageurl", data.data.message.medias[0])
                        val imageview = view.findViewById<ImageView>(R.id.chat_content_image_r)
                        val options =
                            ActivityOptions.makeSceneTransitionAnimation(this, imageview, "image")
                        startActivity(intent, options.toBundle())
                    }
                    else -> {
                        //其他类型 不操作
                    }
                }
            }
        }

        //item子布局长按监听
        binding.chatRv.setOnItemChildLongClickListener { view, position ->
            if (view.id == R.id.chat_avatar_layout_l) {
                //头像点击事件 用于 @
                val profile = adapter.getItemData(position).data.profile
                initChipGroup(profile.id, profile.name)
                //需要弹出键盘
                showKeyboard()
            }
            true
        }

        //发送图片
        binding.chatSendPhoto.setOnClickListener {
            //用进度条来判断聊天室是否连接成功
            if (!binding.chatProgressbar.isShown) {
                PictureSelector.create(this)
                    .openGallery(SelectMimeType.ofImage())
                    .isCameraForegroundService(true)
                    .isGif(true)
                    .setCropEngine { fragment, srcUri, destinationUri, dataSource, requestCode ->
                        UCrop.of(srcUri, destinationUri, dataSource)
                            .start(fragment.requireActivity(), fragment, requestCode);
                    }
                    .setSelectionMode(1)
                    .setImageEngine(GlideEngine.createGlideEngine())
                    .forResult(object : OnResultCallbackListener<LocalMedia> {
                        override fun onResult(result: ArrayList<LocalMedia>) {
                            //发送图片 不添加回复与文字 纯图片发送//后面优化
                            viewModel.sendImage(path = result[0].cutPath, message = "")
                            clearInput()//清空输入框
                        }

                        override fun onCancel() {}
                    })
            }
        }

        //发送消息
        binding.chatSendBtn.setOnClickListener {
            //用进度条来判断聊天室是否连接成功
            if (!binding.chatProgressbar.isShown) {
                //发送消息 和官方一致消息不为空时才能发送
                if (!binding.chatSendContentInput.text.toString().trim().isNullOrBlank()) {
                    when (viewModel.replyMessageType) {
                        "" -> {
                            //不是回复消息 发送的纯文字
                            viewModel.sendMessage(
                                userMentions = atUser,
                                message = binding.chatSendContentInput.text.toString()
                            )
                        }
                        "TEXT_MESSAGE" -> {
                            //回复文字消息
                            viewModel.sendMessage(
                                userMentions = atUser,
                                message = binding.chatSendContentInput.text.toString()
                            )
                        }
                        "IMAGE_MESSAGE" -> {
                            //回复图片消息 //后面添加

                        }
                        else -> {
                            //回复其他消息 //后面添加

                        }
                    }

                    clearInput()//清空输入框
                }
            }
        }

        //输入框监听
        binding.chatSendContentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                //和官方一致消息不为空时才能发送
                binding.chatSendBtn.isEnabled =
                    !binding.chatSendContentInput.text.toString().trim().isNullOrBlank()
            }
        })

        //关闭需要回复的内容
        binding.chatSendContentReplyClose.setOnClickListener {
            binding.chatSendContentReply.text = ""
            binding.chatSendContentReplyLayout.visibility = View.GONE
        }


    }

    //来自service的
    fun initObservable() {
        //收到的消息
        mService.liveData_message.observe(this) {
            //后面要加最大消息数
            adapter.addData(it)
            if (!chatRvBottom) {
                binding.chatRv.scrollToPosition(adapter.data.size - 1)
            }
        }

        //当前通信连接状态
        mService.liveData_state.observe(this) {
            if (it == "failed") {
                MaterialAlertDialogBuilder(this)
                    .setTitle("网络错误")
                    .setMessage("是否尝试重新连接")
                    .setPositiveButton("确定") { dialog, which ->
                        binding.chatProgressbar.show()
                        mService.webSocket(viewModel.roomId)
                    }
                    .setNegativeButton("退出") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            if (it == "success") {
                binding.chatProgressbar.hide()
            }
        }
    }

    //来自viewModel的
    override fun initViewObservable() {
        //收到的消息
        viewModel.liveData_message.observe(this) {
            adapter.addData(it)
            if (!chatRvBottom) {
                binding.chatRv.scrollToPosition(adapter.data.size - 1)
            }
        }

        viewModel.liveDataSendMessage.observe(this) {
            if (it.data != null) {
                //发送成功
                //通过 for循环 找到自己发送的消息进行数据更新
                for (i in 0 until adapter.data.size) {
                    if (adapter.getItemData(i).data != null) {
                        if (adapter.getItemData(i).type == "TEXT_MESSAGE" || adapter.getItemData(i).type == "IMAGE_MESSAGE") {
                            //得到指定id条目的位置
                            if (adapter.getItemData(i).data.message.referenceId == it.data.message.referenceId) {
                                //更新ui
                                adapter.data[i] = it
                                adapter.notifyItemChanged(i)
                                break
                            }
                        }
                    }
                }
            } else {
                //发送失败 弹窗

            }
        }
    }

    private fun initChipGroup(userid: String, name: String) {
        atUser.add(UserMention(userid, name))
        binding.chatSendAt.removeAllViews()
        for (user in atUser) {
            val chip =
                layoutInflater.inflate(R.layout.item_chip_at, binding.chatSendAt, false) as Chip
            chip.text = user.name
            binding.chatSendAt.addView(chip)
            chip.setOnClickListener {
                atUser.remove(user)
                binding.chatSendAt.removeView(chip)
            }
        }
    }

    private fun showKeyboard() {
        binding.chatSendContentInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.chatSendContentInput, 0)
    }

    fun clearInput() {
        //清空输入框
        atUser.clear()
        binding.chatSendAt.removeAllViews()
        viewModel.replyMessage = ""
        viewModel.replyImage = ""
        viewModel.replyId = ""
        viewModel.replyName = ""
        viewModel.replyMessageType = ""
        binding.chatSendContentInput.setText("")
        binding.chatSendContentReply.text = ""
        binding.chatSendContentReplyLayout.visibility = View.GONE
        //滑动到底部
        chatRvBottom = false
        binding.chatRvBottomBtn.visibility = View.GONE
        binding.chatRv.scrollToPosition(adapter.data.size - 1)
    }

}
