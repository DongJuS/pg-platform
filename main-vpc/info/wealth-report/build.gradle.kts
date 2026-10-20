plugins { id("pg.hexagonal-service") }

dependencies {
    implementation("org.apache.pdfbox:pdfbox:3.0.8")
    testImplementation(platform(libs.tc.bom))
    testImplementation(libs.tc.junit)
    testImplementation(libs.tc.postgres)
}
