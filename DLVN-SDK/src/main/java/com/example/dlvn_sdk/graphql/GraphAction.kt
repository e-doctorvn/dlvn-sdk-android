package com.example.dlvn_sdk.graphql

object GraphAction {
    object Mutation {
        const val dlvnAccountInit = "mutation DLVNAccountInit(\$data: String, \$signature: String, \$dcId: String) {\n" +
                "  dlvnAccountInit(data: \$data, signature: \$signature, dcId: \$dcId) {\n" +
                "    accessToken\n" +
                "   }\n" +
                "}"
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
    }
}