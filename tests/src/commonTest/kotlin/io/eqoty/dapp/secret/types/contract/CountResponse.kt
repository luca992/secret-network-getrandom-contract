package io.eqoty.dapp.secret.types.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    @SerialName("randoms_response") val randomsResponse: List<String>
)
