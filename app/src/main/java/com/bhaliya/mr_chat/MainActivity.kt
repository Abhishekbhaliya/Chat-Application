package com.bhaliya.mr_chat

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bhaliya.mr_chat.Adpter.UserAdpter
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.Model.User
import com.bhaliya.mr_chat.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {

    var binding : ActivityMainBinding? = null
    var database : FirebaseDatabase? = null
    var userList : ArrayList<User>? = null
    var userAdpter : UserAdpter? = null
    var dialog : ProgressDialog? = null
    var mAuth : FirebaseAuth? = null
    var user : User? = null
    var auth: FirebaseAuth? = null
    var firebaseFirestore: FirebaseFirestore? = null
    private lateinit var cld : ConnectionLiveData


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        checkNetworkConnection()

        permissionStatus = getSharedPreferences("permissionStatus", Context.MODE_PRIVATE)
//        requestPermission()

//        FirebaseService.sharedPref = getSharedPreferences("sharedPref",Context.MODE_PRIVATE)
//        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
//            FirebaseService.token = it.token
//        }


        auth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        dialog = ProgressDialog(this@MainActivity)
        dialog!!.setMessage("Uploading Image...")
        dialog!!.setCancelable(false)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding!!.shimmer.startShimmer()



        FirebaseMessaging.getInstance().subscribeToTopic("/topics/${mAuth!!.uid}")



        setSupportActionBar(binding!!.toolbar)

        val drawable =
            ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_more_vert_24)
        binding!!.toolbar.overflowIcon = drawable



        userList = ArrayList<User>()
        userAdpter = UserAdpter(this@MainActivity,userList!!)
        val layoutManager = GridLayoutManager(this@MainActivity,1)
        binding!!.mRec.layoutManager = layoutManager

        database!!.reference.child("users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })


        binding!!.mRec.adapter = userAdpter

        database!!.reference.child("users")
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList!!.clear()
                for (snapshot1 in snapshot.children)
                {
                    binding!!.shimmer.stopShimmer()
                    binding!!.shimmer.visibility = View.GONE
                    binding!!.mRec.visibility = View.VISIBLE
                    val user:User? = snapshot1.getValue(User::class.java)
                    if (!user!!.uid.equals(FirebaseAuth.getInstance().uid)) userList!!.add(user!!)
                }
                userAdpter!!.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile -> {
                val intent = Intent(this@MainActivity, ShowProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.logout ->{
                val alert = AlertDialog.Builder(this)

                alert.setTitle("Logout")
                alert.setMessage("Are you sure exit ?")
                alert.setPositiveButton("Yes") {dialog, which ->
                    mAuth?.signOut()
                    val intent = Intent(this@MainActivity, mobile_verification::class.java)
                    finish()
                    startActivity(intent)
                }
                alert.setNegativeButton("No") {dialog, which ->

                }

                alert.show()
            }

        }



        return true
    }


    override fun onResume() {
        checkNetworkConnection()
        super.onResume()
        val documentReference = firebaseFirestore!!.collection("users").document(auth!!.currentUser!!.uid)
        documentReference.update("status", "Online")
        val currentID = FirebaseAuth.getInstance().uid
        database!!.reference.child("presence")
            .child(currentID!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val documentReference = firebaseFirestore!!.collection("users").document(auth!!.currentUser!!.uid)
        documentReference.update("status", "Offline")
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("presence").child(currentId!!).setValue("Offline")
    }

    override fun onStop() {
        super.onStop()
        val documentReference = firebaseFirestore!!.collection("users").document(auth!!.currentUser!!.uid)
        documentReference.update("status", "Offline")
    }

    override fun onStart() {
        super.onStart()
        val documentReference = firebaseFirestore!!.collection("users").document(auth!!.currentUser!!.uid)
        documentReference.update("status", "Online")
    }

    private fun checkNetworkConnection() {
        cld = ConnectionLiveData(application)

        cld.observe(this, { isConnected ->

            if (isConnected){
                binding!!.InternetLayout.visibility = View.GONE
                binding!!.DataLayout.visibility = View.VISIBLE
            }else{
                Toast.makeText(this, "No Internet", Toast.LENGTH_LONG).show()
                binding!!.InternetLayout.visibility = View.VISIBLE
                binding!!.DataLayout.visibility = View.GONE
            }

        })
    }

    // Permission Code 1
    private fun requestPermission(){
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