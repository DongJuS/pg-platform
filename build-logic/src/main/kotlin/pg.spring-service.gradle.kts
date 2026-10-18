plugins { id("pg.java-library"); id("org.springframework.boot") }
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val gateway = project.name == "api-gateway"
val hexagonal = !gateway && !project.name.startsWith("mock-")
val packageName = "com.shinhan.pg." + project.name.replace("-", "")
val webPackage = packageName + if (hexagonal) ".adapter.in.web" else ".api"
val generated = layout.buildDirectory.dir("generated/platform")
val generatePlatform by tasks.registering {
    val templates = rootProject.file("build-logic/src/main/templates")
    inputs.dir(templates)
    inputs.property("packageName", packageName)
    outputs.dir(generated)
    doLast {
        val target = generated.get().asFile
        if (!gateway) {
            templates.resolve("web").listFiles()?.filter { it.extension == "java" }?.forEach { source ->
                val output = target.resolve("java/" + webPackage.replace('.', '/') + "/" + source.name)
                output.parentFile.mkdirs()
                output.writeText(source.readText().replace("@@WEB_PACKAGE@@", webPackage))
            }
        }
        if (hexagonal) {
            val output = target.resolve("java/" + packageName.replace('.', '/') + "/adapter/out/kafka/KafkaHealthIndicator.java")
            output.parentFile.mkdirs()
            output.writeText(templates.resolve("KafkaHealthIndicator.java.template").readText().replace("@@SERVICE_PACKAGE@@", packageName))
        }
    }
}
sourceSets.main { java.srcDir(generated.map { it.dir("java") }); resources.srcDir(rootProject.file("build-logic/src/main/shared-resources")) }
tasks.named("compileJava") { dependsOn(generatePlatform) }
dependencies {
    "implementation"(catalog.findLibrary("boot-actuator").get())
    "implementation"(catalog.findLibrary("boot-validation").get())
    "implementation"(catalog.findLibrary("prometheus").get())
    "implementation"(catalog.findLibrary("tracing-otel").get())
    "implementation"(catalog.findLibrary("otel-exporter").get())
    "implementation"(project(":common-error"))
    "implementation"(catalog.findLibrary(if (gateway) "openapi-webflux" else "openapi-mvc").get())
    if (!gateway) "implementation"(catalog.findLibrary("boot-web").get())
    "testImplementation"(catalog.findLibrary("boot-test").get())
    if (!hexagonal) "testImplementation"(project(":arch-rules"))
}
