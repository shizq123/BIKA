package com.shizq.bika.ui.chat2

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.shizq.bika.adapter.ChatMessageAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChat2Binding
import com.shizq.bika.network.websocket.ChatWebSocketManager
import com.shizq.bika.ui.chatblacklist.ChatBlacklistActivity
import com.shizq.bika.ui.image.ImageActivity
import com.shizq.bika.utils.AndroidBug5497Workaround
import com.shizq.bika.utils.GlideEngine
import com.shizq.bika.widget.UserViewDialog
import com.yalantis.ucrop.UCrop

//新聊天室
//消息是websocket实现，消息是实时，不会留记录,网络不好会丢失消息
class ChatActivity : BaseActivity<ActivityChat2Binding, ChatViewModel>() {
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var userViewDialog: UserViewDialog
    var chatRvBottom = false//false表示底部
    private val atUser = ArrayList<String>() //@的用户名

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat2
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    @SuppressLint("ResourceType")
    override fun initData() {
        AndroidBug5497Workaround.assistActivity(this)
        viewModel.roomId = intent.getStringExtra("id").toString()
        binding.chatInclude.toolbar.title = intent.getStringExtra("title").toString()
        setSupportActionBar(binding.chatInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.webSocketManager = ChatWebSocketManager.getInstance()

        adapter = ChatMessageAdapter()
        binding.chatRv.layoutManager = LinearLayoutManager(this)
        binding.chatRv.adapter = adapter

        userViewDialog = UserViewDialog(this)

        binding.chatProgressbar.show()
        viewModel.WebSocket()
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
            if (id == R.id.chat_avatar_layout_l) {
//                //头像点击事件 查看用户信息
//                //聊天信息携带的用户信息不全 可以进行网络获取 以后再说
//                userViewDialog.showUserDialog(data)
            }
            if (id == R.id.chat_name_l) {
                //名字点击事件 用于 @
//                initChipGroup(data.data.profile.name)
//                //需要弹出键盘
//                showKeyboard()
            }

            if (id == R.id.chat_message_layout_l) {
//                //消息点击事件 用于 回复
//                //判断 语音和图片不能回复
                if (data.data.message.medias!=null) {

                    val intent = Intent(this, ImageActivity::class.java)
                    intent.putExtra("fileserver", "")
                    intent.putExtra("imageurl", data.data.message.medias[0])
                    val imageview=view.findViewById<ImageView>(R.id.chat_content_image_l)
                    val options = ActivityOptions.makeSceneTransitionAnimation(this, imageview, "image")
                    startActivity(intent, options.toBundle())
                }
//                if (!data.audio.isNullOrEmpty()) {
//                    view as RelativeLayout
//                    //遍历所有的子view 找到要进行更新ui的view
//                    for (i in 0 until view.childCount) {
//                        val v: View = view.getChildAt(i)
//                        if (v.id == R.id.chat_voice_l) {
//                            v as LinearLayout
//                            for (j in 0 until v.childCount) {
//                                val voiceView: View = v.getChildAt(j)
//                                if (voiceView.id == R.id.chat_voice_image_l) {
//                                    viewModel.playAudio(data.audio, voiceView)
//                                }
//                                if (voiceView.id == R.id.chat_voice_dian) {
//                                    voiceView.visibility = View.GONE
//                                }
//
//                            }
//                        }
//                    }
                }

//                if (data.audio.isNullOrEmpty() && data.image.isNullOrEmpty()) {
////                        Toast.makeText(this,"回复-${}",Toast.LENGTH_SHORT).show()
//                    binding.chatSendContentReplyLayout.visibility = View.VISIBLE
//                    viewModel.reply=data.message
//                    viewModel.reply_name=data.name
//                    binding.chatSendContentReply.text = data.name + "：" + data.message
//                    //需要弹出键盘
//                    showKeyboard()
//                }
//            }
            if (id == R.id.chat_message_layout_r) {

                if (data.data.message.medias!=null) {
                    val intent = Intent(this, ImageActivity::class.java)
                    intent.putExtra("fileserver", "")
                    intent.putExtra("imageurl", data.data.message.medias[0])
                    val imageview=view.findViewById<ImageView>(R.id.chat_content_image_r)
                    val options = ActivityOptions.makeSceneTransitionAnimation(this, imageview, "image")
                    startActivity(intent, options.toBundle())
                }
            }
        }

        //item子布局长按监听
        binding.chatRv.setOnItemChildLongClickListener { view, position ->
            if (view.id == R.id.chat_avatar_layout_l) {
                //头像点击事件 用于 @
                initChipGroup(adapter.getItemData(position).data.profile.name)
                //需要弹出键盘
                showKeyboard()
            }
            true
        }

        //发送图片
        binding.chatSendPhoto.setOnClickListener {
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
//                        if (atUser.size > 0) {
//                            for (name in atUser) {
//                                viewModel.atname += name.replace("@", "嗶咔_")
//                            }
//                        }
                        viewModel.sendImage(path = result[0].cutPath ,message = "")
                        clearInput()//清空输入框
                    }

                    override fun onCancel() {}
                })
        }

        //发送消息
        binding.chatSendBtn.setOnClickListener {
            //发送消息 和官方一致消息不为空时才能发送
            if (!binding.chatSendContentInput.text.toString().trim().isNullOrBlank()) {
//                if (atUser.size > 0) {
//                    for (name in atUser) {
//                        viewModel.atname += name.replace("@", "嗶咔_")
//                    }
//                }
                viewModel.sendMessage(message = binding.chatSendContentInput.text.toString())
                clearInput()//清空输入框
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


    override fun initViewObservable() {
        //收到的消息
        viewModel.liveData_message.observe(this) {
            //后面要加最大消息数
            adapter.addData(it)
            if (!chatRvBottom) {
                binding.chatRv.scrollToPosition(adapter.data.size - 1)
            }

        }

        //当前通信连接状态
        viewModel.liveData_state.observe(this) {
            if (it == "failed") {
                MaterialAlertDialogBuilder(this)
                    .setTitle("网络错误")
                    .setMessage("是否尝试重新连接")
                    .setPositiveButton("确定") { dialog, which ->
                        binding.chatProgressbar.show()
                        viewModel.WebSocket()
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
            } else{
                //发送失败 弹窗

            }
        }
    }


    private fun initChipGroup(str: String) {
        atUser.add("@$str")
        binding.chatSendAt.removeAllViews()
        for (text in atUser) {
            val chip =
                layoutInflater.inflate(R.layout.item_chip_at, binding.chatSendAt, false) as Chip
            chip.text = text
            binding.chatSendAt.addView(chip)
            chip.setOnClickListener {
                atUser.remove(text)
                binding.chatSendAt.removeView(chip)
            }
        }
    }


    override fun onDestroy() {
        viewModel.webSocketManager.close()
        super.onDestroy()
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
        viewModel.atname = ""
        viewModel.reply = ""
        viewModel.reply_name = ""
        binding.chatSendContentInput.setText("")
        binding.chatSendContentReply.text = ""
        binding.chatSendContentReplyLayout.visibility = View.GONE
        //滑动到底部
        chatRvBottom = false
        binding.chatRvBottomBtn.visibility = View.GONE
        binding.chatRv.scrollToPosition(adapter.data.size - 1)
    }

}
