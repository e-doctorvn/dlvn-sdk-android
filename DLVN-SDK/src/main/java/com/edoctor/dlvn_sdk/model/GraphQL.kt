package com.edoctor.dlvn_sdk.model

data class DLVNAccountInit(
    val accessToken: String
)

data class AccountInitResponse(
    val dlvnAccountInit: DLVNAccountInit
)