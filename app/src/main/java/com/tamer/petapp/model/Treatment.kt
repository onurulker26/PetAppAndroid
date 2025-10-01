package com.tamer.petapp.model

import java.util.Date

data class Treatment(
    var id: String = "",
    var petId: String = "",
    var ownerId: String = "",
    var name: String = "",
    var startDate: Date = Date(),
    var endDate: Date? = null,
    var status: TreatmentStatus = TreatmentStatus.ACTIVE,
    var notes: String = "",
    var medications: List<Medication> = emptyList(),
    var vetAppointments: List<VetAppointment> = emptyList(),
    var hasDocuments: Boolean = false
) {
    // Boş constructor Firestore için gerekli
    constructor() : this("", "", "", "", Date())
}

data class Medication(
    var id: String = "",
    var name: String = "",
    var dosage: String = "",
    var frequency: String = "",
    var startDate: Date = Date(),
    var endDate: Date? = null,
    var reminderTime: String = "",
    var notes: String = ""
) {
    constructor() : this("", "", "", "", Date())
}

data class TreatmentLog(
    var id: String = "",
    var treatmentId: String = "",
    var date: Date = Date(),
    var appetite: Int = 3, // 1-5 arası değerlendirme
    var energyLevel: Int = 3, // 1-5 arası değerlendirme
    var symptoms: String = "",
    var notes: String = "",
    var photoUrls: List<String> = emptyList()
) {
    constructor() : this("", "", Date())
}

data class VetAppointment(
    var id: String = "",
    var clinicName: String = "",
    var date: Date = Date(),
    var notes: String = "",
    var status: AppointmentStatus = AppointmentStatus.SCHEDULED
) {
    constructor() : this("", "", Date())
}

enum class TreatmentStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
} 