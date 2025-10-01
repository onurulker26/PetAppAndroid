package com.tamer.petapp.model

data class VetClinic(
    val id: String,
    val placeId: String = "",
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double,
    val rating: Double,
    val openingHours: String = "Çalışma saati bilgisi yok",
    val isOpen: Boolean = false,
    val phoneNumber: String = "Telefon numarası bulunamadı"
) {
    // Geriye dönük uyumluluk için - 'phone' özelliği 'phoneNumber' ile aynı değeri döndürür
    val phone: String
        get() = phoneNumber
} 