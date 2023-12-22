import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.10.1"
val k9rapidVersion = "1.20231002100147-90c2022"
val ktorVersion = "2.3.7"
val dusseldorfKtorVersion = "4.1.4"
val jsonassertVersion = "1.5.1"

// Database
val flywayVersion = "10.4.0"
val hikariVersion = "5.1.0"
val kotliqueryVersion = "1.9.0"
val postgresVersion = "42.7.1"
val embeddedPostgres = "2.0.6"
val embeddedPostgresBinaries = "12.9.0"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.sonarqube") version "4.4.1.3373"
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("io.zonky.test:embedded-postgres:$embeddedPostgres")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:$embeddedPostgresBinaries"))
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "x-access-token"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        finalizedBy(jacocoTestReport)
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
        gradleVersion = "8.5"
    }

    withType<JacocoReport> {
        dependsOn(test)
        reports {
            xml.required.set(true)
            csv.required.set(false)
        }
    }

}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_omsorgspenger-sak")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.sourceEncoding", "UTF-8")
    }
}
