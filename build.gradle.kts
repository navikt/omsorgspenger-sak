val junitJupiterVersion = "5.6.2"
val jsonassertVersion = "1.5.0"
val k9rapidVersion = "1.8748f39"
val flywayVersion = "6.5.0"
val hikariVersion = "3.4.5"
val kotliqueryVersion = "1.3.1"
val postgresVersion = "42.2.16"
val embeddedPostgres = "0.13.3"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.4.10"
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("com.opentable.components:otj-pg-embedded:$embeddedPostgres")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
    jcenter()
}

tasks {

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<Wrapper> {
        gradleVersion = "6.6.1"
    }

    withType<Jar> {
        manifest.attributes["Main-Class"] = mainClass
    }

}
