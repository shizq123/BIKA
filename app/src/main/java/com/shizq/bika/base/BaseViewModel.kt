package com.shizq.bika.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer

open class BaseViewModel(application: Application) : AndroidViewModel(application),
    DefaultLifecycleObserver, Consumer<Disposable> {

    //管理RxJava，主要针对RxJava异步操作造成的内存泄漏
    private var mCompositeDisposable: CompositeDisposable?
    private fun addSubscribe(disposable: Disposable?) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = CompositeDisposable()
        }
        mCompositeDisposable!!.add(disposable!!)
    }


    override fun onCleared() {
        super.onCleared()
        //ViewModel销毁时会执行，同时取消所有异步任务
        if (mCompositeDisposable != null) {
            mCompositeDisposable!!.clear()
        }
    }

    @Throws(Exception::class)
    override fun accept(disposable: Disposable) {
        addSubscribe(disposable)
    }

    init {
        mCompositeDisposable = CompositeDisposable()
    }
}