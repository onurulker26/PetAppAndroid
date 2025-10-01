package com.tamer.petapp.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class ForumComment(
    @DocumentId
    var id: String = "",
    var postId: String = "",
    var userId: String = "",
    var userName: String = "",
    var content: String = "",
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var likes: List<String> = listOf(),  // Beğenen kullanıcıların ID'leri
    var reportedBy: MutableList<String> = mutableListOf(),  // Bildiren kullanıcıların ID'leri
    var reportReason: String? = null,  // Rapor edilme sebebi
    var parentCommentId: String? = null,  // Eğer bir yoruma cevapsa, ana yorumun ID'si
    var replies: List<String> = listOf()  // Bu yoruma yapılan cevapların ID'leri
) {
    // Firestore için boş constructor
    constructor() : this(
        id = "",
        postId = "",
        userId = "",
        userName = "",
        content = "",
        createdAt = null,
        updatedAt = null,
        likes = listOf(),
        reportedBy = mutableListOf(),
        reportReason = null,
        parentCommentId = null,
        replies = listOf()
    )
} 