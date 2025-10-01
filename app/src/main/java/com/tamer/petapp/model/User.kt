package com.tamer.petapp.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val photoData: String = "", // Base64 formatında profil fotoğrafı
    val createdAt: Long = 0,
    val phone: String = ""
) {
    // Firebase için boş constructor gerekli
    constructor() : this("", "", "", "", "", 0, "")
} 