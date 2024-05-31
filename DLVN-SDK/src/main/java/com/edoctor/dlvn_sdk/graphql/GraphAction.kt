package com.edoctor.dlvn_sdk.graphql

object GraphAction {
    object Mutation {
        const val dlvnAccountInit = "mutation DLVNAccountInit(\$data: String, \$signature: String, \$dcId: String) {\n" +
                "  dlvnAccountInit(data: \$data, signature: \$signature, dcId: \$dcId) {\n" +
                "    accessToken\n" +
                "   }\n" +
                "}"
        const val updateAccountAgreement = "mutation AccountUpdateAgreement(\$isAcceptAgreement: Date, \$isAcceptShareInfo: Date) {\n" +
                "      accountUpdateAggrement(isAcceptAgreement: \$isAcceptAgreement, isAcceptShareInfo: \$isAcceptShareInfo)\n" +
                "    }"
        const val eClinicExpireRinging = "mutation eClinicExpireRinging(\$eClinicId: String!, \$appointmentScheduleId: String!) {\n" +
                "  eClinicExpireRinging(eClinicId: \$eClinicId, appointmentScheduleId: \$appointmentScheduleId) {\n" +
                "    appointmentScheduleId\n" +
                "    state\n" +
                "  }\n" +
                "}"
        const val eClinicApproveCall = "mutation eClinicApprove(\$eClinicId: String!, \$appointmentScheduleId: String!) {\n" +
                "  eClinicApprove(eClinicId: \$eClinicId, appointmentScheduleId: \$appointmentScheduleId) {\n" +
                "    appointmentScheduleId\n" +
                "    state\n" +
                "    product {\n" +
                "      packages {\n" +
                "        ... on ProductPackageVideo {\n" +
                "          time\n" +
                "          type\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}"
        const val eClinicEndCall = "mutation EClinicEndCall(\$eClinicId: String!, \$appointmentScheduleId: String!, \$callId: String) {\n" +
                "  eClinicEndCall(eClinicId: \$eClinicId, appointmentScheduleId: \$appointmentScheduleId, \$callId: String) {\n" +
                "    appointmentScheduleId\n" +
                "  }\n" +
                "}"
        const val cancelAppointmentSchedule = "mutation CancelAppointment(\$eClinicId: String!, \$appointmentScheduleId: String!) {\n" +
                "  eClinicCancel(eClinicId: \$eClinicId, appointmentScheduleId: \$appointmentScheduleId) {\n" +
                "    appointmentScheduleId\n" +
                "    state\n" +
                "  }\n" +
                "}"
        const val confirmAppointmentSchedule = "mutation EClinicJoin(\$eClinicId: String!, \$appointmentScheduleId: String!) {\n" +
                "  eClinicJoin(eClinicId: \$eClinicId, appointmentScheduleId: \$appointmentScheduleId) {\n" +
                "    appointmentScheduleId\n" +
                "    state\n" +
                "  }\n" +
                "}"
    }
    object Query {
        const val sendBirdAccount = "query SendbirdAccount {\n" +
                "  account {\n" +
                "    accountId\n" +
                "    thirdParty {\n" +
                "      sendbird {\n" +
                "        token\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}"
        const val appointmentDetail = "query AppointmentDetail(\$appointmentScheduleId: String) {\n" +
                "  appointmentSchedules(appointmentScheduleId: \$appointmentScheduleId) {\n" +
                "    doctor {\n" +
                "      avatar\n" +
                "      degree {\n" +
                "        shortName\n" +
                "      }\n" +
                "      fullName\n" +
                "    }\n" +
                "    thirdParty {\n" +
                "      sendbird {\n" +
                "        channelUrl\n" +
                "      }\n" +
                "    }\n" +
                "    callDuration\n" +
                "  }\n" +
                "}"
        const val checkAccountExist = "query CheckAccountExist(\$accountId: String, \$phone: String) {\n" +
                "  checkAccountExist(accountId: \$accountId, phone: \$phone)" +
                "}"
    }
}