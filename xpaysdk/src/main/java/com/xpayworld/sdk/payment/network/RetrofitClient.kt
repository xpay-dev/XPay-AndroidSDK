package com.xpayworld.sdk.payment.network

import com.xpayworld.sdk.payment.BuildConfig
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class RetrofitClient
{
    fun getRetrofit(): Retrofit
    {

//================================================================================================================//
//========================================== connection without SSL ==============================================//
//================================================================================================================//
//        val certPinner: CertificatePinner = CertificatePinner.Builder()
//                .add(ApiConstants.API_HOST,
//                        "sha256/4hw5tz+scE+TW+mlai5YipDfFWn1dqvfLG+nU7tq1V8=")
//                .build()

        val interceptor = HttpLoggingInterceptor()
//        interceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
//                .certificatePinner(certPinner)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(interceptor).build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())).client(okHttpClient)
            .build()
//================================================================================================================//
//============================================ connection with SSL ===============================================//
//================================================================================================================//
        // https://gist.github.com/maiconhellmann/c61a533eca6d41880fd2b3f8459c07f7
        // https://stackoverflow.com/questions/37686625/disable-ssl-certificate-check-in-retrofit-library

//        var okHttpClient:OkHttpClient;
//        val interceptor = HttpLoggingInterceptor()
//        interceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
//
//        try {
//            // Create a trust manager that does not validate certificate chains
//            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
//                @Throws(CertificateException::class)
//                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
//                }
//
//                @Throws(CertificateException::class)
//                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
//                }
//
//                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
//                    return arrayOf()
//                }
//            })
//
//            // Install the all-trusting trust manager
//            val sslContext = SSLContext.getInstance("SSL")
//            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
//            // Create an ssl socket factory with our all-trusting manager
//            val sslSocketFactory = sslContext.socketFactory
//
//            val hostnameVerifier = HostnameVerifier { _, session ->
//                //                HttpsURLConnection.getDefaultHostnameVerifier().run {
////                    verify(ApiConstants.API_HOST, session)
////                }
//                true
//            }
//
//            okHttpClient = OkHttpClient.Builder()
//                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//                .hostnameVerifier(hostnameVerifier)
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .addInterceptor(interceptor)
//                .build()
//
//
//        } catch (e: Exception) {
//            throw RuntimeException(e)
//        }
//
//        return Retrofit.Builder()
//            .baseUrl(BuildConfig.API_HOST)
//            .addConverterFactory(GsonConverterFactory.create())
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())).client(okHttpClient)
//            .build()
    }
}