package com.bhaliya.mr_chat

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.Model.User
import com.bhaliya.mr_chat.databinding.ActivityOtpVerificationBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit

class otp_verification : AppCompatActivity() {

    var binding : ActivityOtpVerificationBinding? = null
    var verificationID : String? = null
    var auth : FirebaseAuth? = null
    var database: FirebaseDatabase? = null
    var dialog : ProgressDialog? = null
    var storage: FirebaseStorage? = null

    private lateinit var cld : ConnectionLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        checkNetworkConnection()
        dialog = ProgressDialog(this@otp_verification)
        dialog!!.setMessage("Again sending OTP...")
        dialog!!.setCancelable(false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        supportActionBar?.hide()
        storage = FirebaseStorage.getInstance()

        val phoneNumber = intent.getStringExtra("mobile")
        binding!!.verifynumbertext.text = "$phoneNumber"

        binding!!.sendotpAgain.setOnClickListener {
            if(phoneNumber != null)
            {
                dialog!!.show()
                val options = PhoneAuthOptions.newBuilder(auth!!)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this@otp_verification)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

                        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                            TODO("Not yet implemented")
                            dialog!!.dismiss()

                        }

                        override fun onVerificationFailed(p0: FirebaseException) {
                            dialog!!.dismiss()
                            Toast.makeText(this@otp_verification, "Please Check Internet Conection", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                            super.onCodeSent(p0, p1)
                            dialog!!.dismiss()
                            verificationID = p0
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)

                            Toast.makeText(
                                this@otp_verification,
                                "OTP Send Sucessfuly",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }).build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }

        }


        val oTpNumber = intent.getStringExtra("OTPid")
        val Username = intent.getStringExtra("name")

        verificationID = "$oTpNumber"


        binding!!.otpView.setOtpCompletionListener { otp ->
            val credential = PhoneAuthProvider.getCredential(verificationID!!, otp)

//            val reference = storage!!.reference.child("Profile")
//                .child(auth!!.uid!!)
//                reference.delete()

            auth!!.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (Username != null) {
                            val uid = auth!!.uid
                            val phone = auth!!.currentUser!!.phoneNumber
                            val user = User(uid, Username, phone, "", "")

                            // add realTime database
                            database!!.reference
                                .child("users")
                                .child(uid!!)
                                .setValue(user)
                                .addOnSuccessListener {
                                    val intent = Intent(this@otp_verification, setProfile::class.java)
                                    intent.putExtra("name", Username.toString())
                                    startActivity(intent)
                                    finish()
                                }
                        }
                        else {
                        val uid = auth!!.uid
                        val phone = auth!!.currentUser!!.phoneNumber
                        val user = User(uid, Username, phone, " ", "")

                        // add realTime database
                        database!!.reference
                            .child("users")
                            .child(uid!!)
                            .setValue(user)
                            .addOnSuccessListener {
                                val intent = Intent(this@otp_verification, setProfile::class.java)
                                intent.putExtra("name", Username.toString())
                                startActivity(intent)
                                finish()
                            }
                    }
                }
                    else
                    {
                        Toast.makeText(this, "Wrong otp", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun checkNetworkConnection() {
        cld = ConnectionLiveData(application)

        cld.observe(this) { isConnected ->

            if (isConnected) {

            } else {
                Toast.makeText(this, "No Internet", Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        dialog!!.dismiss()
    }

}