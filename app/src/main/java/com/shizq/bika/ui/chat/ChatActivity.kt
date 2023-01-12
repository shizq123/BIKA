package com.shizq.bika.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
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
import com.shizq.bika.adapter.ChatAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChatBinding
import com.shizq.bika.network.websocket.WebSocketManager
import com.shizq.bika.utils.AndroidBug5497Workaround
import com.shizq.bika.utils.Base64Util
import com.shizq.bika.utils.GlideEngine
import com.shizq.bika.widget.UserViewDialog
import com.yalantis.ucrop.UCrop

//聊天室
//消息是websocket实现，消息是实时，不会留记录,网络不好会丢失消息
class ChatActivity : BaseActivity<ActivityChatBinding, ChatViewModel>() {
    private lateinit var adapter: ChatAdapter
    private lateinit var userViewDialog: UserViewDialog
    var chatRvBottom = false//false表示底部
    private val atUser=ArrayList<String>() //@的用户名

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    @SuppressLint("ResourceType")
    override fun initData() {
        AndroidBug5497Workaround.assistActivity(this)
        viewModel.url =
            intent.getStringExtra("url").toString() + "/socket.io/?EIO=3&transport=websocket"
        binding.chatInclude.toolbar.title = intent.getStringExtra("title").toString()
        setSupportActionBar(binding.chatInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel.webSocketManager = WebSocketManager.getInstance()

        adapter = ChatAdapter()
        binding.chatRv.layoutManager = LinearLayoutManager(this)
        binding.chatRv.adapter = adapter

        userViewDialog = UserViewDialog(this)

        binding.chatProgressbar.show()
        viewModel.WebSocket()
        initListener()
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.toolbar_menu_chat, menu)
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
//            R.id.action_setting -> {
//                Toast.makeText(this, "功能不支持", Toast.LENGTH_SHORT).show()
//            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun initListener() {
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
        binding.chatRvBottomBtn.setOnClickListener {
            chatRvBottom = false
            binding.chatRvBottomBtn.visibility = View.GONE
            binding.chatRv.scrollToPosition(adapter.data.size - 1)
        }

        binding.chatRv.setOnItemChildClickListener { view, position ->

            val id = view.id
            val data = adapter.getItemData(position)
            if (id == R.id.chat_avatar_layout_l) {
                //头像点击事件 查看用户信息
                //聊天信息携带的用户信息不全 可以进行网络获取 以后再说
                userViewDialog.showUserDialog(data)
            }
            if (id == R.id.chat_name_l) {
                //名字点击事件 用于 @
                initChipGroup(data.name)
                //需要弹出键盘
                showKeyboard()
            }

            if (id == R.id.chat_message_layout_l) {
                //消息点击事件 用于 回复
                //判断 语音和图片不能回复
                if (!data.image.isNullOrEmpty()) {
                    userViewDialog.PopupWindow(Base64Util().base64ToBitmap(data.image))
                }
                if (!data.audio.isNullOrEmpty()) {
                    view as RelativeLayout
                    //遍历所有的子view 找到要进行更新ui的view
                    for (i in 0 until view.childCount) {
                        val v: View = view.getChildAt(i)
                        if (v.id == R.id.chat_voice_l) {
                            v as LinearLayout
                            for (j in 0 until v.childCount) {
                                val voiceView: View = v.getChildAt(j)
                                if (voiceView.id == R.id.chat_voice_image_l) {
                                    viewModel.playAudio(data.audio, voiceView)
                                }
                                if (voiceView.id == R.id.chat_voice_dian) {
                                    voiceView.visibility = View.GONE
                                }

                            }
                        }
                    }
                }

                if (data.audio.isNullOrEmpty() && data.image.isNullOrEmpty()) {
//                        Toast.makeText(this,"回复-${}",Toast.LENGTH_SHORT).show()
                    binding.chatSendContentReply.visibility = View.VISIBLE
                    viewModel.reply=data.message
                    viewModel.reply_name=data.name
                    binding.chatSendContentReply.text = data.name + "：" + data.message
                    //需要弹出键盘
                    showKeyboard()
                }
            }
        }

        binding.chatRv.setOnItemChildLongClickListener { view, position ->
            if (view.id == R.id.chat_avatar_layout_l) {
                //头像点击事件 用于 @
                initChipGroup(adapter.getItemData(position).name)
                //需要弹出键盘
                showKeyboard()
            }
            true
        }

        binding.chatSendVoice.setOnClickListener {
            Toast.makeText(this, "发送语音暂不支持", Toast.LENGTH_SHORT).show()
        }
        binding.chatSendPhoto.setOnClickListener {
            PictureSelector.create(this)
                .openGallery(SelectMimeType.ofImage())
                .isCameraForegroundService(true)
                .setCropEngine { fragment, srcUri, destinationUri, dataSource, requestCode ->
                    UCrop.of(srcUri, destinationUri, dataSource)
                        .withMaxResultSize(800, 800)
                        .start(fragment.requireActivity(), fragment, requestCode);
                }
                .setSelectionMode(1)
                .setImageEngine(GlideEngine.createGlideEngine())
                .forResult(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: ArrayList<LocalMedia>) {
                        if (atUser.size > 0) {
                            for (name in atUser){
                                viewModel.atname+=name.replace("@","嗶咔_")
                            }
                        }
                        viewModel.sendMessage(base64Image=Base64Util().getBase64(result[0].cutPath))
                        clearInput()//清空输入框
                    }
                    override fun onCancel() {}
                })
        }
        binding.chatSendBtn.setOnClickListener {
            //发送消息 和官方一致消息不为空时才能发送
            if (!binding.chatSendContentInput.text.toString().trim().isNullOrBlank()) {
                if (atUser.size > 0) {
                    for (name in atUser){
                        viewModel.atname+=name.replace("@","嗶咔_")
                    }
                }
                viewModel.sendMessage(text=binding.chatSendContentInput.text.toString())
                clearInput()//清空输入框
            }
        }
        binding.chatSendContentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                //和官方一致消息不为空时才能发送
                binding.chatSendBtn.isEnabled =
                    !binding.chatSendContentInput.text.toString().trim().isNullOrBlank()
            }
        })

        binding.chatSendContentReply.setOnCloseIconClickListener {
            binding.chatSendContentReply.text = ""
            binding.chatSendContentReply.visibility = View.GONE
        }


    }


    override fun initViewObservable() {
        viewModel.liveData_connections.observe(this) {
            binding.chatInclude.toolbar.subtitle = it
        }
        viewModel.liveData_message.observe(this) {
            //后面要加最大消息数
            adapter.addData(it)
            if (!chatRvBottom) {
                binding.chatRv.scrollToPosition(adapter.data.size - 1)
            }

        }
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

    }


    private fun initChipGroup(str:String) {
        atUser.add("@$str")
        binding.chatSendAt.removeAllViews()
        for (text in atUser) {
            val chip = layoutInflater.inflate(R.layout.item_chip_at, binding.chatSendAt, false) as Chip
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
        val imm =getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.chatSendContentInput, 0)
    }

    fun clearInput(){
        //清空输入框
        atUser.clear()
        binding.chatSendAt.removeAllViews()
        viewModel.atname=""
        viewModel.reply=""
        viewModel.reply_name=""
        binding.chatSendContentInput.setText("")
        binding.chatSendContentReply.text = ""
        binding.chatSendContentReply.visibility = View.GONE
        //滑动到底部
        chatRvBottom = false
        binding.chatRvBottomBtn.visibility = View.GONE
        binding.chatRv.scrollToPosition(adapter.data.size - 1)
    }

}
