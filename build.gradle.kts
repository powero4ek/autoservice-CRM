plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "com.autoservice"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // TornadoFX
    implementation("no.tornado:tornadofx:1.7.20")

    // JavaFX для Windows (JDK 23 не содержит JavaFX)
    val javaFxVersion = "17.0.10"
    implementation("org.openjfx:javafx-base:$javaFxVersion:win")
    implementation("org.openjfx:javafx-controls:$javaFxVersion:win")
    implementation("org.openjfx:javafx-graphics:$javaFxVersion:win")
    implementation("org.openjfx:javafx-fxml:$javaFxVersion:win")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.50.1")

    // PostgreSQL + пул
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Flyway
    implementation("org.flywaydb:flyway-core:10.15.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.15.0")

    // BCrypt
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.springframework.security:spring-security-crypto:6.2.4")
    // Логирование
    implementation("org.slf4j:slf4j-simple:2.0.12")
    implementation("commons-logging:commons-logging:1.3.1")
    implementation("org.springframework.security:spring-security-crypto:6.2.4")
    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("com.autoservice.crm.app.MainAppKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.test {
    useJUnitPlatform()
}