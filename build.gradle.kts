plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    implementation("io.fabric8:kubernetes-client:7.3.1")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        if (project.hasProperty("runIde_ideDir")) {
            local("${project.extra["runIde_ideDir"]}")
        } else {
            intellijIdea("2025.2.6.2")
        }

        // Declare dependency on the bundled Terminal plugin
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

configurations.all {
    exclude("org.slf4j")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    groups.empty()
    // Plugin versions use two segments (e.g. 1.18) rather than SemVer
    headerParserRegex = """(\d+(?:\.\d+)+)""".toRegex()
    repositoryUrl = "https://github.com/sandipchitale/kubemmander"
}
