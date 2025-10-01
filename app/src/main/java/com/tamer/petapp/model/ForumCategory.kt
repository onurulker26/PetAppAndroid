package com.tamer.petapp.model

import com.google.firebase.firestore.DocumentId

data class ForumCategory(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var iconUrl: String? = null,
    var color: String? = null,
    var postCount: Int = 0,
    var order: Int = 0,
    var lastPostAt: Long = 0,
    var lastPostTitle: String = "",
    var lastPostAuthor: String = ""
) {
    constructor() : this("", "", "", null, null, 0, 0, 0, "", "")
    
    override fun toString(): String {
        return name
    }
} 