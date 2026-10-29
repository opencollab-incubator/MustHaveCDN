plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

group = "dev.opencollab.mhcdn"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("org.geysermc.geyser:api:2.11.2-SNAPSHOT")
    val configurateVersion = "4.3.0-GeyserMC-20261023.231928-1";
    annotationProcessor("org.spongepowered:configurate-extra-interface-ap:${configurateVersion}")
    implementation("org.spongepowered:configurate-yaml:${configurateVersion}")
    implementation("org.spongepowered:configurate-core:${configurateVersion}")
    implementation("org.spongepowered:configurate-extra-interface:${configurateVersion}")

    // Use the JDK HTTP client instead of Netty/Apache so we don't clash with Geyser's Netty
    implementation("software.amazon.awssdk:s3:2.56.0") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
        exclude(group = "software.amazon.awssdk", module = "apache-client")
        exclude(group = "software.amazon.awssdk", module = "apache5-client")
    }
    implementation("software.amazon.awssdk:url-connection-client:2.56.0")
    // The SDK's slf4j is relocated so it can't find the platform's logger; bind it to a no-op to avoid startup warnings
    implementation("org.slf4j:slf4j-nop:1.7.36")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("extension.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier = ""
    mergeServiceFiles()
    relocate("org.spongepowered.configurate", "dev.opencollab.mhcdn.shadow.configurate")
    relocate("org.yaml.snakeyaml", "dev.opencollab.mhcdn.shadow.snakeyaml")
    relocate("software.amazon", "dev.opencollab.mhcdn.shadow.amazon")
    relocate("org.reactivestreams", "dev.opencollab.mhcdn.shadow.reactivestreams")
    relocate("org.slf4j", "dev.opencollab.mhcdn.shadow.slf4j")
    relocate("io.leangen.geantyref", "dev.opencollab.mhcdn.shadow.geantyref")
    relocate("net.kyori.option", "dev.opencollab.mhcdn.shadow.kyori.option")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}