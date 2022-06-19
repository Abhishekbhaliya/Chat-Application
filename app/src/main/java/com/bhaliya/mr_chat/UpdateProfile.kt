package com.bhaliya.mr_chat

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.Model.User
import com.bhaliya.mr_chat.databinding.ActivityUpdateProfileBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class UpdateProfile : AppCompatActivity() {

    var binding: ActivityUpdateProfileBinding? = null

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseDatabase: FirebaseDatabase? = null
    private var firebaseFirestore: FirebaseFirestore? = null
    private var storageReference: StorageReference? = null
    private var firebaseStorage: FirebaseStorage? = null
    var imagepath: Uri? = null
    var dialog: ProgressDialog? = null
    var ImageURIacessToken: String? = null
    var Status :String? = null
    private lateinit var cld : ConnectionLiveData

    val Gallery_Image_REQ_Code = 1
    val CAMERA_REQUEST_CODE = 102
    var currentPhotoPath: String? = null



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        checkNetworkConnection()
        dialog = ProgressDialog(this)
        dialog!!.setMessage("Updating Profile...")
        dialog!!.setCancelable(false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()




        setSupportActionBar(binding!!.toolbarofupdateprofile)

        storageReference = firebaseStorage!!.reference

        storageReference!!.child("Profile").child(firebaseAuth!!.uid!!)
            .downloadUrl.addOnSuccessListener { uri ->
                ImageURIacessToken = uri.toString()

                Glide.with(this).load(uri)
                    .placeholder(R.drawable.profile_image)
                    .into(binding!!.getnewuserimageinimageview)

            }


        val documentReference: DocumentReference =
            firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)

        documentReference.get().addOnSuccessListener { uri ->
            if (uri.exists())
            {
                Status = uri.getString("status")

            }
        }



        binding!!.backbuttonofupdateprofile.setOnClickListener { finish() }

        val name = intent.getStringExtra("nameofuser")
        binding!!.getnewusername.setText(name)

        val databaseReference = firebaseDatabase!!.getReference("users").child(firebaseAuth!!.uid!!)


        binding!!.updateprofilebutton.setOnClickListener {

            val name: String = binding!!.getnewusername.text.toString()

            if (name.isEmpty()) {
                binding!!.getnewusername.error = "Please type a name"
                binding!!.getnewusername.requestFocus()
                return@setOnClickListener
            }



            dialog!!.show()

            if (imagepath != null) {

                dialog!!.show()

                val reference = firebaseStorage!!.reference.child("Profile")
                    .child(firebaseAuth!!.uid!!)

                reference.putFile(imagepath!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            val uid = firebaseAuth!!.uid
                            val phone = firebaseAuth!!.currentUser!!.phoneNumber
                            val user = User(uid, name, phone, imageUrl,Status)
                            databaseReference.setValue(user)

                            updateimagetostorage()

                            Toast.makeText(applicationContext, "Updated", Toast.LENGTH_SHORT).show()
                            dialog!!.dismiss()
                            val intent = Intent(this@UpdateProfile, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    else {
                        val uid = firebaseAuth!!.uid
                        val phone = firebaseAuth!!.currentUser!!.phoneNumber
                        val user = User(uid, name, phone, "No Image",Status)
                        databaseReference.setValue(user)
                        updatenameoncloudfirestore()
                        Toast.makeText(applicationContext, "Updated", Toast.LENGTH_SHORT).show()
                        dialog!!.dismiss()
                        val intent = Intent(this@UpdateProfile, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }


           else if (name != null)
            {
                storageReference = firebaseStorage!!.reference
                storageReference!!.child("Profile").child(firebaseAuth!!.uid!!)
                    .downloadUrl.addOnSuccessListener { uri ->
                        ImageURIacessToken = uri.toString()
                    }

                val imageUrl = ImageURIacessToken.toString()
                val uid = firebaseAuth!!.uid
                val phone = firebaseAuth!!.currentUser!!.phoneNumber
                val user = User(uid, name, phone, imageUrl,Status)
                databaseReference.setValue(user)
                Toast.makeText(applicationContext, "Text Updated", Toast.LENGTH_SHORT).show()
                dialog!!.dismiss()
                val intent = Intent(this@UpdateProfile, ShowProfileActivity::class.java)
                startActivity(intent)
                finish()
            }
            else
            {
                dialog!!.dismiss()
                Toast.makeText(this, "you are already updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding!!.SetProfileFeb.setOnClickListener {

            val view : View = layoutInflater.inflate(com.bhaliya.mr_chat.R.layout.internal_data_diloge,null)
            val dialog = Dialog(this)
            dialog.setContentView(view)
            dialog.show()

            val  Image = view.findViewById<LinearLayout>(com.bhaliya.mr_chat.R.id.images_attachment)
            val  camers = view.findViewById<LinearLayout>(com.bhaliya.mr_chat.R.id.cameras_attchment)

            Image.setOnClickListener {
                dialog.dismiss()
                Dexter.withContext(this)
                    .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse) {
                            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            startActivityForResult(intent,Gallery_Image_REQ_Code)
                            dialog.dismiss()
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse) {
                            showSettingsDialog()
                        }
                        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                            token!!.continuePermissionRequest()
                        }
                    }).check()

            }

            camers.setOnClickListener {
                dialog.dismiss()
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

        }
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this@UpdateProfile)

        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, which ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent,CAMERA_REQUEST_CODE)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which ->
            dialog.cancel()
        }

        builder.show()
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

   private fun updatenameoncloudfirestore() {
        val documentReference: DocumentReference =
            firebaseFirestore!!.collection("users").document(firebaseAuth!!.currentUser!!.uid)
        val userdata: MutableMap<String, Any> = HashMap()
        userdata["name"] = binding!!.getnewusername.text.toString()
        userdata["image"] = imagepath.toString()
        userdata["uid"] = firebaseAuth!!.uid!!
        userdata["status"] = "Online"
        userdata["Phone"] = firebaseAuth!!.currentUser!!.phoneNumber.toString()
        documentReference.set(userdata)


    }

    private fun updateimagetostorage() {
        val imageref =
            storageReference!!.child("Profile").child(firebaseAuth!!.uid!!)

        //Image compresesion
        var bitmap: Bitmap? = null
        try {
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imagepath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 25, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        ///putting image to storage
        val uploadTask = imageref.putBytes(data)
        uploadTask.addOnSuccessListener {
            imageref.downloadUrl.addOnSuccessListener { uri ->
                ImageURIacessToken = uri.toString()
                updatenameoncloudfirestore()
            }.addOnFailureListener {
                Toast.makeText(
                    applicationContext,
                    "URI get Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(
                applicationContext,
                "Image Not Updated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val f = File(currentPhotoPath)
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f))
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(f)
                mediaScanIntent.data = contentUri
                this.sendBroadcast(mediaScanIntent)

                binding!!.getnewuserimageinimageview.setImageURI(contentUri)

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

        if (requestCode == Gallery_Image_REQ_Code) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    if (data.data != null) {
                        binding!!.getnewuserimageinimageview.setImageURI(data.data)
                        imagepath = data.data
                        binding!!.getnewuserimageinimageview.setImageURI(imagepath)

                    }
                }
            }
        }

    }

    private fun Camera_ImageUpload(imageByte: ByteArray) {

        val storage = FirebaseStorage.getInstance()
        val reference = storage.reference
            .child("Profile")
            .child(firebaseAuth!!.uid!!)

        reference.putBytes(imageByte).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val filePath = uri.toString()
                    val obj = java.util.HashMap<String, Any>()
                    obj["image"] = filePath
                    firebaseDatabase!!.reference
                        .child("users")
                        .child(FirebaseAuth.getInstance().uid!!)
                        .updateChildren(obj).addOnSuccessListener { }
                }
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