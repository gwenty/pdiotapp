package com.specknet.pdiotapp.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.specknet.pdiotapp.MainActivity
import com.specknet.pdiotapp.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.EmailEditText


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnRegLogin.setOnClickListener {
            startActivity(Intent(this,RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
        }

        loginButton.setOnClickListener {
            when {
                TextUtils.isEmpty(EmailEditText.text.toString().trim {it <= ' '}) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Email field empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(PasswordEditText.text.toString().trim {it <= ' '}) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Password field empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    val email: String = EmailEditText.text.toString().trim {it <= ' '}
                    val password: String = PasswordEditText.text.toString().trim {it <= ' '}

                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                val firebaseUser: FirebaseUser = task.result!!.user!!

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Login Successful (:",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent =
                                    Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("user_id",
                                                FirebaseAuth.getInstance().currentUser!!.uid
                                )
                                intent.putExtra("email_id",email)
                                startActivity(intent)
                                overridePendingTransition(R.anim.slide_from_left,R.anim.slide_to_right)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }


        }

    }
}