plugins {
    `java`
    id("com.github.ben-manes.versions") version "0.53.0"
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
    create("manual") {
        java.srcDir("test")
        resources.srcDir("testResources")
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.release.set(11)
}

dependencies {
    implementation("io.github.osobolev:small-json:1.4")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6")
    implementation("io.javalin:javalin:6.7.0") {
        exclude(group = "org.eclipse.jetty.websocket", module = "websocket-jetty-server")
    }
    implementation("io.javalin:javalin-rendering:6.7.0")
    implementation("org.freemarker:freemarker:2.3.34")
    implementation("org.eclipse.angus:angus-mail:2.0.5")
    runtimeOnly("org.eclipse.jetty:jetty-servlet:11.0.26")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.32")
}

configurations["manualImplementation"].extendsFrom(configurations["implementation"])
configurations["manualRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])
configurations["manualCompileOnly"].extendsFrom(configurations["compileOnly"])

dependencies {
    "manualImplementation"(sourceSets["main"].output)
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.map { conf -> conf.files.map { f -> f.name }.sorted().joinToString(" ") },
            "Main-Class" to "arshin.WebApp"
        )
    }
}

fun getMajor(version: String, majorDepth: Int): String {
    var p = -1
    for (i in 0 until majorDepth) {
        p = version.indexOf('.', p + 1)
        if (p < 0) return version
    }
    return if (p < 0) "" else version.substring(0, p)
}

fun getMajorDepth(mod: ModuleComponentIdentifier): Int {
    if (mod.group == "io.javalin") return 1
    return 0
}

tasks.withType(com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class).configureEach {
    resolutionStrategy {
        componentSelection {
            all(Action<com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent> {
                if (candidate.version.contains("-a")) {
                    reject("Alpha version")
                } else if (candidate.version.contains("-b")) {
                    reject("Beta version")
                } else if (candidate.version.contains("-M")) {
                    reject("Milestone version")
                } else {
                    val majorDepth = getMajorDepth(candidate)
                    if (getMajor(candidate.version, majorDepth) != getMajor(currentVersion, majorDepth)) {
                        reject("Major update")
                    }
                }
            })
        }
    }
}

tasks.clean {
    delete("$rootDir/distr")
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
