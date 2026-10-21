plugins { id("pg.hexagonal-service") }

dependencies { implementation(project(":swift-messages")) }

dependencies { implementation(project(":aml-contracts")) }
