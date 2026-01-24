plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.damienlove.qaverifytrack"
version = "0.1.1"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        local("C:/Program Files/Android/Android Studio")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.damienlove.qaverifytrack"
        name = "QA Verify & Track"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }
}
