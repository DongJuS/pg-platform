plugins { id("pg.spring-service") }

dependencies {
    implementation(platform(libs.cloud.bom))
    implementation(libs.gateway)
    implementation(libs.jwt.resource.server)
    implementation(libs.boot.redis)
}

dependencies { implementation(libs.context.propagation) }
