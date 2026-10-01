plugins {
    java
}

group = "dev.esslite"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    // Copia aquí la MISMA línea de purpur-api y el MISMO toolchain de Java
    // que ya te compila bien en tu proyecto de Dangerous Caves.
    compileOnly("org.purpurmc.purpur:purpur-api:26.3.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.jar {
    archiveBaseName.set("EssLite")
}
