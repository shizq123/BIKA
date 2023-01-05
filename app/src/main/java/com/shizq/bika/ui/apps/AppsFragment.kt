package com.shizq.bika.ui.apps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatListAdapter
import com.shizq.bika.adapter.PicaAppsAdapter
import com.shizq.bika.base.BaseFragment
import com.shizq.bika.databinding.FragmentAppsBinding
import com.shizq.bika.ui.chat.ChatActivity
import com.shizq.bika.utils.SPUtil

class AppsFragment : BaseFragment<FragmentAppsBinding, AppsFragmentViewModel>() {
    private var str: String? = null
    private lateinit var mChatListAdapter: ChatListAdapter
    private lateinit var mPicaAppsAdapter: PicaAppsAdapter

    override fun initContentView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): Int {
        return R.layout.fragment_apps
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        str = arguments?.getString("key")
        mChatListAdapter = ChatListAdapter()
        mPicaAppsAdapter = PicaAppsAdapter()
        binding.appsRv.layoutManager = LinearLayoutManager(context)
        if (str == "chat") {
            binding.appsRv.adapter = mChatListAdapter
            viewModel.getChatList()
        } else {
            binding.appsRv.adapter = mPicaAppsAdapter
            viewModel.getPicaApps()
        }

        binding.appsInclude.loadLayout.isEnabled = false
        initListener()
    }

    private fun initListener() {
        binding.appsRv.setOnItemClickListener { _, position ->
            if (str == "chat") {
                val intent = Intent(activity, ChatActivity::class.java)
                intent.putExtra("title", mChatListAdapter.getItemData(position).title)
                intent.putExtra("url", mChatListAdapter.getItemData(position).url)
                startActivity(intent)
            } else {
                val intent = Intent()
                intent.action = "android.intent.action.VIEW"
                intent.data = Uri.parse(
                    "${mPicaAppsAdapter.getItemData(position).url}/?token=${
                        SPUtil.get(context, "token", "")
                    }&secret=pb6XkQ94iBBny1WUAxY0dY5fksexw0dt"
                )
                startActivity(intent)
            }
        }

        //网络重试点击事件监听
        binding.appsInclude.loadLayout.setOnClickListener {
            binding.appsInclude.loadLayout.isEnabled = false
            binding.appsInclude.loadProgressBar.visibility = ViewGroup.VISIBLE
            binding.appsInclude.loadError.visibility = ViewGroup.GONE
            binding.appsInclude.loadText.text = ""
            if (this.str == "chat") {
                viewModel.getChatList()
            } else {
                viewModel.getPicaApps()
            }

        }
    }

    override fun initViewObservable() {
        viewModel.liveData_chat.observe(this) {
            if (it.code == 200) {
                mChatListAdapter.clear()
                binding.appsInclude.loadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                mChatListAdapter.addData(it.data.chatList)
            } else {
                //网络错误
                binding.appsInclude.loadProgressBar.visibility = ViewGroup.GONE
                binding.appsInclude.loadError.visibility = ViewGroup.VISIBLE
                binding.appsInclude.loadText.text =
                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                binding.appsInclude.loadLayout.isEnabled = true

            }
        }
        viewModel.liveData_apps.observe(this) {
            if (it.code == 200) {
                mPicaAppsAdapter.clear()
                binding.appsInclude.loadLayout.visibility = ViewGroup.GONE//隐藏加载进度条页面
                mPicaAppsAdapter.addData(it.data.apps)
            } else {
                //网络错误
                binding.appsInclude.loadProgressBar.visibility = ViewGroup.GONE
                binding.appsInclude.loadError.visibility = ViewGroup.VISIBLE
                binding.appsInclude.loadText.text =
                    "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                binding.appsInclude.loadLayout.isEnabled = true

            }
        }
    }

}