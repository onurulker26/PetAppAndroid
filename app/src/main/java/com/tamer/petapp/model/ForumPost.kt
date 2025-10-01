package com.tamer.petapp.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class ForumPost(
    @DocumentId
    var id: String = "",
    var title: String = "",
    var content: String = "",
    var categoryId: String? = null,
    var categoryName: String? = null,
    var userId: String = "",
    var userName: String = "",
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var likes: List<String> = listOf(),  // Beğenen kullanıcıların ID'leri
    var commentCount: Int = 0,
    var imageUrl: String? = null,
    var reportedBy: List<String> = listOf(),  // Bildiren kullanıcıların ID'leri
    var reportReason: String? = null   // Rapor edilme sebebi
) {
    // Firestore için boş constructor
    constructor() : this(
        id = "",
        title = "",
        content = "",
        categoryId = null,
        categoryName = null,
        userId = "",
        userName = "",
        createdAt = null,
        updatedAt = null,
        likes = listOf(),
        commentCount = 0,
        imageUrl = null,
        reportedBy = listOf(),
        reportReason = null
    )
} 