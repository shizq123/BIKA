package com.shizq.bika.ui.user

import androidx.appcompat.app.AppCompatActivity

class UserActivity : AppCompatActivity() {
//
//    override fun initContentView(savedInstanceState: Bundle?): Int {
//        return R.layout.activity_user
//    }
//
//    override fun initVariableId(): Int {
//        return BR.viewModel
//    }
//
//    override fun initData() {
//        binding.userInclude.toolbar.title = "编辑"
//        setSupportActionBar(binding.userInclude.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        val fileServer = SPUtil.get("user_fileServer", "") as String
//        val path = SPUtil.get("user_path", "") as String
//        val character = SPUtil.get("user_character", "") as String
//
//        if (fileServer != "") {
//            Glide.with(this)
//                .load(GlideUrlNewKey(fileServer, path))
//                .centerCrop()
//                .placeholder(R.drawable.placeholder_avatar_2)
//                .into(binding.userAvatar)
//        }
//        if (character != "") {
//            Glide.with(this)
//                .load(character)
//                .into(binding.userCharacter)
//        }
//
//        binding.userNickname.text = SPUtil.get("user_name", "") as String
//        binding.userUsername.text = SPUtil.get("username", "") as String
//        binding.userBirthday.text =
//            TimeUtil().getBirthday(SPUtil.get("user_birthday", "") as String)
//        binding.userCreatedAt.text =
//            TimeUtil().getBirthday(SPUtil.get("user_created_at", "") as String)
//        binding.userSlogan.text = SPUtil.get("user_slogan", "") as String
//
//        initListener()
//    }
//
//    private fun initListener() {
//        binding.userAvatarLayout.setOnClickListener {
//            PictureSelector.create(this)
//                .openGallery(SelectMimeType.ofImage())
//                .isCameraForegroundService(true)
//                .setSelectionMode(1)
//                .setImageEngine(GlideEngine.createGlideEngine())
//                .setCropEngine { fragment, srcUri, destinationUri, dataSource, requestCode ->
//                    UCrop.of(srcUri, destinationUri, dataSource)
//                        .withAspectRatio(1f, 1f)
//                        .withMaxResultSize(200, 200) //图片压缩没官方清晰
//                        .start(fragment.requireActivity(), fragment, requestCode)
//                }
//                .forResult(object : OnResultCallbackListener<LocalMedia> {
//                    override fun onResult(result: ArrayList<LocalMedia>) {
//                        Glide.with(this@UserActivity)
//                            .load(R.drawable.placeholder_avatar_2)
//                            .into(binding.userAvatar)
//
//                        binding.userProgressbar.show()
//                        binding.userAvatarLayout.isEnabled = false
//                        binding.userSloganLayout.isEnabled = false
//                        viewModel.putAvatar(Base64Util().getBase64(result[0].cutPath))
//                    }
//
//                    override fun onCancel() {}
//                })
//        }
//
//        binding.userSloganLayout.setOnClickListener {
//            val view = View.inflate(this, R.layout.view_dialog_edit_text_slogan, null)
//            val slogan = view.findViewById<TextInputEditText>(R.id.edittext1)
//            slogan.setText(binding.userSlogan.text)
//            slogan.requestFocus()
//
//            MaterialAlertDialogBuilder(this)
//                .setTitle(R.string.user_slogan)
//                .setView(view)
//                .setPositiveButton("确定") { dialog, which ->
//                    binding.userProgressbar.show()
//                    binding.userAvatarLayout.isEnabled = false
//                    binding.userSloganLayout.isEnabled = false
//                    viewModel.putProfile(slogan.text.toString())
//                }
//                .setNegativeButton("取消", null)
//                .show()
//        }
//
//        //上传头像
//        viewModel.liveData_avatar.observe(this) {
//            if (it.code == 200) {
//                //成功就重新获取个人信息
//                viewModel.getProfile()
//            } else {
//                binding.userProgressbar.hide()
//                binding.userAvatarLayout.isEnabled = true
//                binding.userSloganLayout.isEnabled = true
//                Toast.makeText(this, "更换头像失败", Toast.LENGTH_SHORT).show()
//                //失败切换为原来的头像
//                val fileServer = SPUtil.get("user_fileServer", "") as String
//                val path = SPUtil.get("user_path", "") as String
//                Glide.with(this)
//                    .load(
//                        if (path != "") {
//                            GlideUrlNewKey(fileServer, path)
//                        } else R.drawable.placeholder_avatar_2
//                    )
//                    .placeholder(R.drawable.placeholder_avatar_2)
//                    .into(binding.userAvatar)
//            }
//        }
//
//        //上传自我介绍 签名
//        viewModel.liveDataSlogan.observe(this) {
//            if (it.code == 200) {
//                //成功就重新获取个人信息
//                viewModel.getProfile()
//            } else {
//                binding.userProgressbar.hide()
//                binding.userAvatarLayout.isEnabled = true
//                binding.userSloganLayout.isEnabled = true
//                Toast.makeText(this, "上传自我介绍失败", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        //获取用户信息
//        viewModel.liveData_profile.observe(this) {
//            binding.userProgressbar.hide()
//            binding.userAvatarLayout.isEnabled = true
//            binding.userSloganLayout.isEnabled = true
//            if (it.code == 200) {
//                //
//                var fileServer = ""
//                var path = ""
//                var character = ""
//                if (it.data.user.avatar != null) { //头像
//                    fileServer = it.data.user.avatar.fileServer
//                    path = it.data.user.avatar.path
//                    Glide.with(this)
//                        .load(GlideUrlNewKey(fileServer, path))
//                        .centerCrop()
//                        .placeholder(R.drawable.placeholder_avatar_2)
//                        .into(binding.userAvatar)
//                }
//                if (it.data.user.character != null) { //头像框 新用户没有
//                    character = it.data.user.character
//                    Glide.with(this)
//                        .load(character)
//                        .into(binding.userCharacter)
//                }
//
//                binding.userSlogan.text =
//                    if (it.data.user.slogan.isNullOrBlank()) "" else it.data.user.slogan
//
//                //存一下当前用户信息
//                SPUtil.put("user_fileServer", fileServer)
//                SPUtil.put("user_path", path)
//                SPUtil.put("user_level", it.data.user.level)
//                SPUtil.put("user_exp", it.data.user.exp)
//                SPUtil.put(
//                    "user_slogan",
//                    if (it.data.user.slogan.isNullOrBlank()) "" else it.data.user.slogan
//                )
//
//            } else {
//                //网络错误
//                MaterialAlertDialogBuilder(this)
//                    .setMessage("网络错误，获取用户信息失败")
//                    .setPositiveButton("重试") { _, _ ->
//                        binding.userProgressbar.show()
//                        binding.userAvatarLayout.isEnabled = false
//                        binding.userSloganLayout.isEnabled = false
//                        viewModel.getProfile()
//                    }
//                    .setNegativeButton("取消", null)
//                    .show()
//            }
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> {
//                finish()
//            }
//        }
//
//        return super.onOptionsItemSelected(item)
//    }
//
//}
}