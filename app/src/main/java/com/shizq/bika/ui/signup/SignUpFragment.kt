package com.shizq.bika.ui.signup

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseFragment
import com.shizq.bika.databinding.FragmentSignupBinding
import com.shizq.bika.utils.SPUtil
import java.text.SimpleDateFormat
import java.util.*


/**
 * 注册
 */

class SignUpFragment : BaseFragment<FragmentSignupBinding, SignUpViewModel>() {
    private val da = arrayOf(1, 3, 5, 7, 8, 10, 12)
    private val xiao = arrayOf(4, 6, 9, 11)

    override fun initContentView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): Int {
        return R.layout.fragment_signup
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        (activity as AppCompatActivity).supportActionBar?.title = "注册"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initListener()

    }

    private fun initListener() {
        binding.signupBtn.setOnClickListener {
            if (TextUtils.isEmpty(binding.signupNicknameEdit.text.toString().trim())) {
                binding.signupNicknameLayout.isErrorEnabled = true
                binding.signupNicknameLayout.error = "昵称不能为空！"
            } else if (binding.signupNicknameEdit.text.toString().trim().length < 2) {
                binding.signupNicknameLayout.isErrorEnabled = true
                binding.signupNicknameLayout.error = "昵称不能为小于2字！"
            }
            if (TextUtils.isEmpty(binding.signupUsernameEdit.text.toString().trim())) {
                binding.signupUsernameLayout.isErrorEnabled = true
                binding.signupUsernameLayout.error = "登录账号不能为空！"
            }
            if (TextUtils.isEmpty(binding.signupPasswordEdit.text.toString().trim())) {
                binding.signupPasswordLayout.isErrorEnabled = true
                binding.signupPasswordLayout.error = "登录密码不能为空！"
            } else if (binding.signupConfirmPasswordEdit.text.toString().trim().length < 8) {
                binding.signupPasswordLayout.isErrorEnabled = true
                binding.signupPasswordLayout.error = "登录密码不能小于8字！"
            }
            if (TextUtils.isEmpty(binding.signupConfirmPasswordEdit.text.toString().trim())) {
                binding.signupConfirmPasswordLayout.isErrorEnabled = true
                binding.signupConfirmPasswordLayout.error = "确认密码不能为空！"
            } else if (binding.signupConfirmPasswordEdit.text.toString()
                    .trim() != binding.signupPasswordEdit.text.toString().trim()
            ) {
                binding.signupConfirmPasswordLayout.isErrorEnabled = true
                binding.signupConfirmPasswordLayout.error = "确认密码与登录密码不符！"
            }

            if (TextUtils.isEmpty(binding.signupDateYearEdit.text.toString().trim())) {
                binding.signupDateYearLayout.isErrorEnabled = true
                binding.signupDateYearLayout.error = "年份不能为空"
            } else if (binding.signupDateYearEdit.text.toString()
                    .toInt() > 2022 || binding.signupDateYearEdit.text.toString().toInt() < 1900
            ) {
                binding.signupDateYearLayout.isErrorEnabled = true
                binding.signupDateYearLayout.error = "请输入有效的日期"
            }
            if (TextUtils.isEmpty(binding.signupDateMonthEdit.text.toString().trim())) {
                binding.signupDateMonthLayout.isErrorEnabled = true
                binding.signupDateMonthLayout.error = "月份不能为空"
            } else if (binding.signupDateMonthEdit.text.toString()
                    .toInt() > 12 || binding.signupDateMonthEdit.text.toString().toInt() <= 0
            ) {
                binding.signupDateMonthLayout.isErrorEnabled = true
                binding.signupDateMonthLayout.error = "请输入有效的日期"
            }
            if (TextUtils.isEmpty(binding.signupDateDayEdit.text.toString().trim())) {
                binding.signupDateDayLayout.isErrorEnabled = true
                binding.signupDateDayLayout.error = "年份不能为空"
            } else if (binding.signupDateMonthEdit.text.toString().toInt() > 12
                || binding.signupDateMonthEdit.text.toString().toInt() <= 0
            ) {
                binding.signupDateDayLayout.isErrorEnabled = true
                binding.signupDateDayLayout.error = "请输入有效的月份"
            } else {

                if (binding.signupDateMonthEdit.text.toString().toInt() == 2) {
                    if (binding.signupDateYearEdit.text.toString().toInt() % 4 == 0) {
                        if (binding.signupDateDayEdit.text.toString().toInt() > 29
                            || binding.signupDateDayEdit.text.toString().toInt() <= 0
                        ) {
                            binding.signupDateDayLayout.isErrorEnabled = true
                            binding.signupDateDayLayout.error = "请输入有效的日期"
                        }
                    } else {
                        if (binding.signupDateDayEdit.text.toString().toInt() > 28
                            || binding.signupDateDayEdit.text.toString().toInt() <= 0
                        ) {
                            binding.signupDateDayLayout.isErrorEnabled = true
                            binding.signupDateDayLayout.error = "请输入有效的日期"
                        }
                    }

                } else if (da.contains(binding.signupDateMonthEdit.text.toString().toInt())) {
                    if (binding.signupDateDayEdit.text.toString().toInt() > 31
                        || binding.signupDateDayEdit.text.toString().toInt() <= 0
                    ) {
                        binding.signupDateDayLayout.isErrorEnabled = true
                        binding.signupDateDayLayout.error = "请输入有效的日期"
                    }
                } else if (xiao.contains(binding.signupDateMonthEdit.text.toString().toInt())) {
                    if (binding.signupDateDayEdit.text.toString().toInt() > 30
                        || binding.signupDateDayEdit.text.toString().toInt() <= 0
                    ) {
                        binding.signupDateDayLayout.isErrorEnabled = true
                        binding.signupDateDayLayout.error = "请输入有效的日期"
                    }
                }
            }
            if (TextUtils.isEmpty(binding.signupGenderEdit.text.toString().trim())) {
                binding.signupGenderLayout.isErrorEnabled = true
                binding.signupGenderLayout.error = "性别不能为空！"
            }

            if (TextUtils.isEmpty(binding.signupSafetyProblemEdit1.text.toString().trim())) {
                binding.signupSafetyProblemLayout1.isErrorEnabled = true
                binding.signupSafetyProblemLayout1.error = "安全问题1 不能为空！"
            }
            if (TextUtils.isEmpty(binding.signupSafetyProblemEdit2.text.toString().trim())) {
                binding.signupSafetyProblemLayout2.isErrorEnabled = true
                binding.signupSafetyProblemLayout2.error = "安全问题2 不能为空！"
            }
            if (TextUtils.isEmpty(binding.signupSafetyProblemEdit3.text.toString().trim())) {
                binding.signupSafetyProblemLayout3.isErrorEnabled = true
                binding.signupSafetyProblemLayout3.error = "安全问题3 不能为空！"
            }
            if (TextUtils.isEmpty(binding.signupAnswerEdit1.text.toString().trim())) {
                binding.signupAnswerLayout1.isErrorEnabled = true
                binding.signupAnswerLayout1.error = "答案1 不能为空！"
            }
            if (TextUtils.isEmpty(binding.signupAnswerEdit2.text.toString().trim())) {
                binding.signupAnswerLayout2.isErrorEnabled = true
                binding.signupAnswerLayout2.error = "答案2 不能为空！"
            }
            if (TextUtils.isEmpty(binding.signupAnswerEdit3.text.toString().trim())) {
                binding.signupAnswerLayout3.isErrorEnabled = true
                binding.signupAnswerLayout3.error = "答案3 不能为空！"
            }

            if (!TextUtils.isEmpty(binding.signupNicknameEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupUsernameEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupPasswordEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupConfirmPasswordEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupDateYearEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupDateMonthEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupDateDayEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupGenderEdit.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupSafetyProblemEdit1.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupSafetyProblemEdit2.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupSafetyProblemEdit3.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupAnswerEdit1.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupAnswerEdit2.text.toString().trim())
                && !TextUtils.isEmpty(binding.signupAnswerEdit3.text.toString().trim())
            ) {
                //所有输入框不为空时 向下执行
                if (binding.signupNicknameEdit.text.toString().trim().length >= 2
                    && (binding.signupConfirmPasswordEdit.text.toString().trim()
                            == binding.signupPasswordEdit.text.toString().trim())
                    && binding.signupConfirmPasswordEdit.text.toString().trim().length >= 8
                    && binding.signupDateYearEdit.text.toString().toInt() in 1900..2022
                    && binding.signupDateMonthEdit.text.toString().toInt() in 1..12
                ) {
                    //密码和确认密码一致并且所有年月日期正确时 向下执行
                    if ((binding.signupDateMonthEdit.text.toString().toInt() == 2
                                && binding.signupDateYearEdit.text.toString().toInt() % 4 == 0
                                && binding.signupDateDayEdit.text.toString().toInt() in 1..29)
                        || (binding.signupDateMonthEdit.text.toString().toInt() == 2
                                && binding.signupDateYearEdit.text.toString().toInt() % 4 != 0
                                && binding.signupDateDayEdit.text.toString().toInt() in 1..28)
                        || (da.contains(binding.signupDateMonthEdit.text.toString().toInt())
                                && binding.signupDateDayEdit.text.toString().toInt() in 1..31)
                        || (xiao.contains(binding.signupDateMonthEdit.text.toString().toInt())
                                && binding.signupDateDayEdit.text.toString().toInt() in 1..30)
                    ) {
                        // 这里月份四个条件，符合其中一条就说明日期正确 进行下一步
                        // 1.当2月时 闰年 29天
                        // 2.当2月时 非闰年 28天
                        // 3.当大月时 31天
                        // 4.当小月时 30天


                        //判断是否超过18岁 超过18岁向下执行
                        if (y18(
                                binding.signupDateYearEdit.text.toString().toInt(),
                                binding.signupDateMonthEdit.text.toString().toInt(),
                                binding.signupDateDayEdit.text.toString().toInt()
                            )
                        ) {

                            viewModel.name = binding.signupNicknameEdit.text.toString().trim()
                            viewModel.email = binding.signupUsernameEdit.text.toString().trim()
                            viewModel.password = binding.signupPasswordEdit.text.toString().trim()
                            viewModel.birthday = "${binding.signupDateYearEdit.text.toString()}-${
                                String.format(
                                    "%02d",
                                    binding.signupDateMonthEdit.text.toString().toInt()
                                )
                            }-${
                                String.format(
                                    "%02d",
                                    binding.signupDateDayEdit.text.toString().toInt()
                                )
                            }"
                            viewModel.question1 =
                                binding.signupSafetyProblemEdit1.text.toString().trim()
                            viewModel.question2 =
                                binding.signupSafetyProblemEdit2.text.toString().trim()
                            viewModel.question3 =
                                binding.signupSafetyProblemEdit3.text.toString().trim()
                            viewModel.answer1 = binding.signupAnswerEdit1.text.toString().trim()
                            viewModel.answer2 = binding.signupAnswerEdit2.text.toString().trim()
                            viewModel.answer3 = binding.signupAnswerEdit3.text.toString().trim()

                            //显示加载进度条
                            hideProgressBar(false)

                            viewModel.requestSignUp()

                        } else {
                            Toast.makeText(activity, "未满18岁不能注册", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.signupGenderEdit.setOnClickListener {
            showPopupMenu(binding.signupGenderEdit)
        }

        binding.signupNicknameEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupNicknameLayout.isErrorEnabled = false
            }
        })
        binding.signupUsernameEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupUsernameLayout.isErrorEnabled = false
            }
        })
        binding.signupPasswordEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupPasswordLayout.isErrorEnabled = false
            }
        })
        binding.signupConfirmPasswordEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupConfirmPasswordLayout.isErrorEnabled = false
            }
        })
        binding.signupDateYearEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupDateYearLayout.isErrorEnabled = false
            }
        })
        binding.signupDateMonthEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupDateMonthLayout.isErrorEnabled = false
            }
        })
        binding.signupDateDayEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupDateDayLayout.isErrorEnabled = false
            }
        })
        binding.signupGenderEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupGenderLayout.isErrorEnabled = false
            }
        })
        binding.signupSafetyProblemEdit1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupSafetyProblemLayout1.isErrorEnabled = false
            }
        })
        binding.signupSafetyProblemEdit2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupSafetyProblemLayout2.isErrorEnabled = false
            }
        })
        binding.signupSafetyProblemEdit3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupSafetyProblemLayout3.isErrorEnabled = false
            }
        })
        binding.signupAnswerEdit1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupAnswerLayout1.isErrorEnabled = false
            }
        })
        binding.signupAnswerEdit2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupAnswerLayout2.isErrorEnabled = false
            }
        })
        binding.signupAnswerEdit3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.signupAnswerLayout3.isErrorEnabled = false
            }
        })
    }

    override fun initViewObservable() {
        //网络请求结果
        viewModel.liveData_signup.observe(this) {
            hideProgressBar(true)
            //进度条
            if (it.code == 200) {
                // 注册成功 保存账号密码
//                MmkvUtils.putSet("username", viewModel.email.toString())
//                MmkvUtils.putSet("password", viewModel.password.toString())
                SPUtil.put(context,"username", viewModel.email.toString())
                SPUtil.put(context,"password", viewModel.password.toString())
                // 提示是否继续登录
                MaterialAlertDialogBuilder(activity as AppCompatActivity)
                    .setTitle("注册成功是否继续登录")
                    .setPositiveButton("确定") { dialog, which ->
                        // TODO 需要优化 添加 自动登录 或者 跳转到登录页（登录页要显示注册的账号密码）
//                        Navigation.findNavController(activity as AppCompatActivity,R.id.login_fcv).navigateUp()
                        Navigation.findNavController(activity as AppCompatActivity,R.id.login_fcv).navigate(R.id.action_signUpFragment_to_signInFragment)

                    }
                    .setNegativeButton("取消"){ dialog, which ->

                    }
                    .show()

            } else {
                //注册失败提示网络错误
                MaterialAlertDialogBuilder(activity as AppCompatActivity)
                    .setTitle("注册失败")
                    .setMessage("网络错误code=${it.code} error=${it.error} message=${it.message}")
                    .setPositiveButton("重试") { dialog, which ->
                        hideProgressBar(false)
                        viewModel.requestSignUp()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

    }

    private fun showPopupMenu(view: View) {
        // View当前PopupMenu显示的相对View的位置
        val popupMenu = PopupMenu(activity as AppCompatActivity, view)
        // menu布局
        popupMenu.menuInflater.inflate(R.menu.activity_sigup_gender, popupMenu.menu)
        // menu的item点击事件
        popupMenu.setOnMenuItemClickListener { item ->
            binding.signupGenderEdit.setText(item.title)
            viewModel.gender = when (item.itemId) {
                R.id.menu_id_nan -> "m"
                R.id.menu_id_nv -> "f"
                else -> "bot"
            }
            false
        }
        popupMenu.show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun y18(year: Int, month: Int, day: Int): Boolean {
        //检测年龄过18岁的工具类
        val date = Date()

        val YEAR = SimpleDateFormat("yyyy").format(date).toInt()
        if ((YEAR - year) > 18) {
            return true
        } else if ((YEAR - year) < 18) {
            return false
        }

        val MONTH = SimpleDateFormat("MM").format(date).toInt()
        if (MONTH > month) {
            return true
        } else if (MONTH < month) {
            return false
        }

        return SimpleDateFormat("dd").format(date).toInt() > day
    }

    private fun hideProgressBar(boolean: Boolean) {
        binding.signupProgressBar.visibility = if (boolean) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.signupNicknameEdit.isEnabled = boolean
        binding.signupUsernameEdit.isEnabled = boolean
        binding.signupPasswordEdit.isEnabled = boolean
        binding.signupConfirmPasswordEdit.isEnabled = boolean
        binding.signupDateYearEdit.isEnabled = boolean
        binding.signupDateMonthEdit.isEnabled = boolean
        binding.signupDateDayEdit.isEnabled = boolean
        binding.signupGenderEdit.isEnabled = boolean
        binding.signupSafetyProblemEdit1.isEnabled = boolean
        binding.signupSafetyProblemEdit2.isEnabled = boolean
        binding.signupSafetyProblemEdit3.isEnabled = boolean
        binding.signupAnswerEdit1.isEnabled = boolean
        binding.signupAnswerEdit2.isEnabled = boolean
        binding.signupAnswerEdit3.isEnabled = boolean
        binding.signupBtn.isEnabled = boolean
    }

}

