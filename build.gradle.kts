plugins {
    `java`
    id("com.github.ben-manes.versions") version "0.43.0"
}

group = "io.github.osobolev"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("test")
        resources.srcDir("testResources")
    }
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.release.set(8)
    }
}

dependencies {
    implementation("org.json:json:20220924")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1.3")
    implementation("io.javalin:javalin:4.6.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("org.freemarker:freemarker:2.3.31")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.4")
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.map { conf -> conf.files.map { f -> f.name }.sorted().joinToString(" ") },
            "Main-Class" to "arshin.WebApp"
        )
    }
}

tasks.register("distr", Copy::class) {
    from(configurations.runtimeClasspath)
    from(tasks.jar)
    from("config")
    from(".") {
        include("web/**")
    }
    into("$rootDir/distr/arshin")
}
