plugins {
    java
}

version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit", "junit", "4.12")
    compile("io.vertx:vertx-web:3.6.0-SNAPSHOT")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
