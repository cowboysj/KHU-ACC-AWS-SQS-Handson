// core는 공통 라이브러리 모듈이므로 최소한의 의존성만 포함
dependencies {
    // Spring Boot BOM import
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))

    // Jackson for JSON serialization
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}