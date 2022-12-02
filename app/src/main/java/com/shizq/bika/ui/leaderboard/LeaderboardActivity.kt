package com.shizq.bika.ui.leaderboard

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.tabs.TabLayoutMediator
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ViewPagerAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityLeaderboardBinding

/**
 * 排行榜
 */

class LeaderboardActivity : BaseActivity<ActivityLeaderboardBinding,LeaderboardViewModel>() {
    val tabText = listOf(
        "日榜",
        "周榜",
        "月榜",
        "骑士榜"
    )
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_leaderboard
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }
    override fun initData() {
        binding.toolbar.title = "排行榜"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabList = listOf(
            LeaderboardDayFragment("H24"),LeaderboardDayFragment("D7"),LeaderboardDayFragment("D30"),LeaderboardKnightFragment()
        )
        val tabAdapter=ViewPagerAdapter(tabList, supportFragmentManager, this.lifecycle)
        binding.leaderboardVp.apply {
            adapter = tabAdapter
            offscreenPageLimit = 1
        }
        TabLayoutMediator(binding.leaderboardTab, binding.leaderboardVp) { tab, position ->
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