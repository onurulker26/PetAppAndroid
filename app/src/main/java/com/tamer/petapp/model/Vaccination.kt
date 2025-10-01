package com.tamer.petapp.model

import com.google.firebase.Timestamp
import java.util.Date

data class Vaccination(
    var id: String = "",
    var petId: String = "",
    var ownerId: String = "",
    var name: String = "",
    var date: Date = Date(),
    var nextDate: Date? = null,
    var notes: String = "",
    var status: VaccinationStatus = VaccinationStatus.UPCOMING
) {
    // Boş constructor Firestore için gerekli
    constructor() : this("", "", "", "", Date())
}

enum class VaccinationStatus {
    COMPLETED,
    UPCOMING,
    OVERDUE
} 