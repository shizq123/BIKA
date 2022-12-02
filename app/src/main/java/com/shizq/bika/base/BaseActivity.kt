package com.shizq.bika.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<V : ViewDataBinding, VM : BaseViewModel> : AppCompatActivity(), IBaseView {
    protected lateinit var binding: V
    protected lateinit var viewModel: VM
    private var viewModelId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //页面接受的参数方法
        initParam()
        //私有的初始化 binding和ViewModel方法
        initViewDataBinding(savedInstanceState)
        initToolbar()
        //页面数据初始化方法
        initData()
        //页面事件监听的方法，一般用于ViewModel层转到View层的事件注册
        initViewObservable()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
    }

    private fun initViewDataBinding(savedInstanceState: Bundle?) {
        //DataBindingUtil类需要在project的build中配置 dataBinding {enabled true }, 同步后会自动关联android.binding包
        binding = DataBindingUtil.setContentView(this, initContentView(savedInstanceState))
        viewModelId = initVariableId()
        //        viewModel = initViewModel()
        binding.lifecycleOwner = this
        val modelClass: Class<VM>
        val type = javaClass.genericSuperclass

        modelClass = if (type is ParameterizedType) {
            type.actualTypeArguments[1] as Class<VM>
        } else {
            //如果没有指定泛型参数，则默认使用BaseViewModel
            BaseViewModel::class.java as Class<VM>
        }
        viewModel = createViewModel(this, modelClass)
        //关联ViewModel
        binding.setVariable(viewModelId, viewModel)
        //让ViewModel拥有View的生命周期感应
        lifecycle.addObserver(viewModel)
    }

    private fun initToolbar() {
    }

    override fun initParam() {

    }

    /**
     * 初始化根布局
     *
     * @return 布局layout的id
     */
    abstract fun initContentView(savedInstanceState: Bundle?): Int

    /**
     * 初始化ViewModel的id
     *
     * @return BR的id
     */
    abstract fun initVariableId(): Int

    override fun initData() {}

    override fun initViewObservable() {}

    override fun onContentReload() {}

    /**
     * @param cls 类
     * @param <T> 泛型参数,必须继承ViewMode
     * @return 生成的viewMode实例
    </T> */
    private fun <T : ViewModel> createViewModel(activity: FragmentActivity, cls: Class<T>): T {
        return ViewModelProvider(activity).get(cls)
    }
    /**
     * 跳转页面
     * @param clz 所跳转的目的Activity类
     */
    fun startActivity(clz: Class<*>?) {
        startActivity(Intent(this, clz))
    }

    /**
     * 跳转页面
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    fun startActivity(clz: Class<*>?, bundle: Bundle?) {
        val intent = Intent(this, clz)
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        startActivity(intent)
    }

}