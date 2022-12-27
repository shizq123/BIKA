package com.shizq.bika.ui.apps

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.tabs.TabLayoutMediator
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ViewPagerAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityAppsBinding

//小程序
class AppsActivity : BaseActivity<ActivityAppsBinding,AppsViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_apps
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.toolbar.title="哔咔小程序"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabText = listOf(
            "聊天室",
            "小程序"
        )
        val tabList = listOf(
            AppsFragment("chat"),
            AppsFragment("apps"),
        )
        val tabAdapter= ViewPagerAdapter(tabList, supportFragmentManager, this.lifecycle)
        binding.appsVp.apply {
            adapter = tabAdapter
            offscreenPageLimit = 1
        }
        TabLayoutMediator(binding.appsTab, binding.appsVp) { tab, position ->
            tab.text = tabText[position]
        }.attach()
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