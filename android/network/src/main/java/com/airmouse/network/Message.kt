package com.airmouse.network

import kotlinx.serialization.Serializable

@Serializable
sealed interface ClientMessage

@Serializable
@kotlinx.serialization.SerialName("move")
data class Move(val dx: Float, val dy: Float): ClientMessage

@Serializable
@kotlinx.serialization.SerialName("click")
data class Click(val id: String? = null): ClientMessage

@Serializable
@kotlinx.serialization.SerialName("doubleclick")
data class DoubleClick(val id: String? = null): ClientMessage

@Serializable
@kotlinx.serialization.SerialName("rightclick")
data class RightClick(val id: String? = null): ClientMessage

@Serializable
@kotlinx.serialization.SerialName("scroll")
data class Scroll(val delta: Int, val id: String? = null): ClientMessage

@Serializable
data class Ack(val type: String = "ack", val id: String)

@Serializable
data class DiscoveryResponse(val type: String = "discovery_response", val ip: String, val port: Int, val mdns: String? = null)
