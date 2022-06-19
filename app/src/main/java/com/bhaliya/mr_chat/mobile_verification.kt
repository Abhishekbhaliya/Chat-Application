package com.bhaliya.mr_chat

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.databinding.ActivityMobileVerificationBinding
import com.google.android.gms.auth.api.credentials.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATION")
class mobile_verification : AppCompatActivity() {

    var binding : ActivityMobileVerificationBinding? = null
    var auth : FirebaseAuth? = null
    var verificationID : String? = null

    companion object {
        var CREDENTIAL_PICKER_REQUEST = 1
    }

    private lateinit var cld : ConnectionLiveData
    var dialog : ProgressDialog? = null

    private val PERMISSION_CALLBACK_CONSTANT = 100
    private val REQUEST_PERMISSION_SETTING = 101
    private var permissionStatus: SharedPreferences? = null
    private var sentToSettings = false

    private var permissionsRequired =
        arrayOf(Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

    var num : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMobileVerificationBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        auth = FirebaseAuth.getInstance()

        checkNetworkConnection()
        dialog = ProgressDialog(this@mobile_verification)
        dialog!!.setMessage("Sending OTP...")
        dialog!!.setCancelable(false)

        if (auth!!.currentUser != null)
        {
            var intent = Intent(this@mobile_verification,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        supportActionBar?.hide()
        binding!!.ccp.requestFocus()
        binding!!.t1.requestFocus()


        permissionStatus = getSharedPreferences("permissionStatus", Context.MODE_PRIVATE)


        binding!!.continuBtn.setOnClickListener {
            var numM  = binding!!.t1.text.toString()
            var name  = binding!!.UserName.text.toString()

            binding!!.ccp.registerCarrierNumberEditText(binding!!.t1)

            num = binding!!.ccp!!.fullNumberWithPlus.replace(" ", "")


            if (numM.isEmpty())
            {
                dialog!!.dismiss()
                binding!!.t1.setError("Enter mobile number")
                binding!!.t1.requestFocus()
            }
            else
                if (name.isEmpty())
                {
                    dialog!!.dismiss()
                    binding!!.UserName.setError("Enter Your Name")
                    binding!!.UserName.requestFocus()
                }
            else {
                dialog!!.show()
                val options = PhoneAuthOptions.newBuilder(auth!!)
                    .setPhoneNumber(num!!)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this@mobile_verification)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                            TODO("Not yet implemented")
                        }

                        override fun onVerificationFailed(p0: FirebaseException) {
                            dialog!!.dismiss()
                            Toast.makeText(this@mobile_verification, "Please Check Internet Conection", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                            super.onCodeSent(p0, p1)
                            dialog!!.dismiss()
                            verificationID = p0
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as  InputMethodManager
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
                            val intent = Intent(this@mobile_verification, otp_verification::class.java)
                            finish()
                            intent.putExtra("mobile", binding!!.ccp!!.fullNumberWithPlus.replace(" ", ""))
                            intent.putExtra("name",name)
                            intent.putExtra("OTPid",verificationID)
                            startActivity(intent)
                        }

                    }).build()


                PhoneAuthProvider.verifyPhoneNumber(options)


            }
            }




    }

    private fun phoneSelection() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()
        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        val credentialsClient = Credentials.getClient(applicationContext, options)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        try {
            startIntentSenderForResult(
                intent.intentSender,
                CREDENTIAL_PICKER_REQUEST, null, 0, 0, 0, Bundle()
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == RESULT_OK) {

            // get data from the dialog which is of type Credential
            val credential: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)

            // set the received data t the text view
            var Mobnumber :String = credential!!.id

            binding!!.t1.setText(Mobnumber)

        } else if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == CredentialsApi.ACTIVITY_RESULT_NO_HINTS_AVAILABLE) {
            Toast.makeText(this, "No phone numbers found", Toast.LENGTH_LONG).show();
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

    // Permission Code 1
    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, permissionsRequired[0]) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, permissionsRequired[1]) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, permissionsRequired[2]) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])) {
                //Show Information about why you need the permission
                getAlertDialog()
            } else if (permissionStatus!!.getBoolean(permissionsRequired[0], false)) {
                //Previously Permission Request was cancelled with 'Dont Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Need Multiple Permissions")
                builder.setMessage("This app needs permissions.")
                builder.setPositiveButton("Grant") { dialog, which ->
                    dialog.cancel()
                    sentToSettings = true
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, REQUEST_PERMISSION_SETTING)
                    Toast.makeText(applicationContext, "Go to Permissions to Grant ", Toast.LENGTH_LONG).show()
                }
                builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                builder.show()
            } else {
                //just request the permission
                ActivityCompat.requestPermissions(this, permissionsRequired, PERMISSION_CALLBACK_CONSTANT)
            }

            //   txtPermissions.setText("Permissions Required")

            val editor = permissionStatus!!.edit()
            editor.putBoolean(permissionsRequired[0], true)
            editor.commit()
        } else {
            //You already have the permission, just go ahead.
//            Toast.makeText(applicationContext, "Allowed All Permissions", Toast.LENGTH_LONG).show()
        }
    }
    // Permission Code 2
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CALLBACK_CONSTANT) {
            //check if all permissions are granted
            var allgranted = false
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    allgranted = true
                } else {
                    allgranted = false
                    break
                }
            }

            if (allgranted) {
//                Toast.makeText(applicationContext, "Allowed All Permissions", Toast.LENGTH_LONG).show()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[1])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, permissionsRequired[2])) {

                getAlertDialog()
            } else {
                Toast.makeText(applicationContext, "Unable to get Permission", Toast.LENGTH_LONG).show()
            }
        }
    }
    // Permission Code 3
    private fun getAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need Multiple Permissions")
        builder.setMessage("This app needs permissions.")
        builder.setPositiveButton("Grant") { dialog, which ->
            dialog.cancel()
            ActivityCompat.requestPermissions(this, permissionsRequired, PERMISSION_CALLBACK_CONSTANT)
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

}