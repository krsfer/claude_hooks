package com.claudehooks.dashboard.data.remote

import android.content.Context
import com.claudehooks.dashboard.data.model.RedisHookData
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
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class RedisService(
    private val config: RedisConfig,
    private val context: Context
) {
    
    companion object {
        private const val CA_CERT_PATH = "redis_ca.pem"
        private const val CLIENT_CERT_PATH = "redis-db-12916440.crt"
        private const val CLIENT_KEY_PATH = "redis-db-12916440.key"
        private const val CONNECTION_TIMEOUT_SECONDS = 20L
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null
    
    suspend fun connect(): Boolean {
        return try {
            // Use hostname instead of IP for proper SSL certificate validation
            val redisHost = config.host
            
            Timber.d("Connecting to Redis at $redisHost:${config.port}")
            
            // Create Redis URI with proper SSL configuration
            val uriBuilder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(config.port)
                .withTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
            
            // Add authentication
            if (!config.password.isNullOrEmpty()) {
                uriBuilder.withPassword(config.password.toCharArray())
            }
            
            // Configure SSL with certificates
            val sslOptions = loadCertificatesFromAssets()
            if (sslOptions != null && config.useTls) {
                Timber.d("Using SSL/TLS with certificates from assets")
                uriBuilder.withSsl(true)
                
                // Enable peer verification for hostname-based connections
                uriBuilder.withVerifyPeer(true)
                Timber.d("Enabled peer verification for hostname-based connection")
                
                val redisUri = uriBuilder.build()
                client = RedisClient.create(redisUri)
                
                // Apply SSL options to client
                client?.setOptions(io.lettuce.core.ClientOptions.builder()
                    .sslOptions(sslOptions)
                    .build())
            } else {
                Timber.w("Using basic SSL without verification")
                uriBuilder.withSsl(config.useTls)
                if (config.useTls) {
                    uriBuilder.withVerifyPeer(false)
                }
                
                val redisUri = uriBuilder.build()
                client = RedisClient.create(redisUri)
            }
            
            // Establish connections
            connection = client?.connect()
            pubSubConnection = client?.connectPubSub()
            
            if (connection != null && pubSubConnection != null) {
                Timber.i("Successfully connected to Redis at $redisHost:${config.port}")
                true
            } else {
                Timber.e("Failed to establish Redis connection - connection is null")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error connecting to Redis server")
            false
        }
    }
    
    fun subscribeToHooks(): Flow<RedisHookData> = callbackFlow {
        try {
            val pubSub = pubSubConnection ?: throw IllegalStateException("Not connected to Redis")
            
            pubSub.addListener(object : RedisPubSubListener<String, String> {
                override fun message(channel: String, message: String) {
                    try {
                        if (channel == config.channel) {
                            Timber.d("Received Redis message: ${message.take(100)}...")
                            val hookData = json.decodeFromString<RedisHookData>(message)
                            trySend(hookData).isSuccess
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse hook data: ${message.take(100)}...")
                    }
                }
                
                override fun message(pattern: String, channel: String, message: String) {
                    // Not used for exact channel subscription
                }
                
                override fun subscribed(channel: String, count: Long) {
                    Timber.d("Subscribed to Redis channel: $channel")
                }
                
                override fun unsubscribed(channel: String, count: Long) {
                    Timber.d("Unsubscribed from Redis channel: $channel")
                }
                
                override fun psubscribed(pattern: String, count: Long) {}
                override fun punsubscribed(pattern: String, count: Long) {}
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
    
    private fun loadCertificatesFromAssets(): SslOptions? {
        try {
            // Load CA certificate from assets
            val caCertStream = context.assets.open(CA_CERT_PATH)
            val clientCertStream = context.assets.open(CLIENT_CERT_PATH)
            val clientKeyStream = context.assets.open(CLIENT_KEY_PATH)
            
            // Create trust store with CA certificate
            val caCert = loadCertificate(caCertStream)
            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            trustStore.load(null)
            trustStore.setCertificateEntry("ca-cert", caCert)
            
            // Initialize trust manager
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustStore)
            
            // Load client certificate
            val clientCert = loadCertificate(clientCertStream)
            
            // Load client private key
            val clientKey = loadPrivateKey(clientKeyStream)
            
            // Create key store with client certificate and key
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null)
            keyStore.setCertificateEntry("client-cert", clientCert)
            keyStore.setKeyEntry("client-key", clientKey, "".toCharArray(), arrayOf(clientCert))
            
            // Initialize key manager
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "".toCharArray())
            
            // Create SSL context
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
            
            // Create SSL options for Lettuce
            return SslOptions.builder()
                .jdkSslProvider()
                .trustManager(trustManagerFactory)
                .keyManager(keyManagerFactory)
                .protocols("TLSv1.2", "TLSv1.3")
                .build()
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading certificates from assets")
            return null
        }
    }
    
    private fun loadCertificate(inputStream: InputStream): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        return certificateFactory.generateCertificate(inputStream) as X509Certificate
    }
    
    private fun loadPrivateKey(inputStream: InputStream): java.security.PrivateKey {
        val keyContent = inputStream.bufferedReader().use { it.readText() }
        
        try {
            val privateKeyPEMFormats = listOf(
                Pair("-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----"),
                Pair("-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----"),
                Pair("-----BEGIN EC PRIVATE KEY-----", "-----END EC PRIVATE KEY-----")
            )
            
            for ((startMarker, endMarker) in privateKeyPEMFormats) {
                val startPos = keyContent.indexOf(startMarker)
                val endPos = keyContent.indexOf(endMarker)
                
                if (startPos >= 0 && endPos > startPos) {
                    val base64Content = keyContent.substring(startPos + startMarker.length, endPos)
                        .replace("\\s+".toRegex(), "")
                    
                    try {
                        val keyBytes = Base64.getDecoder().decode(base64Content)
                        val keyFactory = KeyFactory.getInstance("RSA")
                        val keySpec = PKCS8EncodedKeySpec(keyBytes)
                        return keyFactory.generatePrivate(keySpec)
                    } catch (e: IllegalArgumentException) {
                        Timber.e(e, "Base64 decode failed for format: $startMarker")
                    }
                }
            }
            
            throw IllegalArgumentException("Private key file does not match any supported PEM format")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load private key")
            throw e
        }
    }
}