plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.github.zhenyuan"
version = "1.0.1"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

intellij {
    version.set("2023.3")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf("Git4Idea"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("")
    }

    buildSearchableOptions {
        enabled = false
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}