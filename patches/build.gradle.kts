group = "app.froggo"

patches {
    // TODO: Update this section with your project details.
    about {
        name = "Froggo Morphe Patches"
        description = "Morphe patches for Facebook 573.0.0.37.74"
        source = "https://github.com/SapitoSucio/FroggoMorphePatches"
        author = "SapitoSucio"
        contact = "https://github.com/SapitoSucio"
        website = "https://github.com/SapitoSucio/FroggoMorphePatches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
