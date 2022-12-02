package com.shizq.bika.ui.history

import android.os.Bundle
import android.view.MenuItem
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityHistoryBinding

class HistoryActivity : BaseActivity<ActivityHistoryBinding, HistoryViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_history
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }
    override fun initData() {
        binding.collectionsInclude.toolbar.title = "历史记录"
        setSupportActionBar(binding.collectionsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
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