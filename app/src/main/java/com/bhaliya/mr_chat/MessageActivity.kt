package com.bhaliya.mr_chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.MapActivity.MapsActivity
import com.bhaliya.mr_chat.Model.Message
import com.bhaliya.mr_chat.Model.NotificationData
import com.bhaliya.mr_chat.Model.PushNotification
import com.bhaliya.mr_chat.Model.User
import com.bhaliya.mr_chat.databinding.ActivityMessageBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.malkinfo.answerandquestion.adapter.MessagesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MessageActivity : AppCompatActivity() {

    var binding: ActivityMessageBinding? = null
    var adapter: MessagesAdapter? = null
    var messages: ArrayList<Message>? = null
    var senderRoom: String? = null
    var receiverRoom: String? = null

    var database: FirebaseDatabase? = null
    var user : User? = null

    var storage: FirebaseStorage? = null

    var senderUid: String? = null
    var receiverUid: String? = null

    var firebaseFirestore: FirebaseFirestore? = null

    var firebaseAuth: FirebaseAuth? = null

    var dialog: ProgressDialog? = null
    val Gallery_Image_REQ_Code = 1
    val PDF_REQ_Code = 3
    val CAMERA_REQUEST_CODE = 102
    var currentPhotoPath: String? = null
    var topic = ""
    var name : String? = null
    private lateinit var cld : ConnectionLiveData



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        checkNetworkConnection()

        dialog = ProgressDialog(this)
        dialog!!.setMessage("Sending...")
        dialog!!.setCancelable(false)

        setSupportActionBar(binding!!.toolbar)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()


        messages = ArrayList<Message>()

        firebaseFirestore = FirebaseFirestore.getInstance()

        name = intent.getStringExtra("name")
        val profile = intent.getStringExtra("image")

        binding!!.name.text = name

        Glide.with(this@MessageActivity).load(profile)
            .placeholder(com.bhaliya.mr_chat.R.drawable.profile_image)
            .into(binding!!.profile01)

        binding!!.backbuttonofmessageActivity.setOnClickListener(View.OnClickListener { finish() })

        receiverUid = intent.getStringExtra("uid")
        senderUid = FirebaseAuth.getInstance().uid

       database!!.reference.child("presence").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("ResourceAsColor")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(String::class.java)
                        if (!status!!.isEmpty()) {
                            if (status == "Offline") {
                                binding!!.status.visibility = View.GONE
                            } else {
                                binding!!.status.text = status
                                binding!!.status.setTextColor(com.bhaliya.mr_chat.R.color.red)
                                binding!!.status.visibility = View.VISIBLE

                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid
        adapter = MessagesAdapter(this, messages, senderRoom!!, receiverRoom!!)
        binding!!.recyclerView.layoutManager = LinearLayoutManager(this)
        binding!!.recyclerView.adapter = adapter


        database!!.reference.child("chats")
            .child(senderRoom!!)
            .child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages!!.clear()
                    for (snapshot1 in snapshot.children)
                    {
                        val message: Message? = snapshot1.getValue(Message::class.java)
                        message!!.messageId = snapshot1.key
                        messages!!.add(message)
                    }

                    // Auto Scroll code
                    binding!!.recyclerView.smoothScrollToPosition(binding!!.recyclerView.getAdapter()?.getItemCount()!!)
                    adapter!!.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        binding!!.sendBtn.setOnClickListener(View.OnClickListener {
            val messageTxt: String = binding!!.messageBox.text.toString()
            val date = Date()
            val formatter = SimpleDateFormat("hh:mm a")
            val dateAndTime: String = formatter.format(date)
            val randomKey = database!!.reference.push().key

            val message = Message(randomKey, messageTxt, senderUid,receiverUid, "","", dateAndTime,"","")
            binding!!.messageBox.setText("")


            // Push Notification Code
          if (message.message != "" || message.pdfUrl != "" || message.imageUrl != "")
          {
              database!!.reference.child("users")
                  .child(FirebaseAuth.getInstance().uid!!)
                  .addValueEventListener(object : ValueEventListener{
                      override fun onDataChange(snapshot: DataSnapshot) {
                          user = snapshot.getValue(User::class.java)
                          val senderName : String? = user!!.name.toString()


                          database!!.reference.child("presence").child(receiverUid!!)
                              .addValueEventListener(object : ValueEventListener {
                                  @SuppressLint("ResourceAsColor")
                                  override fun onDataChange(snapshot: DataSnapshot) {
                                      if (snapshot.exists()) {
                                          val status = snapshot.getValue(String::class.java)
                                          if (!status!!.isEmpty()) {
                                              if (status == "Offline") {

                                                  // push notification code
                                                  topic = "/topics/${receiverUid}"
                                                  PushNotification(
                                                      NotificationData(senderName!!,messageTxt),
                                                      topic).also {
                                                      sendNotification(it)

                                                  }
                                              }
                                          }
                                      }
                                  }
                                  override fun onCancelled(error: DatabaseError) {}
                              })
                      }
                      override fun onCancelled(error: DatabaseError) {
                          Toast.makeText(applicationContext, "Failed To Fetch", Toast.LENGTH_SHORT).show()
                      }
                  })
          }


            val lastMsgObj = HashMap<String, Any>()
            lastMsgObj["lastMsg"] = message.message!!
            lastMsgObj["lastMsgTime"] = date.time
                database!!.reference.child("chats").child(senderRoom!!).updateChildren(lastMsgObj)
                database!!.reference.child("chats").child(receiverRoom!!).updateChildren(lastMsgObj)

            if (message.message != "" || message.pdfUrl != "" || message.imageUrl != "")
            {
                //add cloud firestore
                firebaseFirestore!!.collection("chats")
                    .document(senderRoom!!)
                    .collection("messages")
                    .document(randomKey!!)
                    .set(message)
                    .addOnSuccessListener {
                        firebaseFirestore!!
                            .collection("chats")
                            .document(receiverRoom!!)
                            .collection("messages")
                            .document(randomKey)
                            .set(message)
                    }
                // add realtime database
                database!!.reference.child("chats")
                    .child(senderRoom!!)
                    .child("messages")
                    .child(randomKey)
                    .setValue(message).addOnSuccessListener {
                        database!!.reference.child("chats")
                            .child(receiverRoom!!)
                            .child("messages")
                            .child(randomKey)
                            .setValue(message).addOnSuccessListener { }
                    }
            }
        })

        binding!!.CameraAttachment.setOnClickListener {
            Dexter.withContext(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        dispatchTakePictureIntent()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        showSettingsDialog()
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                        token!!.continuePermissionRequest()
                    }
                }).check()
        }

        binding!!.attachment.setOnClickListener(View.OnClickListener {

            val view : View = layoutInflater.inflate(com.bhaliya.mr_chat.R.layout.attachment_bottomsheet,null)
            val dialog = BottomSheetDialog(this)
            dialog.setContentView(view)
            dialog.show()

           val  Image = view.findViewById<LinearLayout>(com.bhaliya.mr_chat.R.id.image_attachment)
           val  Documents = view.findViewById<LinearLayout>(com.bhaliya.mr_chat.R.id.doc_attchment)
           val  Location = view.findViewById<LinearLayout>(com.bhaliya.mr_chat.R.id.location_attachment)

            Image.setOnClickListener {
                dialog.dismiss()
                Dexter.withContext(this)
                    .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse) {
                            val intent = Intent()
                            intent.action = Intent.ACTION_GET_CONTENT
                            intent.type = "image/*"
                            dialog.dismiss()
                            startActivityForResult(intent, Gallery_Image_REQ_Code)                       }

                        override fun onPermissionDenied(response: PermissionDeniedResponse) {
                            showSettingsDialog()
                        }
                        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                            token!!.continuePermissionRequest()
                        }
                    }).check()
            }

            Documents.setOnClickListener {
                dialog.dismiss()
                Dexter.withContext(this)
                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse) {
                            val intent = Intent()
                            intent.action = Intent.ACTION_GET_CONTENT
                            intent.type="application/*"
                            dialog.dismiss()
                            startActivityForResult(intent,PDF_REQ_Code)
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse) {
                            showSettingsDialog()
                        }

                        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                            token!!.continuePermissionRequest()
                        }
                    }).check()
            }

            Location.setOnClickListener {
                val intent = Intent(this@MessageActivity,MapsActivity::class.java)
                startActivity(intent)
                finish()
            }

        })

        val handler = Handler(Looper.getMainLooper())

        binding!!.messageBox.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                database!!.reference.child("presence").child(senderUid!!).setValue("typing...")
                binding!!.sendBtn.visibility = View.VISIBLE

                val documentReference = firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
                documentReference.update("status", "typing...")
                // Auto Scroll code
                binding!!.recyclerView.smoothScrollToPosition(binding!!.recyclerView.getAdapter()?.getItemCount()!!)
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 1000)
            }

            var userStoppedTyping =
                Runnable {
                    val documentReference = firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
                    documentReference.update("status", "Online")
                    database!!.reference.child("presence").child(senderUid!!).setValue("Online")
                }
        })
        supportActionBar!!.setDisplayShowTitleEnabled(false)

    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this@MessageActivity)

        // below line is the title
        // for our alert dialog.
        builder.setTitle("Need Permissions")
        // below line is our message for our dialog
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, which -> // this method is called on click on positive
            // button and on clicking shit button we
            // are redirecting our user from our app to the
            // settings page of our app.
            dialog.cancel()
            // below is the intent from which we
            // are redirecting our user.
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, 101)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> // this method is called when
            // user click on negative button.
            dialog.cancel()
        }
        // below line is used
        // to display our dialog
        builder.show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == Gallery_Image_REQ_Code) {
                if(resultCode == RESULT_OK) {
                    if (data != null) {
                        if (data.data != null) {
                            val selectedImage = data.data

                            val documentFile =
                                DocumentFile.fromSingleUri(this@MessageActivity, selectedImage!!)
                            val fileName = documentFile!!.name
                            val extension = fileName!!.substring(fileName!!.lastIndexOf("."))

                            try {
                                var original : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedImage)
                                val strim = ByteArrayOutputStream()
                                original.compress(Bitmap.CompressFormat.JPEG,50,strim)
                                var imageByte = strim.toByteArray()
                                ImageUpload(imageByte,extension,fileName)
                            }
                            catch (e : IOException)
                            {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val f = File(currentPhotoPath)
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f))
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(f)
                mediaScanIntent.data = contentUri
                this.sendBroadcast(mediaScanIntent)

                try {
                    var original : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver,contentUri)
                    val strim = ByteArrayOutputStream()
                    original.compress(Bitmap.CompressFormat.JPEG,50,strim)
                    var imageByte = strim.toByteArray()

                    Camera_ImageUpload(imageByte)
                }
                catch (e : IOException)
                {
                    e.printStackTrace()
                }
            }
        }
            if (requestCode == PDF_REQ_Code) {
                if(resultCode == RESULT_OK) {
                    if (data != null) {

                        if (data.data != null) {
                            val selectedPdf = data.data
                            val documentFile =
                                DocumentFile.fromSingleUri(this@MessageActivity, selectedPdf!!)

                            val fileName = documentFile!!.name
                            val extension = fileName!!.substring(fileName!!.lastIndexOf("."))

                            val reference = storage!!.reference.child("chats")
                                .child(fileName)

                            dialog!!.show()
                            reference.putFile(selectedPdf!!).addOnCompleteListener { task ->
                                dialog!!.dismiss()
                                if (task.isSuccessful) {
                                    reference.downloadUrl.addOnSuccessListener { uri ->
                                        val PDFfilePath = uri.toString()
                                        val messageTxt: String =
                                            binding!!.messageBox.text.toString()
                                        val date = Date()
                                        val formatter = SimpleDateFormat("hh:mm a")
                                        val dateAndTime: String = formatter.format(date)

                                        val randomKey = database!!.reference.push().key

                                        val message = Message(randomKey, messageTxt, senderUid, receiverUid, " ", PDFfilePath, dateAndTime,extension,fileName)

                                        binding!!.messageBox.setText("")

                                        val lastMsgObj = HashMap<String, Any>()

                                        lastMsgObj["lastMsg"] = message.message!!
                                        lastMsgObj["lastMsgTime"] = date.time

                                        database!!.reference.child("chats").child(senderRoom!!)
                                            .updateChildren(lastMsgObj)
                                        database!!.reference.child("chats").child(receiverRoom!!)
                                            .updateChildren(lastMsgObj)

                                        database!!.reference.child("chats")
                                            .child(senderRoom!!)
                                            .child("messages")
                                            .child(randomKey!!)
                                            .setValue(message).addOnSuccessListener {
                                                database!!.reference.child("chats")
                                                    .child(receiverRoom!!)
                                                    .child("messages")
                                                    .child(randomKey)
                                                    .setValue(message).addOnSuccessListener { }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }

    }


    private fun Camera_ImageUpload(imageByte: ByteArray) {

        val date = Date()
        val formatter = SimpleDateFormat("hh:mm a")
        val dateAndTime: String = formatter.format(date)

        val reference = storage!!.reference.child("chats")
            .child(dateAndTime)

        dialog!!.show()

        reference.putBytes(imageByte).addOnCompleteListener { task ->
            dialog!!.dismiss()
            if (task.isSuccessful){
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val ImagefilePath = uri.toString()
                    val messageTxt: String =
                        binding!!.messageBox.text.toString()
                    val date = Date()
                    val formatter = SimpleDateFormat("hh:mm a")
                    val dateAndTime: String = formatter.format(date)


                    val randomKey = database!!.reference.push().key


                    val message = Message(randomKey, messageTxt, senderUid, receiverUid, ImagefilePath, "", dateAndTime, "", "")
                    binding!!.messageBox.setText("")
                    val lastMsgObj = HashMap<String, Any>()

                    lastMsgObj["lastMsg"] = message.message!!
                    lastMsgObj["lastMsgTime"] = date.time


                    // add cloud firestore
                    firebaseFirestore!!.collection("chats")
                        .document(senderRoom!!)
                        .set(lastMsgObj)
                    firebaseFirestore!!.collection("chats")
                        .document(receiverRoom!!)
                        .set(lastMsgObj)

                    firebaseFirestore!!.collection("chats")
                        .document(senderRoom!!)
                        .collection("messages")
                        .document(randomKey!!)
                        .set(message)
                        .addOnSuccessListener {
                            firebaseFirestore!!
                                .collection("chats")
                                .document(receiverRoom!!)
                                .collection("messages")
                                .document(randomKey!!)
                                .set(message)
                        }
                    // add realtime database
                    database!!.reference.child("chats").child(senderRoom!!)
                        .updateChildren(lastMsgObj)
                    database!!.reference.child("chats").child(receiverRoom!!)
                        .updateChildren(lastMsgObj)

                    database!!.reference.child("chats")
                        .child(senderRoom!!)
                        .child("messages")
                        .child(randomKey!!)
                        .setValue(message).addOnSuccessListener {
                            database!!.reference.child("chats")
                                .child(receiverRoom!!)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener { }
                        }
                }
            }
        }
    }

    private fun ImageUpload(imageByte: ByteArray, extension: String, fileName: String) {

        val reference = storage!!.reference.child("chats")
            .child(fileName)

        dialog!!.show()

        reference.putBytes(imageByte!!).addOnCompleteListener { task ->
            dialog!!.dismiss()
            if (task.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val ImagefilePath = uri.toString()
                    val messageTxt: String =
                        binding!!.messageBox.text.toString()
                    val date = Date()
                    val formatter = SimpleDateFormat("hh:mm a")
                    val dateAndTime: String = formatter.format(date)

                    val randomKey = database!!.reference.push().key


                    val message = Message(randomKey, messageTxt, senderUid, receiverUid, ImagefilePath, "", dateAndTime,extension,fileName)

                    binding!!.messageBox.setText("")

                    val lastMsgObj = HashMap<String, Any>()

                    lastMsgObj["lastMsg"] = message.message!!
                    lastMsgObj["lastMsgTime"] = date.time


                    // add cloud firestore

                    firebaseFirestore!!.collection("chats")
                        .document(senderRoom!!)
                        .set(lastMsgObj)
                    firebaseFirestore!!.collection("chats")
                        .document(receiverRoom!!)
                        .set(lastMsgObj)

                    firebaseFirestore!!.collection("chats")
                        .document(senderRoom!!)
                        .collection("messages")
                        .document(randomKey!!)
                        .set(message)
                        .addOnSuccessListener {
                            firebaseFirestore!!
                                .collection("chats")
                                .document(receiverRoom!!)
                                .collection("messages")
                                .document(randomKey)
                                .set(message)
                        }

                    // add realtime database
                    database!!.reference.child("chats").child(senderRoom!!)
                        .updateChildren(lastMsgObj)
                    database!!.reference.child("chats").child(receiverRoom!!)
                        .updateChildren(lastMsgObj)

                    database!!.reference.child("chats")
                        .child(senderRoom!!)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener {
                            database!!.reference.child("chats")
                                .child(receiverRoom!!)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener { }
                        }
                }
            }
        }

    }


    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        //        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "net.smallacademy.android.fileprovider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
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

    override fun onResume() {
        super.onResume()
        val documentReference = firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
        documentReference.update("status", "Online")
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val documentReference = firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
        documentReference.update("status", "Offline")
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("presence").child(currentId!!).setValue("Offline")
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }


    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d("TAG", "Response: ${Gson().toJson(response)}")
            } else {
                Log.e("TAG", response.errorBody()!!.string())
            }
        } catch(e: Exception) {
            Log.e("TAG", e.toString())
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


}