package com.example.dlvn_sdk.graphql

object GraphAction {
    object Mutation {
        const val dlvnAccountInit = "mutation DLVNAccountInit(\$data: String, \$signature: String, \$dcId: String) { dlvnAccountInit(data: \$data, signature: \$signature, dcId: \$dcId) { accessToken }}"
    }
    object Query {

    }
}