package com.edoctor.dlvn_sdk.model

// DLVNAccountInit
data class DLVNAccountInit(
    val accessToken: String
)

data class AccountInitResponse(
    val dlvnAccountInit: DLVNAccountInit
)


// SendbirdAccount
data class SendBirdAccount(
    val accountId: String?,
    val token: String
)

data class ThirdParty(
    val sendbird: SendBirdAccount
)
data class Account(
    val accountId: String,
    val thirdParty: ThirdParty
)
data class SBAccountResponse(
    val account: Account
)