plugins { id("pg.java-library") }

dependencies {
    // WO-A3: 이벤트 payload를 Jackson JSON(byte[])으로 직렬화 — OutboxRecord.payload 계약에 맞춤.
    // 버전은 pg.java-library가 적용한 Spring Boot BOM이 관리한다.
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
