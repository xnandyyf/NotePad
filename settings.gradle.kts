pluginManagement {
    repositories {
        // 国内镜像优先：无需 VPN 即可同步依赖（腾讯云聚合 Maven Central 与 Google Maven）
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") { name = "TencentMirror" }
        maven("https://maven.aliyun.com/repository/google") { name = "AliyunGoogle" }
        maven("https://maven.aliyun.com/repository/gradle-plugin") { name = "AliyunGradlePlugin" }
        maven("https://maven.aliyun.com/repository/public") { name = "AliyunPublic" }

        // 官方仓库兜底：仅当镜像缺少某个包时才会访问
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") { name = "TencentMirror" }
        maven("https://maven.aliyun.com/repository/google") { name = "AliyunGoogle" }
        maven("https://maven.aliyun.com/repository/public") { name = "AliyunPublic" }
        google()
        mavenCentral()
    }
}

rootProject.name = "NotePad"
include(":app")
