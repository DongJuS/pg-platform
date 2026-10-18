plugins { base }
allprojects { group = "com.shinhan.pg"; version = "0.1.0-SNAPSHOT" }
tasks.named("build") { dependsOn(subprojects.map { "${it.path}:build" }) }
