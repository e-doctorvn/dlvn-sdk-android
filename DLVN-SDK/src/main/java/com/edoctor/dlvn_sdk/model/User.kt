package com.edoctor.dlvn_sdk.model

data class User(
    val id: String,
    val email: String,
    val password: String,
    val name: String,
    val role: String,
    val avatar: String
)
