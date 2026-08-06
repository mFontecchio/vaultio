package com.mrhayami.vaultio.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CollectionExportDto(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val folders: List<FolderDto> = emptyList(),
    val userCards: List<UserCardDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FolderDto(
    val id: Long,
    val name: String,
    val icon: String?,
    val color: String?
)

@JsonClass(generateAdapter = true)
data class UserCardDto(
    val cardId: String,
    val quantity: Int,
    val condition: String,
    val printing: String,
    val finish: String,
    val manualPrice: Double?,
    val dateAdded: Long,
    val folderIds: List<Long> = emptyList()
)
