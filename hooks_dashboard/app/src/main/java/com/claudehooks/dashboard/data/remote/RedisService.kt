package com.claudehooks.dashboard.data.remote

import com.claudehooks.dashboard.domain.model.HookData
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SslOptions
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.RedisPubSubListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@Singleton
class RedisService @Inject constructor(
    private val config: RedisConfig
) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null
    
    suspend fun connect(): Boolean {
        return try {
            val redisUri = buildRedisUri()
            client = RedisClient.create(redisUri)
            
            connection = client?.connect()
            pubSubConnection = client?.connectPubSub()
            
            Timber.d("Connected to Redis: ${config.host}:${config.port}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to Redis")
            false
        }
    }
    
    fun subscribeToHooks(): Flow<HookData> = callbackFlow {
        try {
            val pubSub = pubSubConnection ?: throw IllegalStateException("Not connected to Redis")
            
            pubSub.addListener(object : RedisPubSubListener<String, String> {
                override fun message(channel: String, message: String) {
                    try {
                        if (channel == config.channel) {
                            val hookData = json.decodeFromString<HookData>(message)
                            trySend(hookData)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse hook data: $message")
                    }
                }
                
                override fun message(pattern: String, channel: String, message: String) {
                    // Not used for exact channel subscription
                }
                
                override fun subscribed(channel: String, count: Long) {
                    Timber.d("Subscribed to channel: $channel")
                }
                
                override fun unsubscribed(channel: String, count: Long) {
                    Timber.d("Unsubscribed from channel: $channel")
                }
                
                override fun psubscribed(pattern: String, count: Long) {
                    // Not used for exact channel subscription
                }
                
                override fun punsubscribed(pattern: String, count: Long) {
                    // Not used for exact channel subscription
                }
            })
            
            pubSub.sync().subscribe(config.channel)
            
            awaitClose {
                try {
                    pubSub.sync().unsubscribe(config.channel)
                } catch (e: Exception) {
                    Timber.e(e, "Error unsubscribing from Redis")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up Redis subscription")
            close(e)
        }
    }
    
    suspend fun disconnect() {
        try {
            pubSubConnection?.close()
            connection?.close()
            client?.shutdown()
            
            pubSubConnection = null
            connection = null
            client = null
            
            Timber.d("Disconnected from Redis")
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from Redis")
        }
    }
    
    fun isConnected(): Boolean {
        return connection?.isOpen == true
    }
    
    private fun buildRedisUri(): RedisURI {
        val builder = RedisURI.Builder
            .redis(config.host, config.port)
            
        if (config.password != null) {
            builder.withPassword(config.password.toCharArray())
        }
        
        if (config.useTls) {
            builder.withSsl(true)
            
            // Configure SSL context with custom certificates if available
            config.certPath?.let { certPath ->
                try {
                    val sslContext = createSslContext(certPath)
                    builder.withSslOptions(
                        SslOptions.builder()
                            .sslContext(sslContext)
                            .build()
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load custom SSL certificates, using default")
                }
            }
        }
        
        return builder.build()
    }
    
    private fun createSslContext(certPath: String): SSLContext {
        val certDir = File(certPath)
        if (!certDir.exists()) {
            throw IllegalArgumentException("Certificate directory does not exist: $certPath")
        }
        
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        
        // Load certificates from the cert directory
        certDir.listFiles { _, name -> name.endsWith(".crt") || name.endsWith(".pem") }
            ?.forEach { certFile ->
                try {
                    certFile.inputStream().use { inputStream ->
                        val cert = certificateFactory.generateCertificate(inputStream)
                        keyStore.setCertificateEntry(certFile.nameWithoutExtension, cert)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load certificate: ${certFile.name}")
                }
            }
        
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        
        return sslContext
    }
}