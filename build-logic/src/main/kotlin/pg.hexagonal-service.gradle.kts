plugins { id("pg.spring-service") }
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    "implementation"(catalog.findLibrary("boot-jdbc").get())
    "implementation"(catalog.findLibrary("boot-redis").get())
    "implementation"(catalog.findLibrary("spring-kafka").get())
    "runtimeOnly"(catalog.findLibrary("msk-iam").get())
    "implementation"(catalog.findLibrary("flyway-core").get())
    "runtimeOnly"(catalog.findLibrary(if (project.name in setOf("account", "ledger")) "mariadb" else "postgres").get())
    "runtimeOnly"(catalog.findLibrary(if (project.name in setOf("account", "ledger")) "flyway-mysql" else "flyway-postgres").get())
    "implementation"(project(":common-money"))
    "implementation"(project(":event-contracts"))
    "implementation"(project(":outbox"))
    "testImplementation"(project(":arch-rules"))
    "testImplementation"(testFixtures(project(":arch-rules")))
}
