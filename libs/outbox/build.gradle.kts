plugins { id("pg.java-library") }
dependencies {
    implementation(libs.boot.autoconfigure)
    implementation(libs.spring.context)
    implementation(libs.spring.kafka)
    implementation(libs.boot.jdbc)
    implementation("io.micrometer:micrometer-core")

    testImplementation(platform(libs.tc.bom))
    testImplementation(libs.tc.junit)
    testImplementation(libs.tc.postgres)
    testImplementation(libs.postgres)
    testImplementation(libs.boot.test)
    testImplementation("io.micrometer:micrometer-registry-prometheus")
}
