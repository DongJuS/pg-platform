plugins { id("pg.java-library"); `java-test-fixtures` }
dependencies {
    api(libs.archunit)
    testFixturesImplementation(platform(libs.boot.bom))
    testFixturesImplementation(platform(libs.tc.bom))
    testFixturesApi(libs.boot.test)
    testFixturesApi(libs.boot.web)
    testFixturesApi(libs.boot.jdbc)
    testFixturesApi(libs.boot.redis)
    testFixturesApi(libs.tc.junit)
    testFixturesApi(libs.tc.postgres)
    testFixturesApi(libs.tc.kafka)
    testFixturesApi(libs.tc.core)
}
