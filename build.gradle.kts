plugins {
    kotlin("jvm") version "2.1.21"
}

group = "tech.robd"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.10")

    implementation("tech.robd:jcoroutines:0.1.0")
    implementation("tech.robd:abcoroutines:0.1.0")

}

tasks.test {

    systemProperty("jcoroutines.diag", "true")
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}