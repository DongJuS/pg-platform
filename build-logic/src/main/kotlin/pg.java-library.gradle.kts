plugins { `java-library` }
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
java { toolchain { languageVersion.set(JavaLanguageVersion.of(catalog.findVersion("java").get().requiredVersion)) } }
dependencies {
    "implementation"(platform(catalog.findLibrary("boot-bom").get()))
    "testImplementation"(catalog.findLibrary("junit").get())
    "testRuntimeOnly"(catalog.findLibrary("junit-launcher").get())
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.compilerArgs.add("-parameters") }
tasks.withType<Test>().configureEach { useJUnitPlatform(); testLogging { events("failed", "skipped") } }

tasks.withType<Test>().configureEach { systemProperty("integration", providers.gradleProperty("integration").getOrElse("false")) }
