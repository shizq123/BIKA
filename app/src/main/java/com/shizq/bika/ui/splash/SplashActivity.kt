package com.shizq.bika.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.bean.InitBean
import com.shizq.bika.databinding.ActivitySplashBinding
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.ui.main.MainActivity
import com.shizq.bika.utils.AppVersion
import com.shizq.bika.utils.SPUtil


@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {
    private lateinit var splashScreen: SplashScreen

    override fun initParam() {
        super.initParam()
        splashScreen = installSplashScreen()
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_splash
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        showProgressBar(true,"获取版本信息...")//加载时 view不可点击
        viewModel.getLatestVersion()//版本检测
    }

    @SuppressLint("SetTextI18n")
    override fun initViewObservable() {
        super.initViewObservable()

        //更新
        viewModel.liveData_latest_version.observe(this) {
            if (it != null && it.version.toInt() > AppVersion().code()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("新版本 v${it.short_version}")
                    .setMessage(it.release_notes)
                    .setCancelable(false)
                    .setPositiveButton("更新") { _, _ ->
                        val intent = Intent()
                        intent.action = "android.intent.action.VIEW"
                        intent.data = Uri.parse(it.download_url)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("取消"){_, _ ->
                        showProgressBar(true,"获取节点信息...")
                        viewModel.getInit()// 网络请求获取 节点一节点二
                    }
                    .show()
            } else {
                showProgressBar(true,"获取节点信息...")
                viewModel.getInit()// 网络请求获取 节点一节点二
            }
        }


        //节点
        viewModel.liveData_init.observe(this, Observer { initBean: InitBean ->
            if (initBean.status == "ok") {
                //查看是否有默认节点 没有就存一个


                //保存两个host地址
                //2024.3.8 能跑以后改
                if (!initBean.addresses.isNullOrEmpty()) {
                SPUtil.put(this,"addresses1",initBean.addresses[0])
                SPUtil.put(this,"addresses2",initBean.addresses[1])
                }else if (!initBean.address.isNullOrEmpty()) {
                    //2024.3.8 哔咔数据改变
                    SPUtil.put(this,"addresses1",initBean.address[0])
                    SPUtil.put(this,"addresses2",initBean.address[1])
                } else {
                    //2024.3.8 防闪退加入哔咔常用的ip(以后不确定能用)
                    SPUtil.put(this,"addresses1","172.67.194.19")
                    SPUtil.put(this,"addresses2","104.21.20.188")
                }

                //检查是否有token 没有就进行登录 显示登录提示框
                if (SPUtil.get(this,"token", "") == "") {
                    //没有token 挑转到登录页面
                    startActivity(AccountActivity::class.java)
                    finish()
                } else {
                    //有token 跳转主页
                    startActivity(MainActivity::class.java)
                    finish()
                }
            } else {
                //请求init失败 提示网络错误
                showProgressBar(false,"网络错误，点击重试")

            }
        })
        //节点 其他网络失败
        viewModel.liveData_init_error.observe(this, Observer {
            showProgressBar(false,"网络错误，点击重试\n$it")
        })

        //网络重试点击事件监听
        binding.loadLayout.setOnClickListener {
            showProgressBar(true,"获取节点信息...")
            viewModel.getInit()
        }
    }

    private fun showProgressBar(show: Boolean, string: String){
        binding.loadProgressBar.visibility=if (show)View.VISIBLE else View.GONE
        binding.loadError.visibility=if (show)View.GONE else View.VISIBLE
        binding.loadText.text = string
        binding.loadLayout.isEnabled = !show
    }

}