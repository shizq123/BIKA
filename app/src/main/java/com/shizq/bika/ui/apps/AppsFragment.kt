package com.shizq.bika.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseFragment
import com.shizq.bika.databinding.FragmentAppsBinding

class AppsFragment (val str: String): BaseFragment<FragmentAppsBinding, AppsFragmentViewModel>() {
    override fun initContentView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): Int {
        return R.layout.fragment_apps
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {

    }
}