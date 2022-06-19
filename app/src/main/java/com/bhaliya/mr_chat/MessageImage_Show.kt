package com.bhaliya.mr_chat


import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bhaliya.mr_chat.InternetService.ConnectionLiveData
import com.bhaliya.mr_chat.databinding.ActivityChatImageShowBinding
import com.bhaliya.mr_chat.utils.showToast
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class MessageImage_Show : AppCompatActivity() {

    var binding : ActivityChatImageShowBinding? = null
    var database: FirebaseDatabase? = null
    var senderRoom: String? = null
    var receiverRoom: String? = null
    private lateinit var cld : ConnectionLiveData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatImageShowBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        checkNetworkConnection()



        database =FirebaseDatabase.getInstance()

        val profile = intent.getStringExtra("image1")
        senderRoom = intent.getStringExtra("senderID")
        receiverRoom = intent.getStringExtra("reciverID")

        Glide.with(this@MessageImage_Show).load(profile)
            .placeholder(R.drawable.placeholder)
            .into(binding!!.chatimage)

        val permissionContract = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultMap ->
            resultMap.entries.forEach { entry ->
                if (entry.value) {
                 downloadImage(this, profile!!)
                }
            }
        }

//        binding!!.ivDownload.setOnClickListener {
//            if (Build.VERSION.SDK_INT >= 29)
//               downloadImage(this, profile!!)
//            else
//                permissionContract.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
//        }

    }


    fun downloadImage(context: Context, imageUrl: String) {
        val cookie = CookieManager.getInstance().getCookie(imageUrl)

        val downloadRequest = DownloadManager.Request(Uri.parse(imageUrl)).apply {
            setTitle("Image")
            setDescription("Image")
            addRequestHeader("cookie", cookie)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${System.currentTimeMillis()}.jpg")
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(downloadRequest)

        (context as AppCompatActivity).showToast("Downloading started.")
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