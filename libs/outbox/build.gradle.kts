plugins { id("pg.java-library") }
dependencies {
    implementation(libs.boot.autoconfigure)
    implementation(libs.spring.context)
    implementation(libs.spring.kafka)
}
