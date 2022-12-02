package com.shizq.bika.ui.messages

import android.os.Bundle
import android.view.MenuItem
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityMessagesBinding

class MessagesActivity : BaseActivity<ActivityMessagesBinding, MessagesViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_messages
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }
    override fun initData() {
        binding.collectionsInclude.toolbar.title = "我的消息"
        setSupportActionBar(binding.collectionsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}