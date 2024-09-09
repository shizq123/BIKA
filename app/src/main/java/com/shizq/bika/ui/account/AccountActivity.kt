package com.shizq.bika.ui.account

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityAccountBinding

//登录注册
class AccountActivity : BaseActivity<ActivityAccountBinding, AccountViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_account
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        setSupportActionBar(binding.loginInclude.toolbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                //只有注册页有返回键 所以回退到登录页
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //返回键执行home键 //TODO 需要优化 添加到 fragment中
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        //只有一级所以就不判断了 只有注册页面会返回到登录页 登录页返回是退出应用
        supportActionBar?.title = "登录"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}