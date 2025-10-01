package com.tamer.petapp.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Banner(
    @DocumentId 
    var id: String = "",
    
    var title: String = "",
    var description: String = "",
    var imageUrl: String = "",
    
    var buttonText: String? = null,
    var buttonUrl: String? = null,
    
    var priority: Int = 0,
    
    @PropertyName("isActive")
    var isActive: Boolean = true,
    
    var createdAt: Date = Date()
) {
    // Firestore için boş constructor
    constructor() : this(
        id = "",
        title = "",
        description = "",
        imageUrl = "",
        buttonText = null,
        buttonUrl = null,
        priority = 0,
        isActive = true,
        createdAt = Date()
    )
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Banner

        if (id != other.id) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (imageUrl != other.imageUrl) return false
        if (buttonText != other.buttonText) return false
        if (buttonUrl != other.buttonUrl) return false
        if (priority != other.priority) return false
        if (isActive != other.isActive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + imageUrl.hashCode()
        result = 31 * result + (buttonText?.hashCode() ?: 0)
        result = 31 * result + (buttonUrl?.hashCode() ?: 0)
        result = 31 * result + priority
        result = 31 * result + isActive.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "Banner(id='$id', title='$title', isActive=$isActive, priority=$priority)"
    }
} 