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
        const val eClinicEndCall = "mutation EClinicEndCall(\$eClinicId: String!, \$appointmentScheduleId: String!) {\n" +
                "  eClinicEndCall(eClinicId: \$eClinicId, appointmentScheduleId: \$appointmentScheduleId) {\n" +
                "    appointmentScheduleId\n" +
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
        const val appointmentSchedules = "query AppointmentSchedules(\$appointmentScheduleId: String, \$accountId: String, \$profileId: String, \$eClinicId: String, \$warehouseId: String, \$state: [AppointmentScheduleState], \$scheduledAt: AppointmentScheduleScheduleAt, \$limit: PageLimitInput, \$sort: AppointmentScheduleSort) {\n" +
                "      appointmentSchedules(appointmentScheduleId: \$appointmentScheduleId, accountId: \$accountId, profileId: \$profileId, eClinicId: \$eClinicId, warehouseId: \$warehouseId, state: \$state, scheduledAt: \$scheduledAt, limit: \$limit, sort: \$sort) {\n" +
                "        appointmentScheduleId\n" +
                "        isVideoRecord\n" +
                "        doctor { fullName degree { degreeId shortName name } doctorId avatar account { fullName } }\n" +
                "        thirdParty { sendbird { channelUrl } }\n" +
                "        eClinic { \n" +
                "          doctor { \n" +
                "            fullName\n" +
                "            degree {\n" +
                "              shortName\n" +
                "            }\n" +
                "          }\n" +
                "          appointment {\n" +
                "            prepareJoin\n" +
                "            duration\n" +
                "            limitImmediate\n" +
                "            lateJoin\n" +
                "            callRingingTime\n" +
                "          }\n" +
                "          displayName \n" +
                "          eClinicId \n" +
                "          avatar \n" +
                "          title \n" +
                "          description \n" +
                "          slug \n" +
                "          isCloseChat\n" +
                "          isOpening\n" +
                "        }\n" +
                "        warehouse { warehouseId title }\n" +
                "        product {\n" +
                "          title\n" +
                "          productId\n" +
                "        }\n" +
                "        profile {\n" +
                "          profileId\n" +
                "          profileCode\n" +
                "          birthday\n" +
                "          account {\n" +
                "            accountId\n" +
                "            thirdParty { sendbird { token } }\n" +
                "          }\n" +
                "          fullName \n" +
                "          relation \n" +
                "        }\n" +
                "        comment {\n" +
                "          star\n" +
                "          content\n" +
                "        }\n" +
                "        package\n" +
                "        reason\n" +
                "        reasonImage\n" +
                "        scheduledAt\n" +
                "        state\n" +
                "        joinNumber\n" +
                "        joinMode\n" +
                "        supportNumber\n" +
                "        medicalExamination {\n" +
                "          medicalExaminationId\n" +
                "          content\n" +
                "          reason\n" +
                "          result\n" +
                "          appointmentSchedule {\n" +
                "            appointmentScheduleId\n" +
                "            profile {\n" +
                "              profileId\n" +
                "              profileCode\n" +
                "              fullName\n" +
                "              relation\n" +
                "            }\n" +
                "            doctor {\n" +
                "              fullName\n" +
                "              doctorId\n" +
                "              degree { degreeId shortName name }\n" +
                "            }\n" +
                "            eClinic {\n" +
                "              eClinicId\n" +
                "              displayName\n" +
                "              avatar\n" +
                "            }\n" +
                "            supportNumber\n" +
                "            scheduledAt\n" +
                "            createdAt\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }"
    }
    object Subscription {
        const val appointmentScheduleWs = "subscription subscribeToSchedule(\$eClinicId: String, \$accountId: String) {\n" +
                "  appointmentSchedule(eClinicId: \$eClinicId, accountId: \$accountId){\n" +
                "    appointmentScheduleId\n" +
                "    doctor { fullName degree { degreeId name shortName } workingRegion graduateYear doctorId avatar statistic {\n" +
                "      fusionRateCustomerAverage\n" +
                "      fusionRateAnswerCount\n" +
                "    } account { fullName } } \n" +
                "    thirdParty { sendbird { channelUrl } }\n" +
                "    eClinic { \n" +
                "      appointment {\n" +
                "        prepareJoin\n" +
                "        lateJoin\n" +
                "        callRingingTime\n" +
                "      }\n" +
                "      workingTimes { type duration data { dayOfWeek isActive blocks { from to isActive } } }\n" +
                "      displayName \n" +
                "      isCloseChat\n" +
                "      eClinicId \n" +
                "      avatar \n" +
                "      title \n" +
                "      description \n" +
                "      cover \n" +
                "      slug \n" +
                "    }\n" +
                "    profile {\n" +
                "      profileCode\n" +
                "      profileId\n" +
                "      avatar\n" +
                "      birthday\n" +
                "      account {\n" +
                "        accountId\n" +
                "        fullName\n" +
                "        type\n" +
                "        gender\n" +
                "        sourceType\n" +
                "        deviceId\n" +
                "        accessToken\n" +
                "        thirdParty { sendbird { token } }\n" +
                "      }\n" +
                "      fullName \n" +
                "      relation \n" +
                "      phone \n" +
                "    }\n" +
                "    comment {\n" +
                "      star\n" +
                "      content\n" +
                "    }\n" +
                "    warehouse { warehouseId }\n" +
                "    package\n" +
                "    reason\n" +
                "    reasonImage\n" +
                "    scheduledAt\n" +
                "    scheduleToken\n" +
                "    createdAt\n" +
                "    updatedAt\n" +
                "    state\n" +
                "    joinNumber\n" +
                "    joinMode\n" +
                "    joinAt\n" +
                "    supportNumber\n" +
                "    medicalExamination {\n" +
                "      medicalExaminationId\n" +
                "    }    \n" +
                "  }\n" +
                "}"
    }
}