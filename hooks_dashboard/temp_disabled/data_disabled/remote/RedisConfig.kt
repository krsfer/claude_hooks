package com.claudehooks.dashboard.data.remote

data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String?,
    val useTls: Boolean = true,
    val channel: String = "hooksdata",
    val certPath: String? = null
) {
    companion object {
        fun fromEnvironment(): RedisConfig {
            // Default config based on requirements
            return RedisConfig(
                host = "redis-18773.c311.eu-central-1-1.ec2.redns.redis-cloud.com",
                port = 18773,
                password = null, // Will be set from environment
                useTls = true,
                channel = "hooksdata",
                certPath = "/Users/${System.getProperty("user.name")}/.redis/certs/"
            )
        }
    }
}