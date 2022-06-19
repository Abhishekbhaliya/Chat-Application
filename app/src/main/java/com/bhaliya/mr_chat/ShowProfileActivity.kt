package com.bhaliya.mr_chat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.Model.User
import com.bhaliya.mr_chat.databinding.ActivityShowProfileBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ShowProfileActivity : AppCompatActivity() {


    var firebaseAuth: FirebaseAuth? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var firebaseFirestore: FirebaseFirestore? = null
    var storageReference: StorageReference? = null
    var firebaseStorage: FirebaseStorage? = null
    private var ImageURIacessToken: String? = null
    var user : User? = null
    private lateinit var cld : ConnectionLiveData


    var  binding : ActivityShowProfileBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        checkNetworkConnection()


        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        setSupportActionBar(binding!!.toolbarofviewprofile)

        binding!!.backbuttonofviewprofile.setOnClickListener { finish() }


        storageReference = firebaseStorage!!.reference

        storageReference!!.child("Profile").child(firebaseAuth!!.uid!!)
            .downloadUrl.addOnSuccessListener { uri ->
                ImageURIacessToken = uri.toString()

                Glide.with(this@ShowProfileActivity).load(uri)
                    .placeholder(R.drawable.profile_image)
                    .into(binding!!.viewuserimageinimageview)


            }

        firebaseDatabase!!.reference.child("users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    user = snapshot.getValue(User::class.java)

//                    Glide.with(this@ShowProfileActivity).load(user!!.profileImage)
//                        .placeholder(R.drawable.profile_image)
//                        .into(binding!!.viewuserimageinimageview)

                    binding!!.viewusername.setText(user!!.name).toString()

                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, "Failed To Fetch", Toast.LENGTH_SHORT).show()
                }

            })


            binding!!.editbuttonofviewprofile.setOnClickListener {
                val intent = Intent(this@ShowProfileActivity, UpdateProfile::class.java)
                intent.putExtra("nameofuser", binding!!.viewusername.text.toString())
                finish()
                startActivity(intent)
            }


    }

    override fun onStop() {
        super.onStop()
        val documentReference = firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
        documentReference.update("status", "Offline")
    }

    override fun onStart() {
        super.onStart()
        val documentReference = firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
        documentReference.update("status", "Online")
    }

    private fun checkNetworkConnection() {
        cld = ConnectionLiveData(application)

        cld.observe(this, { isConnected ->

            if (isConnected){

            }else{
                Toast.makeText(this, "No Internet", Toast.LENGTH_LONG).show()

            }

        })
    }


}