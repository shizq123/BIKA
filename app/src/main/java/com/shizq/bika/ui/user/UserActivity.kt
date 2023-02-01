package com.shizq.bika.ui.user

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityUserBinding
import com.shizq.bika.utils.*
import com.yalantis.ucrop.UCrop

class UserActivity : BaseActivity<ActivityUserBinding, UserViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_user
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.userInclude.toolbar.title = "编辑"
        setSupportActionBar(binding.userInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fileServer = SPUtil.get(this, "user_fileServer", "") as String
        val path = SPUtil.get(this, "user_path", "") as String
        val character = SPUtil.get(this, "user_character", "") as String

        if (fileServer != "") {
            GlideApp.with(this)
                .load(GlideUrlNewKey(fileServer, path))
                .centerCrop()
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(binding.userAvatar)
        }
        if (character != "") {
            GlideApp.with(this)
                .load(character)
                .into(binding.userCharacter)
        }

        binding.userNickname.text = SPUtil.get(this, "user_name", "") as String
        binding.userUsername.text = SPUtil.get(this, "username", "") as String
        binding.userBirthday.text =
            (SPUtil.get(this, "user_birthday", "") as String).subSequence(0, 10)
        binding.userSlogan.text = SPUtil.get(this, "user_slogan", "") as String

        initListener()
    }

    private fun initListener() {
        binding.userAvatarLayout.setOnClickListener {
            PictureSelector.create(this)
                .openGallery(SelectMimeType.ofImage())
                .isCameraForegroundService(true)
                .setSelectionMode(1)
                .setImageEngine(GlideEngine.createGlideEngine())
                .setCropEngine { fragment, srcUri, destinationUri, dataSource, requestCode ->
                    UCrop.of(srcUri, destinationUri, dataSource)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(200, 200) //图片压缩没官方清晰
                        .start(fragment.requireActivity(), fragment, requestCode)
                }
                .forResult(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: ArrayList<LocalMedia>) {
                        GlideApp.with(this@UserActivity)
                            .load(R.drawable.placeholder_avatar_2)
                            .into(binding.userAvatar)

                        binding.userProgressbar.show()
                        binding.userAvatarLayout.isEnabled = false
                        binding.userSloganLayout.isEnabled = false
                        viewModel.putAvatar(Base64Util().getBase64(result[0].cutPath))
                    }

                    override fun onCancel() {}
                })
        }

        binding.userSloganLayout.setOnClickListener {
            val view = View.inflate(this, R.layout.view_dialog_edit_text_slogan, null)
            val slogan = view.findViewById<TextInputEditText>(R.id.edittext1)
            slogan.setText(binding.userSlogan.text)
            slogan.requestFocus()

            MaterialAlertDialogBuilder(this)
                .setTitle("自我介绍")
                .setView(view)
                .setPositiveButton("确定") { dialog, which ->
                    binding.userProgressbar.show()
                    binding.userAvatarLayout.isEnabled = false
                    binding.userSloganLayout.isEnabled = false
                    viewModel.putProfile(slogan.text.toString())
                }
                .setNegativeButton("取消", null)
                .show()
        }

        //上传头像
        viewModel.liveData_avatar.observe(this) {
            if (it.code == 200) {
                //成功就重新获取个人信息
                viewModel.getProfile()
            } else {
                binding.userProgressbar.hide()
                binding.userAvatarLayout.isEnabled = true
                binding.userSloganLayout.isEnabled = true
                Toast.makeText(this, "更换头像失败", Toast.LENGTH_SHORT).show()
                //失败切换为原来的头像
                val fileServer = SPUtil.get(this, "user_fileServer", "") as String
                val path = SPUtil.get(this, "user_path", "") as String
                GlideApp.with(this)
                    .load(
                        if (path != "") {
                            GlideUrlNewKey(fileServer, path)
                        } else R.drawable.placeholder_avatar_2
                    )
                    .placeholder(R.drawable.placeholder_avatar_2)
                    .into(binding.userAvatar)
            }
        }

        //上传自我介绍 签名
        viewModel.liveDataSlogan.observe(this) {
            if (it.code == 200) {
                //成功就重新获取个人信息
                viewModel.getProfile()
            } else {
                binding.userProgressbar.hide()
                binding.userAvatarLayout.isEnabled = true
                binding.userSloganLayout.isEnabled = true
                Toast.makeText(this, "上传自我介绍失败", Toast.LENGTH_SHORT).show()
            }
        }

        //获取用户信息
        viewModel.liveData_profile.observe(this) {
            binding.userProgressbar.hide()
            binding.userAvatarLayout.isEnabled = true
            binding.userSloganLayout.isEnabled = true
            if (it.code == 200) {
                //
                var fileServer = ""
                var path = ""
                var character = ""
                if (it.data.user.avatar != null) { //头像
                    fileServer = it.data.user.avatar.fileServer
                    path = it.data.user.avatar.path
                    GlideApp.with(this)
                        .load(GlideUrlNewKey(fileServer, path))
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .into(binding.userAvatar)
                }
                if (it.data.user.character != null) { //头像框 新用户没有
                    character = it.data.user.character
                    GlideApp.with(this)
                        .load(character)
                        .into(binding.userCharacter)
                }

                binding.userSlogan.text =
                    if (it.data.user.slogan.isNullOrBlank()) "" else it.data.user.slogan

                //存一下当前用户信息
                SPUtil.put(this, "user_fileServer", fileServer)
                SPUtil.put(this, "user_path", path)
                SPUtil.put(this, "user_level", it.data.user.level)
                SPUtil.put(this, "user_exp", it.data.user.exp)
                SPUtil.put(
                    this,
                    "user_slogan",
                    if (it.data.user.slogan.isNullOrBlank()) "" else it.data.user.slogan
                )

            } else {
                //网络错误
                MaterialAlertDialogBuilder(this)
                    .setMessage("网络错误，获取用户信息失败")
                    .setPositiveButton("重试") { _, _ ->
                        binding.userProgressbar.show()
                        binding.userAvatarLayout.isEnabled = false
                        binding.userSloganLayout.isEnabled = false
                        viewModel.getProfile()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
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