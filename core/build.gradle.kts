import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
}

// Pure Kotlin/JVM: the logic is tested without a phone and android/ consumes it as a composite build.
group = "app.friendlypear"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}

// Evaluation runner for the rules alone (eval/); not part of the library.
tasks.register<JavaExec>("evalShield") {
    group = "verification"
    description = "Runs the shield's rules over an evaluation TSV"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("app.friendlypear.core.eval.EvalShieldKt")
}
