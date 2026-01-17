package com.shizq.bika.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shizq.bika.BR
import com.shizq.bika.MainActivity
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivitySplashBinding
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.utils.SPUtil


@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {
    override fun initParam() {
        super.initParam()
        installSplashScreen()
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_splash
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initViewObservable() {
        //检查是否有token 没有就进行登录 显示登录提示框
        if (SPUtil.get("token", "") != "") {
            //没有token 挑转到登录页面
            startActivity(AccountActivity::class.java)
            finish()
        } else {
            //有token 跳转主页
            startActivity(MainActivity::class.java)
            finish()
        }
    }
}