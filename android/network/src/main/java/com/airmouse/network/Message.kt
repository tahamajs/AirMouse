package com.airmouse.network

data class MoveMessage(
	val type: String = "move",
	val dx: Float,
	val dy: Float
)

data class ClickMessage(
	val type: String = "click",
	val id: Long? = null
)

data class DoubleClickMessage(
	val type: String = "doubleclick",
	val id: Long? = null
)

data class RightClickMessage(
	val type: String = "rightclick",
	val id: Long? = null
)

data class ScrollMessage(
	val type: String = "scroll",
	val delta: Int,
	val id: Long? = null
)

data class HelloMessage(
	val type: String = "hello",
	val name: String
)

data class AckMessage(
	val type: String = "ack",
	val id: Long
)

data class DiscoveryResponse(
	val type: String = "discovery_response",
	val ip: String,
	val port: Int,
	val mdns: String? = null
)

data class DiscoveredServer(
	val host: String,
	val port: Int
) {
	override fun toString(): String = "$host:$port"
}
