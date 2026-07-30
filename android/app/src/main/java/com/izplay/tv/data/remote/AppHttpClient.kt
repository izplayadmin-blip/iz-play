package com.izplay.tv.data.remote

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object AppDns {
    const val SYSTEM = "Automático (sistema)"
    const val CLOUDFLARE = "Cloudflare (1.1.1.1)"
    const val GOOGLE = "Google (8.8.8.8)"
    const val QUAD9 = "Quad9 (9.9.9.9)"
    val options = listOf(SYSTEM, CLOUDFLARE, GOOGLE, QUAD9)

    @Volatile
    var provider: String = SYSTEM

    private val bootstrapClient by lazy { OkHttpClient.Builder().build() }
    private val cloudflare by lazy {
        doh("https://cloudflare-dns.com/dns-query", "1.1.1.1", "1.0.0.1")
    }
    private val google by lazy {
        doh("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4")
    }
    private val quad9 by lazy {
        doh("https://dns.quad9.net/dns-query", "9.9.9.9", "149.112.112.112")
    }

    val dynamic: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            // Android 7 TV boxes commonly fail the TLS/HTTP2 handshake used by
            // public DoH resolvers. The system resolver remains reliable after
            // network DNS provisioning, so avoid false UnknownHost failures.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                return Dns.SYSTEM.lookup(hostname)
            }
            return when (provider) {
                CLOUDFLARE -> cloudflare.lookup(hostname)
                GOOGLE -> google.lookup(hostname)
                QUAD9 -> quad9.lookup(hostname)
                else -> Dns.SYSTEM.lookup(hostname)
            }
        }
        }

    private fun doh(url: String, vararg bootstrapAddresses: String): Dns =
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(url.toHttpUrl())
            .bootstrapDnsHosts(bootstrapAddresses.map(InetAddress::getByName))
            .build()
}

object AppHttpClient {
    val defaultHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android TV; IZ Play) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
        "Connection" to "keep-alive"
    )

    fun create(): OkHttpClient = OkHttpClient.Builder()
        .dns(AppDns.dynamic)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            defaultHeaders.forEach { (name, value) -> builder.header(name, value) }
            chain.proceed(builder.build())
        }
        .build()

}

fun Request.Builder.withPlayerHeaders(): Request.Builder = apply {
    AppHttpClient.defaultHeaders.forEach { (name, value) -> header(name, value) }
}
