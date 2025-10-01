package com.tamer.petapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class TreatmentDocument(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var uploadDate: Date = Date(),
    var fileUrl: String = "",
    var fileContent: String = "",
    @get:Exclude
    var localUri: String? = null,
    @get:Exclude
    val extraProperties: MutableMap<String, Any> = mutableMapOf()
) {
    // Boş constructor Firebase için gerekli
    constructor() : this("", "", "", Date(), "", "", null)
    
    // toString metodu debug için
    override fun toString(): String {
        return "TreatmentDocument(id='$id', name='$name', type='$type', fileUrl='$fileUrl')"
    }
} 