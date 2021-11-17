package com.specknet.pdiotapp.login

import android.content.Intent
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.specknet.pdiotapp.R
import kotlinx.android.synthetic.main.activity_register.*


class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        toLoginButton.setOnClickListener {
            onBackPressed()
        }

        registerButton.setOnClickListener {
            when {
                TextUtils.isEmpty(EmailEditText.text.toString().trim {it <= ' '}) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Email field empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(PasswordEditText.text.toString().trim {it <= ' '}) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Password field empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    val email: String = EmailEditText.text.toString().trim {it <= ' '}
                    val password: String = PasswordEditText.text.toString().trim {it <= ' '}

                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                val firebaseUser: FirebaseUser = task.result!!.user!!

                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Registration Successful (:",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent =
                                    Intent(this@RegisterActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("user_id", firebaseUser.uid)
                                intent.putExtra("email_id",email)
                                startActivity(intent)
                                overridePendingTransition(R.anim.slide_from_left,R.anim.slide_to_right)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }


        }


    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_from_left,R.anim.slide_to_right)
    }

}