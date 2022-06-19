package com.bhaliya.mr_chat.Model

class Message {

    var messageId :String? =null
    var message :String? =null
    var senderId :String? =null
    var reciverId :String? =null
    var imageUrl :String? =null
    var pdfUrl :String? =null
    var timeStamp :String?  =null
    var FileFormate : String? = null
    var FileName : String? = null

    constructor(){}

    constructor(
        messageId: String?,
        message: String?,
        senderId: String?,
        reciverId: String?,
        imageUrl: String?,
        pdfUrl: String?,
        timeStamp: String?,
        FileFormate: String,
        FileName: String
    )
    {
        this.messageId = messageId
        this.message = message
        this.senderId = senderId
        this.reciverId = reciverId
        this.imageUrl = imageUrl
        this.pdfUrl = pdfUrl
        this.timeStamp = timeStamp
        this.FileFormate = FileFormate
        this.FileName = FileName
    }


}