import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("plugin-desktop-application")
}

kotlin {
    sourceSets.jvmMain.dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.toolkit.arch.lumber)
        implementation(projects.sample.app)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pedrobneto.navigation.desktop"
            packageVersion = "1.0.0"
        }
    }
}
