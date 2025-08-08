package com.claudehooks.dashboard.data.remote

data class RedisConfig(
    val host: String,
    val port: Int = 18773,
    val password: String?,
    val useTls: Boolean = true,
    val channel: String = "hooksdata",
    val certPath: String? = null
)