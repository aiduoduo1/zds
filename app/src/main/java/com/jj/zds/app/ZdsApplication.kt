package com.jj.zds.app

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.jj.core.JiangCore
import com.jj.core.config.JiangConfig
import com.jj.common.exception.JiangException
import com.jj.network.JiangNetwork
import com.jj.network.config.JiangNetworkConfig
import com.jj.network.config.JiangNetworkLogLevel
import com.jj.network.header.JiangNetworkHeaders
import com.jj.storage.JiangStorage
import com.jj.storage.config.JiangStorageConfig
import com.jj.ui.toast.JToast
import com.jj.zds.BuildConfig
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.feature.login.ui.LoginActivity
import com.jj.zds.feature.message.ZdsMessageSseService
import okhttp3.Interceptor
import java.util.concurrent.TimeUnit

class ZdsApplication : Application() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var lastUnauthorizedAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        JiangCore.init(
            application = this,
            config = JiangConfig(
                debug = BuildConfig.DEBUG,
                appName = "zds",
                baseUrl = BASE_URL,
            ),
        )
        JiangStorage.init(
            application = this,
            config = JiangStorageConfig(
                defaultKvId = "zds_default",
                debug = BuildConfig.DEBUG,
            ),
        )
        JiangNetwork.init(
            config = JiangNetworkConfig(
                baseUrl = BASE_URL,
                dynamicHeadersProvider = {
                    mapOf(
                        "MES-UP-TOKEN" to JiangStorage.kv.getString(
                            ZdsStorageKeys.MES_UP_TOKEN,
                            "",
                        ).orEmpty(),
                        "User-Agent" to "zds-android",
                    )
                },
                debug = BuildConfig.DEBUG,
                interceptors = createNetworkInterceptors(),
                logLevel = if (BuildConfig.DEBUG) {
                    JiangNetworkLogLevel.BODY
                } else {
                    JiangNetworkLogLevel.BODY
                },
                unauthorizedHandler = ::handleUnauthorized,
            ),
        )
        JToast.init(this)
    }

    private fun handleUnauthorized(exception: JiangException) {
        val now = System.currentTimeMillis()
        if (now - lastUnauthorizedAt < UNAUTHORIZED_REDIRECT_INTERVAL_MS) {
            return
        }
        lastUnauthorizedAt = now
        mainHandler.post {
            clearLoginCache()
            ZdsMessageSseService.stop(this)
            JToast.show(exception.message.ifBlank { "登录已失效，请重新登录" })
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
    }

    private fun clearLoginCache() {
        JiangStorage.kv.remove(ZdsStorageKeys.MES_UP_TOKEN)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_ID)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_NAME)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_SKILLS)
    }

    private fun createNetworkInterceptors(): List<Interceptor> {
//        if (!BuildConfig.DEBUG) {
//            return emptyList()
//        }
        return listOf(
            Interceptor { chain ->
                val request = chain.request()
                if (request.url.encodedPath == OSCILLOSCOPE_SCREENSHOT_PATH) {
                    chain
                        .withConnectTimeout(OSCILLOSCOPE_SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .withReadTimeout(OSCILLOSCOPE_SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .withWriteTimeout(OSCILLOSCOPE_SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .proceed(request)
                } else {
                    chain.proceed(request)
                }
            },
            ChuckerInterceptor.Builder(this)
                .maxContentLength(250_000L)
                .redactHeaders(
                    JiangNetworkHeaders.MES_UP_TOKEN,
                    JiangNetworkHeaders.AUTHORIZATION,
                    "Authorization",
                    "Cookie",
                    "token",
                    "password",
                    "pwd",
                )
                .alwaysReadResponseBody(true)
                .build(),
        )
    }

    companion object {
        val BASE_URL = if (BuildConfig.DEBUG) {
            "http://192.168.1.216:81/"
             //"http://1.94.220.45/"
        } else {
            "http://1.94.220.45/"
        }

        fun buildFileDownloadUrl(fileId: String, token: String): String {
            return "${BASE_URL.trimEnd('/')}/task/taskFile/download/$fileId?token=$token"
        }

        const val UNAUTHORIZED_REDIRECT_INTERVAL_MS = 1_500L
        const val OSCILLOSCOPE_SCREENSHOT_PATH = "/s/micsig/screenAndGet"
        const val OSCILLOSCOPE_SCREENSHOT_TIMEOUT_SECONDS = 30
    }
}
