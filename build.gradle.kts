import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.apollographql.apollo3") version "3.8.2"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("branch.protection.tool.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

apollo {
    service("service") {
        packageName.set("com.branch.protection.tool")
    }
    generateKotlinModels.set(true)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}