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
import com.bhaliya.mr_chat.databinding.ActivitySetProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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


class setProfile : AppCompatActivity() {

    var binding: ActivitySetProfileBinding? = null
    var auth: FirebaseAuth? = null
    var firebaseFirestore: FirebaseFirestore? = null

    var database: FirebaseDatabase? = null
    var storage: FirebaseStorage? = null
    var selectedImage: Uri? = null
    var dialog: ProgressDialog? = null
    var Status :String? = null
    private lateinit var cld : ConnectionLiveData

    val CAMERA_REQUEST_CODE = 102
    var currentPhotoPath: String? = null
    val Gallery_Image_REQ_Code = 1




    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        dialog = ProgressDialog(this)
        dialog!!.setMessage("Updating Profile...")
        dialog!!.setCancelable(false)
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()


        checkNetworkConnection()

        var nameUser =   intent.getStringExtra("name")
        binding!!.profileBox.setText(nameUser)



        supportActionBar?.hide()

        binding!!.ProfileFeb.setOnClickListener(View.OnClickListener {

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
                            dialog.dismiss()                        }

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

        })

        binding!!.setProfileContinuBtn.setOnClickListener(View.OnClickListener {

            val name: String = binding!!.profileBox.text.toString()

            if (name.isEmpty()) {
                binding!!.profileBox.error = "Please enter name"
                binding!!.profileBox.requestFocus()
                return@OnClickListener
            }
            dialog!!.show()


            if (selectedImage != null) {
                val reference = storage!!.reference.child("Profile")
                    .child(auth!!.uid!!)
                reference.putFile(selectedImage!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->

                            val imageUrl = uri.toString()
                            val uid = auth!!.uid
                            val phone = auth!!.currentUser!!.phoneNumber
                            val user = User(uid, name, phone, imageUrl,Status)

                            // add realTime database
                            firebaseFirestore!!.collection("users")
                                .document(uid!!)
                                .set(user)

                            // add realTime database
                            database!!.reference
                                .child("users")
                                .child(uid!!)
                                .setValue(user)
                                .addOnSuccessListener {
                                    dialog!!.dismiss()
                                    val intent = Intent(this@setProfile, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        }
                    }
                    else {
                        val uid = auth!!.uid
                        val phone = auth!!.currentUser!!.phoneNumber
                        val user = User(uid, name, phone, "No Image",Status)

                        firebaseFirestore!!.collection("users")
                            .document(uid!!)
                            .set(user).addOnSuccessListener {
                                dialog!!.dismiss()
                                val intent = Intent(this@setProfile, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }

                        // add realTime database
                        database!!.reference
                            .child("users")
                            .child(uid)
                            .setValue(user)
                            .addOnSuccessListener {
                                dialog!!.dismiss()
                                val intent = Intent(this@setProfile, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                    }

                }
            }
            else
                if (name != null) {

                    val uid = auth!!.uid
                    val phone = auth!!.currentUser!!.phoneNumber
                    val user = User(uid, name, phone, "",Status)

                    // add realTime database
                    firebaseFirestore!!.collection("users")
                        .document(uid!!).set(user)

                    // add realTime database
                    database!!.reference
                        .child("users")
                        .child(uid!!)
                        .setValue(user)
                        .addOnSuccessListener {
                            dialog!!.dismiss()
                            val intent = Intent(this@setProfile, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                }

        })

    }


    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this@setProfile)

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



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val f = File(currentPhotoPath)
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f))
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(f)
                mediaScanIntent.data = contentUri
                this.sendBroadcast(mediaScanIntent)

                binding!!.imageView.setImageURI(contentUri)

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
                        val uri = data.data
                        val storage = FirebaseStorage.getInstance()
                        val reference = storage.reference
                            .child("Profile")
                            .child(auth!!.uid!!)
                        reference.putFile(uri!!).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                reference.downloadUrl.addOnSuccessListener { uri ->
                                    val filePath = uri.toString()
                                    val obj = HashMap<String, Any>()
                                    obj["image"] = filePath
                                    database!!.reference
                                        .child("users")
                                        .child(FirebaseAuth.getInstance().uid!!)
                                        .updateChildren(obj).addOnSuccessListener { }
                                }
                            }
                        }
                        binding!!.imageView.setImageURI(uri)



                        selectedImage = uri
                    }
                }
            }
        }
    }

    private fun Camera_ImageUpload(imageByte: ByteArray) {

        val storage = FirebaseStorage.getInstance()
        val reference = storage.reference
            .child("Profile")
            .child(auth!!.uid!!)

        reference.putBytes(imageByte).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val filePath = uri.toString()
                    val obj = HashMap<String, Any>()
                    obj["image"] = filePath
                    database!!.reference
                        .child("users")
                        .child(FirebaseAuth.getInstance().uid!!)
                        .updateChildren(obj).addOnSuccessListener { }
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



}