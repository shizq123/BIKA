package com.shizq.bika.ui.account

//import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.shizq.bika.ui.signin.LoginScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(
                onLoginSuccess = {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                },
                onSignUpClick = { Toast.makeText(this, "立即注册", Toast.LENGTH_SHORT).show() },
                onForgotPasswordClick = {
                    Toast.makeText(this, "忘记密码", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}