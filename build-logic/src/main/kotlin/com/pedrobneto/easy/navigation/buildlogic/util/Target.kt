package com.pedrobneto.easy.navigation.buildlogic.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Target {
    @SerialName("ANDROID")
    Android,
    @SerialName("JVM")
    Jvm,
    @SerialName("JS")
    Js,
    @SerialName("WASM_JS")
    WasmJs,
    @SerialName("WASM_WASI")
    WasmWasi,
    @SerialName("ANDROID_NATIVE_ARM32")
    AndroidNativeArm32,
    @SerialName("ANDROID_NATIVE_ARM64")
    AndroidNativeArm64,
    @SerialName("ANDROID_NATIVE_X86")
    AndroidNativeX86,
    @SerialName("ANDROID_NATIVE_X64")
    AndroidNativeX64,
    @SerialName("IOS_ARM64")
    IosArm64,
    @SerialName("IOS_X64")
    IosX64,
    @SerialName("IOS_SIMULATOR_ARM64")
    IosSimulatorArm64,
    @SerialName("WATCHOS_ARM32")
    WatchosArm32,
    @SerialName("WATCHOS_ARM64")
    WatchosArm64,
    @SerialName("WATCHOS_X64")
    WatchosX64,
    @SerialName("WATCHOS_SIMULATOR_ARM64")
    WatchosSimulatorArm64,
    @SerialName("TVOS_ARM64")
    TvosArm64,
    @SerialName("TVOS_X64")
    TvosX64,
    @SerialName("TVOS_SIMULATOR_ARM64")
    TvosSimulatorArm64,
    @SerialName("MACOS_X64")
    MacosX64,
    @SerialName("MACOS_ARM64")
    MacosArm64,
    @SerialName("LINUX_X64")
    LinuxX64,
    @SerialName("LINUX_ARM64")
    LinuxArm64,
    @SerialName("MINGW_X64")
    MingwX64
}
