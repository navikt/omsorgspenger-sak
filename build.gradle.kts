import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitJupiterVersion = "5.7.2"
val k9rapidVersion = "1.f20388f"
val ktorVersion = "1.5.4"
val dusseldorfKtorVersion = "1.5.4.ae44e47"
val jsonassertVersion = "1.5.0"
val flywayVersion = "7.9.1"
val hikariVersion = "4.0.3"
val kotliqueryVersion = "1.3.1"
val postgresVersion = "42.2.20"
val embeddedPostgres = "1.2.10"


val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("io.zonky.test:embedded-postgres:$embeddedPostgres")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion") {
        exclude(group = "com.github.jknack")
    }
    testImplementation ("org.skyscreamer:jsonassert:$jsonassertVersion")
}

repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    maven { url = uri("https://jitpack.io") }
    mavenLocal()
    mavenCentral()
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "15"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "15"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClass
                )
            )
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.0.2"
    }

}
