package com.edoctor.dlvn_sdk.model

// DLVNAccountInit
data class DLVNAccountInit(
    val accessToken: String
)

data class AccountInitResponse(
    val dlvnAccountInit: DLVNAccountInit
)


// SendbirdAccount
data class Sendbird(
    val channelUrl: String?
)
data class SendBirdAccount(
    val accountId: String?,
    val token: String?,
)
data class ThirdParty(
    val sendbird: SendBirdAccount
)
data class ADThirdParty(
    val sendbird: Sendbird
)
data class Account(
    val accountId: String,
    val thirdParty: ThirdParty
)
data class SBAccountResponse(
    val account: Account
)

// AppointmentDetail
data class Degree(
    val shortName: String
)
data class Doctor(
    val avatar: String,
    val fullName: String,
    val degree: Degree
)
data class AppointmentDetail(
    val doctor: Doctor,
    val thirdParty: ADThirdParty
)

data class AppointmentDetailInfo(
    var doctor: Doctor = Doctor("", "", Degree("")),
    var channelUrl: String = ""
)

data class AppointmentDetailResponse(
    val appointmentSchedules: Array<AppointmentDetail>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppointmentDetailResponse

        if (!appointmentSchedules.contentEquals(other.appointmentSchedules)) return false

        return true
    }

    override fun hashCode(): Int {
        return appointmentSchedules.contentHashCode()
    }
}