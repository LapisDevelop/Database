plugins {
    id("java")
}

group = "com.lapisdev"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {

}

tasks.test {
    useJUnitPlatform()
}