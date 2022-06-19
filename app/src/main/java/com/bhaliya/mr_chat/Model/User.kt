package com.bhaliya.mr_chat.Model

class User {

    var uid : String? = null
    var name : String? = null
    var phoneNumber : String? = null
    var profileImage : String? = null
    var status : String? = null


    constructor(){}

    constructor(uid: String?, name: String?, phoneNumber: String?, profileImage: String?,status : String?) {
        this.uid = uid
        this.name = name
        this.phoneNumber = phoneNumber
        this.profileImage = profileImage
        this.status = status
    }




}