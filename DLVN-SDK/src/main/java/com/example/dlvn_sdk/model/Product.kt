package com.example.dlvn_sdk.model

data class Product(
    val id: String,
    val title: String,
    val price: String,
    val category: Any,
    val description: String,
    val images: List<String>
)
