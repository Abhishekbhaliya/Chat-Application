package com.bhaliya.mr_chat.Splesh

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.bhaliya.mr_chat.MainActivity
import com.bhaliya.mr_chat.R
import com.bhaliya.mr_chat.databinding.ActivitySpleshBinding
import com.bhaliya.mr_chat.mobile_verification
import com.google.firebase.auth.FirebaseAuth


class splesh : AppCompatActivity() {

    var binding : ActivitySpleshBinding? = null
    private val SPLASH_TIME_OUT = 1000L
    var auth : FirebaseAuth? = null

    var topAnim: Animation? = null
    var bottomAnim:Animation? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpleshBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        auth = FirebaseAuth.getInstance()


        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this,R.anim.bottum_animation);

        binding!!.logoText.setAnimation(topAnim);

        supportActionBar?.hide()


        Handler(Looper.getMainLooper()).postDelayed({
            if (auth!!.currentUser != null)
            {
                var intent = Intent(this@splesh, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else
            {
                val i = Intent(this@splesh, mobile_verification::class.java)
                startActivity(i)
                finish()
            }

        }, SPLASH_TIME_OUT)



    }
}