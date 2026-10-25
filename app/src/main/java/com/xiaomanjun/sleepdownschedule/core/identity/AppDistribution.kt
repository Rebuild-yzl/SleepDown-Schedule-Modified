package com.xiaomanjun.sleepdownschedule.core.identity

import com.xiaomanjun.sleepdownschedule.BuildConfig

enum class DistributionChannel {
    GITHUB,
    STORE
}

object AppDistribution {
    val channel: DistributionChannel = when (BuildConfig.DISTRIBUTION_CHANNEL) {
        "github" -> DistributionChannel.GITHUB
        "store" -> DistributionChannel.STORE
        else -> error("Unknown SleepDown distribution channel: ${BuildConfig.DISTRIBUTION_CHANNEL}")
    }

    // 修改版使用独立 applicationId，官方 Release 里的 APK 无法覆盖安装，自更新只会把官方版
    // 作为另一个应用装到设备上，因此只允许官方身份（含官方 debug 构建）发起自更新。
    private val isOfficialIdentity: Boolean
        get() = BuildConfig.APPLICATION_ID == AppIdentity.PRODUCTION_PACKAGE_NAME ||
            BuildConfig.APPLICATION_ID == "${AppIdentity.PRODUCTION_PACKAGE_NAME}.debug"

    val supportsSelfUpdate: Boolean
        get() = channel == DistributionChannel.GITHUB && isOfficialIdentity
}
