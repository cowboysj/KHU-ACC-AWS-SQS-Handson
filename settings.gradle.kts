plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ACC-SQS"

include("core")
include("order-service")
include("delivery-service")