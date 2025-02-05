import org.jetbrains.changelog.Changelog
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("plugin.serialization") version "2.0.0"

    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.2.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    updateSinceUntilBuild.set(false)

    plugins.set(
        properties("platformPlugins").split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
    )
}

changelog {
//    version.set(properties("pluginVersion"))
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

tasks {
    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = it
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        // untilBuild.set(properties("pluginUntilBuild"))

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion")) ?: getUnreleased()
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    buildPlugin {
        exclude { "coroutines" in it.name }
    }

    buildSearchableOptions {
        enabled = false
    }

    prepareSandbox {
        exclude { "coroutines" in it.name }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
//    implementation("com.aallam.openai:openai-client:3.7.2") {
//        exclude(group = "org.slf4j", module = "slf4j-api")
//        // Prevents java.lang.LinkageError: java.lang.LinkageError: loader constraint violation:when resolving method 'long kotlin.time.Duration.toLong-impl(long, kotlin.time.DurationUnit)'
//        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
//    }
//    implementation("io.ktor:ktor-client-cio:2.3.11") {
//        exclude(group = "org.slf4j", module = "slf4j-api")
//        // Prevents java.lang.LinkageError: java.lang.LinkageError: loader constraint violation: when resolving method 'long kotlin.time.Duration.toLong-impl(long, kotlin.time.DurationUnit)'
//        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
//    }
//
//    implementation("com.knuddels:jtokkit:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // langchain4j integrations
    implementation("dev.langchain4j:langchain4j-open-ai:0.32.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.32.0")
    implementation("dev.langchain4j:langchain4j-qianfan:0.32.0") // The Baidu Qianfan Large Model Platform, including the ERNIE series, can be accessed at https://docs.langchain4j.dev/integrations/language-models/qianfan/.
    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini:0.32.0")
//    implementation("dev.langchain4j:langchain4j-hugging-face:0.28.0")
//    implementation("dev.langchain4j:langchain4j-milvus:0.28.0")
//    implementation("dev.langchain4j:langchain4j-local-ai:0.28.0")

    // tests
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
