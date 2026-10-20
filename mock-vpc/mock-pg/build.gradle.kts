plugins { id("pg.spring-service") }
dependencies {
    implementation(libs.boot.jdbc)
    implementation(libs.flyway.core)
    implementation(project(":common-money"))
    runtimeOnly(libs.flyway.postgres)
    runtimeOnly(libs.postgres)
    testImplementation(platform(libs.tc.bom))
    testImplementation(libs.tc.junit)
    testImplementation(libs.tc.postgres)
    testImplementation(libs.postgres)
}
