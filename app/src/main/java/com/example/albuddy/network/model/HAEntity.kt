package com.example.albuddy.network.model

data class HAEntity(
    val entity_id: String,
    val state: String,
    val attributes: HAAttributes? = null
)

data class HAAttributes(
    val friendly_name: String? = null
)
