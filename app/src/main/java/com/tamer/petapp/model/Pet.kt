package com.tamer.petapp.model

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable
import java.util.*

@IgnoreExtraProperties
data class Pet(
    var id: String = "",
    val name: String = "",
    val type: String = "", // Kedi, köpek, kuş vb.
    val breed: String = "", // Irk
    val birthDate: Long = 0, // Doğum tarihi (timestamp)
    val gender: String = "", // Cinsiyet
    val weight: Double = 0.0, // Ağırlık
    val imageUrl: String = "", // Evcil hayvan fotoğrafı (eski yöntem)
    val base64Image: String = "", // Base64 formatında fotoğraf (yeni yöntem)
    var ownerId: String = "", // Sahibinin kullanıcı ID'si
    val notes: String = "", // Notlar
    val createdAt: Long = System.currentTimeMillis() // Oluşturulma zamanı, varsayılan olarak şimdiki zaman
) : Serializable {
    // Firebase için boş constructor
    constructor() : this("", "", "", "", 0, "", 0.0, "", "", "", "", System.currentTimeMillis())
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pet

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (breed != other.breed) return false
        if (birthDate != other.birthDate) return false
        if (gender != other.gender) return false
        if (weight != other.weight) return false
        if (notes != other.notes) return false
        if (ownerId != other.ownerId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + breed.hashCode()
        result = 31 * result + birthDate.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + ownerId.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "Pet(id='$id', name='$name', type='$type', breed='$breed', ownerId='$ownerId')"
    }
} 