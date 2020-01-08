package com.github.adamantcheese.chan.report

import com.github.adamantcheese.chan.BuildConfig
import com.github.adamantcheese.chan.Chan.inject
import com.github.adamantcheese.chan.core.base.BasePresenter
import com.github.adamantcheese.chan.core.base.MResult
import com.github.adamantcheese.chan.utils.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.reactivex.Single
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

class ReportActivityPresenter : BasePresenter<ReportActivity>() {

    @Inject
    lateinit var okHttpClient: OkHttpClient
    @Inject
    lateinit var gson: Gson

    override fun onCreate(view: ReportActivity) {
        super.onCreate(view)

        inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun sendReport(title: String, description: String, logs: String?): Single<MResult<Boolean>> {
        require(title.isNotEmpty())
        require(description.isNotEmpty())
        require(title.length <= MAX_TITLE_LENGTH) {
            "title is too long ${title.length}"
        }
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "description is too long ${description.length}"
        }
        logs?.let {
            require(it.length <= MAX_LOGS_LENGTH) {
                "logs are too long"
            }
        }

        val reportUrl = "${BuildConfig.DEV_API_ENDPOINT}/report"
        val json = gson.toJson(
                ReportRequest(
                        BuildConfig.FLAVOR,
                        BuildConfig.VERSION_NAME,
                        title,
                        description,
                        logs
                )
        )
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
                .url(reportUrl)
                .post(requestBody)
                .build()

        return Single.create<MResult<Boolean>> { emitter ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.onSuccess(MResult.error(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Logger.e(TAG, "Response is not successful, status = ${response.code}")
                        emitter.onSuccess(MResult.value(false))
                        return
                    }

                    emitter.onSuccess(MResult.value(true))
                }
            })
        }
    }

    data class ReportRequest(
            @SerializedName("build_flavor")
            val buildFlavor: String,
            @SerializedName("version_name")
            val versionName: String,
            @SerializedName("report_title")
            val title: String,
            @SerializedName("report_description")
            val description: String,
            @SerializedName("report_logs")
            val logs: String?
    )

    companion object {
        private const val TAG = "ReportActivityPresenter"

        const val MAX_TITLE_LENGTH = 512
        const val MAX_DESCRIPTION_LENGTH = 8192
        const val MAX_LOGS_LENGTH = 65535
    }
}