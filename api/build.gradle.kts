plugins {
    id("geyser.publish-conventions")
}

dependencies {
    api(libs.base.api)
}

tasks {
    withType<Checkstyle> {
        configFile = rootProject.file(".checkstyle/checkstyle-api.xml")
    }
}