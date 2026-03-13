plugins {
    `java-library`
}

description = "Shared Medicare Advantage Star Ratings calculation models"

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
