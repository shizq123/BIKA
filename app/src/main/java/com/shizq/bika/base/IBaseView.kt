package com.shizq.bika.base

interface IBaseView {
    /**
     * 初始化界面传递参数
     */
    fun initParam()

    /**
     * 初始化数据
     */
    fun initData()

    /**
     * 初始化界面观察者的监听
     */
    fun initViewObservable()

    /**
     * 列表无数据点击加载网络功能
     */
    fun onContentReload()
}