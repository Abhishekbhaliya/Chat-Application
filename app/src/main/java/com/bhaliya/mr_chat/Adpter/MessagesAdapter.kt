package com.malkinfo.answerandquestion.adapter

import android.app.AlertDialog
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bhaliya.mr_chat.MessageImage_Show
import com.bhaliya.mr_chat.Model.Message
import com.bhaliya.mr_chat.R
import com.bhaliya.mr_chat.databinding.DeleteLayoutBinding
import com.bhaliya.mr_chat.databinding.ReceiveMsgBinding
import com.bhaliya.mr_chat.databinding.SendMsgBinding
import com.bhaliya.mr_chat.utils.showToast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.File


class MessagesAdapter(
        var context: Context,
        messages: ArrayList<Message>?,
        senderRoom: String,
        receiverRoom: String
) :
        RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    lateinit var messages: ArrayList<Message>
    private val ITEM_SENT = 1
    private val ITEM_RECEIVE = 2
    var senderRoom: String
    var receiverRoom: String

    var dialog: ProgressDialog? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
        return if (viewType == ITEM_SENT) {
            val view: View = LayoutInflater.from(context).inflate(com.bhaliya.mr_chat.R.layout.send_msg, parent, false)
            SentViewHolder(view)
        }
        else {
            val view: View =
                    LayoutInflater.from(context).inflate(com.bhaliya.mr_chat.R.layout.receive_msg, parent, false)
            ReceiverViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message: Message = messages[position]
        return if (FirebaseAuth.getInstance().uid == message.senderId) {
            ITEM_SENT
        } else {
            ITEM_RECEIVE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        dialog = ProgressDialog(context)
        dialog!!.setMessage("Sending...")
        dialog!!.setCancelable(false)


        val message: Message = messages[position]

        if (holder.javaClass == SentViewHolder::class.java) {

            val viewHolder = holder as SentViewHolder

            viewHolder.itemView.setOnLongClickListener {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.delete_layout, null)
                val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
                val dialog: AlertDialog = AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setView(binding.root)
                    .create()

                binding.everyone.setOnClickListener(View.OnClickListener {
                    message.message = "This message is removed."
                    message.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("chats")
                            .child(senderRoom)
                            .child("messages")
                            .child(it1).setValue(message)
                    }

                        message.messageId?.let { it1 ->
                            FirebaseDatabase.getInstance().reference
                                .child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(it1).setValue(message)
                        }

                    dialog.dismiss()
                })
                binding.delete.setOnClickListener(View.OnClickListener {
                    message.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("chats")
                            .child(senderRoom)
                            .child("messages")
                            .child(it1).setValue(null)
                    }
                    dialog.dismiss()
                })
                binding.cancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
                dialog.show()
                false
            }

            if (message.pdfUrl != "")
            {
                viewHolder.binding.SpdfLinear.visibility = View.VISIBLE
                viewHolder.binding.Spdfimage.visibility = View.VISIBLE
                viewHolder.binding.SmLinear.visibility = View.GONE
                viewHolder.binding.Smessage.visibility = View.GONE
                viewHolder.binding.Simage.visibility = View.GONE
                viewHolder.binding.SmLinear1.visibility = View.GONE


                if(message.FileFormate!!.contains(".pdf"))
                {
                    Glide.with(context)
                        .load(message.pdfUrl)
                        .placeholder(R.drawable.pdf_file)
                        .into(viewHolder.binding.Spdfimage)
                }
                else
                    if (message.FileFormate!!.contains(".doc") || message.pdfUrl!!.contains(".docx") )
                    {
                        Glide.with(context)
                            .load(message.pdfUrl)
                            .placeholder(R.drawable.doc)
                            .into(viewHolder.binding.Spdfimage)
                    }


                viewHolder.binding.SdocName.text = message.FileName
                viewHolder.binding.SpdfDateTextchat1.text = message.timeStamp

                viewHolder.binding.Spdfimage.setOnLongClickListener {
                    val view: View =
                        LayoutInflater.from(context).inflate(R.layout.reciver_delete_layout, null)
                    val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
                    val dialog: AlertDialog = AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setView(binding.root)
                        .create()
                    binding.delete.setOnClickListener(View.OnClickListener {
                        message.messageId?.let { it1 ->
                            FirebaseDatabase.getInstance().reference
                                .child("chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(it1).setValue(null)
                        }
                        dialog.dismiss()
                    })
                    binding.cancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
                    dialog.show()
                    false
                }
                viewHolder.binding.SpdfLinear.setOnClickListener {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(messages.get(position).pdfUrl))
                    viewHolder.itemView.context.startActivity(intent)
                }
            }
            else
            if (message.imageUrl != "") {

                viewHolder.binding.SpdfLinear.visibility = View.GONE
                viewHolder.binding.Spdfimage.visibility = View.GONE
                viewHolder.binding.Simage.visibility = View.VISIBLE
                viewHolder.binding.SmLinear1.visibility = View.VISIBLE
                viewHolder.binding.Smessage.visibility = View.GONE
                viewHolder.binding.SmLinear.visibility = View.GONE

                Glide.with(context)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(viewHolder.binding.Simage)

                viewHolder.binding.Simagname.text = message.FileName
                viewHolder.binding.ScalDateTextchat1.text = message.timeStamp

                viewHolder.binding.Simage.setOnLongClickListener {
                    val view: View =
                        LayoutInflater.from(context).inflate(R.layout.reciver_delete_layout, null)
                    val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
                    val dialog: AlertDialog = AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setView(binding.root)
                        .create()
                    binding.delete.setOnClickListener(View.OnClickListener {
                        message.messageId?.let { it1 ->
                            FirebaseDatabase.getInstance().reference
                                .child("chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(it1).setValue(null)
                        }
                        dialog.dismiss()
                    })
                    binding.cancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
                    dialog.show()
                    false
                }

                viewHolder.binding.Simage.setOnClickListener {
                    val intent = Intent(context, MessageImage_Show::class.java)
                    intent.putExtra("image1",message.imageUrl)
                    intent.putExtra("senderID",message.senderId)
                    intent.putExtra("reciverID",message.reciverId)
                    context.startActivity(intent)
                }
            }
            else
            if (message.message != "")
            {
                viewHolder.binding.SpdfLinear.visibility = View.GONE
                viewHolder.binding.Spdfimage.visibility = View.GONE
                viewHolder.binding.Simage.visibility = View.GONE
                viewHolder.binding.SmLinear1.visibility = View.GONE
                viewHolder.binding.Smessage.visibility = View.VISIBLE
                viewHolder.binding.SmLinear.visibility = View.VISIBLE

                viewHolder.binding.Smessage.text = message.message
                viewHolder.binding.ScalDateTextchat.text = message.timeStamp
            }
            else
            {
                viewHolder.binding.SpdfLinear.visibility = View.GONE
                viewHolder.binding.Spdfimage.visibility = View.GONE
                viewHolder.binding.Simage.visibility = View.GONE
                viewHolder.binding.SmLinear1.visibility = View.GONE
                viewHolder.binding.Smessage.visibility = View.GONE
                viewHolder.binding.SmLinear.visibility = View.GONE
            }
        }

        else {
            val viewHolder = holder as ReceiverViewHolder

            if (message.pdfUrl != "")
            {
                viewHolder.binding.RpdfLinear.visibility = View.VISIBLE
                viewHolder.binding.Rpdfimage.visibility = View.VISIBLE
                viewHolder.binding.RpdfdownBTN.visibility =  View.VISIBLE
                viewHolder.binding.RmLinear1.visibility = View.GONE
                viewHolder.binding.Rmessage.visibility = View.GONE
                viewHolder.binding.RmLinear.visibility = View.GONE
                viewHolder.binding.Rimage.visibility = View.GONE

                if(message.FileFormate!!.contains(".pdf"))
                {
                    Glide.with(context)
                        .load(message.pdfUrl)
                        .placeholder(R.drawable.pdf_file)
                        .into(viewHolder.binding.Rpdfimage)
                }
                else
                    if (message.FileFormate!!.contains(".doc") || message.pdfUrl!!.contains(".docx") )
                    {
                        Glide.with(context)
                            .load(message.pdfUrl)
                            .placeholder(R.drawable.doc)
                            .into(viewHolder.binding.Rpdfimage)
                    }

                viewHolder.binding.Rdocname.text = message.FileName
                viewHolder.binding.RpdfDateTextchat1.text = message.timeStamp

                viewHolder.binding.RpdfLinear.setOnLongClickListener {
                    val view: View =
                        LayoutInflater.from(
                            context).inflate(R.layout.reciver_delete_layout, null)

                    val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)

                    val dialog: AlertDialog = AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setView(binding.root)
                        .create()


                    binding.delete.setOnClickListener(View.OnClickListener {
                        message.messageId?.let { it1 ->
                            FirebaseDatabase.getInstance().reference
                                .child("chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(it1).setValue(null)
                        }
                        dialog.dismiss()
                    })
                    binding.cancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
                    dialog.show()
                    false
                }

                viewHolder.binding.RpdfdownBTN.setOnClickListener {
                    dialog!!.show()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(messages.get(position).pdfUrl))
                    viewHolder.itemView.context.startActivity(intent)
                    viewHolder.binding.RpdfdownBTN.visibility =  View.GONE
                    dialog!!.dismiss()
                }



            }
            else
            if (message.imageUrl != "") {

                viewHolder.binding.RpdfLinear.visibility = View.GONE
                viewHolder.binding.Rpdfimage.visibility = View.GONE
                viewHolder.binding.Rimage.visibility = View.GONE
                viewHolder.binding.RmLinear1.visibility = View.VISIBLE
                viewHolder.binding.Rmessage.visibility = View.GONE
                viewHolder.binding.RmLinear.visibility = View.GONE

                viewHolder.binding.RimageDowenload.visibility = View.VISIBLE


                Glide.with(context)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(viewHolder.binding.Rimage)


                viewHolder.binding.RcalDateTextchat1.text = message.timeStamp
                viewHolder.binding.Rimagname.text = message.FileName

                viewHolder.binding.Rimage.setOnLongClickListener {
                    val view: View =
                        LayoutInflater.from(context).inflate(R.layout.reciver_delete_layout, null)

                    val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)

                    val dialog: AlertDialog = AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setView(binding.root)
                        .create()




                    binding.delete.setOnClickListener(View.OnClickListener {
                        message.messageId?.let { it1 ->
                            FirebaseDatabase.getInstance().reference
                                .child("chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(it1).setValue(null)
                        }
                        dialog.dismiss()
                    })
                    binding.cancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
                    dialog.show()
                    false
                }


                viewHolder.binding.Rimage.setOnClickListener {
                    val intent = Intent(context, MessageImage_Show::class.java)
                    intent.putExtra("image1",message.imageUrl)

//                    var Check = check(message.imageUrl)
//                    Log.d(TAG, "onBindViewHolder: "+Check)

                    context.startActivity(intent)
                }
                viewHolder.binding.RimageDowenload.setOnClickListener {
                    dowenloadImage(message.imageUrl,context,message.FileName)

                    viewHolder.binding.RimageDowenload.visibility = View.GONE
                    viewHolder.binding.Rimage.visibility = View.VISIBLE
                }
            }

            else
            if (message.message != "") {
                viewHolder.binding.RpdfLinear.visibility = View.GONE
                viewHolder.binding.Rpdfimage.visibility = View.GONE
                viewHolder.binding.Rimage.visibility = View.GONE
                viewHolder.binding.RmLinear1.visibility = View.GONE
                viewHolder.binding.Rmessage.visibility = View.VISIBLE
                viewHolder.binding.RmLinear.visibility = View.VISIBLE
                viewHolder.binding.Rmessage.text = message.message
                viewHolder.binding.RcalDateTextchat.text = message.timeStamp
            }
            else
            {
                viewHolder.binding.RpdfLinear.visibility = View.GONE
                viewHolder.binding.Rpdfimage.visibility = View.GONE
                viewHolder.binding.Rimage.visibility = View.GONE
                viewHolder.binding.RmLinear1.visibility = View.GONE
                viewHolder.binding.Rmessage.visibility = View.GONE
                viewHolder.binding.RmLinear.visibility = View.GONE
            }

            viewHolder.itemView.setOnLongClickListener {
                val view: View =
                        LayoutInflater.from(context).inflate(R.layout.reciver_delete_layout, null)
                val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
                val dialog: AlertDialog = AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setView(binding.root)
                        .create()


                binding.delete.setOnClickListener(View.OnClickListener {
                    message.messageId?.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                                .child("chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(it1).setValue(null)
                    }
                    dialog.dismiss()
                })
                binding.cancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
                dialog.show()
                false
            }
        }
    }





    private fun DocPDF_download(pdfUrl: String?) {
        val request = DownloadManager.Request(Uri.parse(pdfUrl))
        request.setTitle(pdfUrl)
        request.setMimeType("applcation/*")
        request.allowScanningByMediaScanner()
        request.setAllowedOverMetered(true)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS, pdfUrl
        )
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        dialog!!.dismiss()

    }



    fun check(paths: String?): Boolean
    {
        val path: String =
            java.lang.String.valueOf(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + paths + ".JPEG"))
        val file = File(path)
        return file.exists()
    }



    private fun dowenloadImage(imageUrl: String?, context: Context, fileName: String?){

        val path: String =
            java.lang.String.valueOf(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + "sex"))

        Log.d(TAG, "dowenloadImage: "+path)

        val file = File(path)

        if (file.exists())
        {
            Toast.makeText(context, "already", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val cookie = CookieManager.getInstance().getCookie(imageUrl)
            val downloadRequest = DownloadManager.Request(Uri.parse(imageUrl)).apply {
                setTitle("Image")
                setDescription("Image")
                addRequestHeader("cookie", cookie)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$fileName")
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(downloadRequest)
            (context as AppCompatActivity).showToast("Downloading started.")
            dialog!!.dismiss()
            val intent = Intent(context, MessageImage_Show::class.java)
            intent.putExtra("image1",imageUrl)
            context.startActivity(intent)
        }

    }





    override fun getItemCount(): Int {
        return messages.size
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: SendMsgBinding
        init {
            binding = SendMsgBinding.bind(itemView)
        }
    }

    inner class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ReceiveMsgBinding
        init {
            binding = ReceiveMsgBinding.bind(itemView)
        }
    }

    init {

        if (messages != null) {
            this.messages = messages
        }
        this.senderRoom = senderRoom
        this.receiverRoom = receiverRoom

    }



}