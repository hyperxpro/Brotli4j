# Brotli4j

[![Maven Central](https://img.shields.io/maven-central/v/com.aayushatharva.brotli4j/brotli4j-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aayushatharva.brotli4j%22%20AND%20a:%22brotli4j-parent%22)

Brotli4j provides Brotli compression and decompression for Java.

## Supported Platforms:

| Module                        | Architecture |                       Tested On |
|:------------------------------|:------------:|--------------------------------:|
| Windows (Windows Server 2022) |     x64      | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| Windows 11                    |   Aarch64    |                           JDK 8 |
| Linux (CentOS 6)              |     x64      | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| Linux (Ubuntu 18.04)          |   Aarch64    | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| Linux (Ubuntu 18.04)          |    ARMv7     |         JDK 1.8, JDK 11, JDK 17 |
| Linux (Ubuntu 18.04)          |    s390x     |                 JDK 1.8, JDK 11 |
| Linux (Ubuntu 18.04)          |   ppc64le    |                 JDK 1.8, JDK 11 |
| Linux (Ubuntu 20.04)          |   RISC-v64   | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| macOS (Catalina)              |     x64      | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| macOS (Catalina)              |   Aarch64    | JDK 1.8, JDK 11, JDK 17, JDK 21 |

> [!IMPORTANT]
> Install [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist?view=msvc-170) before running this
> library on Windows

## Download

### Maven

For maven, the natives will
[import automatically by your system family and architecture](https://github.com/hyperxpro/Brotli4j/blob/main/natives/pom.xml#L38-L114).

```xml
<dependency>
    <groupId>com.aayushatharva.brotli4j</groupId>
    <artifactId>brotli4j</artifactId>
    <version>1.18.0</version>
</dependency>
```

### Gradle

For gradle, some additional configuration must be added in order to allow gradle to resolve the correct native artifacts automatically,
based on your architecture.
This also supports [shadow](https://gradleup.com/shadow/), so that the shaded jar will contain the natives for *all* architectures.

> [!NOTE]
> Do note that when creating a [distribution zip/tar](https://docs.gradle.org/current/userguide/distribution_plugin.html),
> this will cause the distribution to only have the natives for the architecture it was compiled on.
> In order to have it include all natives, some additional configuration would be necessary.
> (For example, creating a custom configuration which has the `all` operating system & architecture, similar to what is done for shadow)

There are two ways it can be added:

1. Project-wide configuration.  
   For this, you must add the rule class as well as the global rule usage & global attributes to the `settings.gradle.kts`/`settings.gradle` file.
   This rule will apply to *all* subprojects, and is best suited for working with multiple gradle subprojects.
2. Subproject-specific configuration.  
   For this, you must add the rule class as well as the rule usage & attributes to the `build.gradle.kts`/`build.gradle` file.
   This rule will only apply to the project it is added to and will not be applied to other projects.
   This is useful when, for example, you only have a single gradle subproject.

As of gradle 8.14.3, both of these methods work.
But they should also work for any version of gradle since 8.0.
They have not been tested on versions lower than that and may or may not work.

Note that this method requires the use of some internal gradle apis, which may get break at some point in the future.
If it does, please open an issue.
However, the apis seem to be somewhat stable, and if it does break then in the past it's just because they were moved to a different package.

To add the dependency, use:

```kotlin
dependencies {
    implementation("com.aayushatharva.brotli4j:brotli4j:1.18.0")
    runtimeOnly("com.aayushatharva.brotli4j:natives:1.18.0")
}
```

> [!IMPORTANT]
> Don't forget to also include the dependency metadata rule. You can see how to include the appropriate rule below based on which DSL you're using.

<details>
<summary><h4>Gradle Kotlin DSL</h4></summary>

<details>
<summary>Project-Wide Configuration</summary>

To configure the rule to be used project-wide, in addition to the below `Brotli4JRule` class,
this must also be added to the `settings.gradle.kts` file:

```kotlin
import org.gradle.nativeplatform.internal.DefaultTargetMachineFactory

gradle.beforeProject {
    val host = DefaultTargetMachineFactory(objects).host()

    // This tells gradle that we want an artifact with the correct OperatingSystemFamily and MachineArchitecture
    // Gradle will then attempt to find the artifact with the best matching value, or if one cannot be found,
    // fall back to the default (i.e. in the case of any dependency other than brotli4j)
    configurations.configureEach {
        // if the configuration is being published, don't set the attributes
        if (isCanBeConsumed)
            return@configureEach

        attributes {
            if (OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE !in keySet())
                attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, host.operatingSystemFamily)
            if (MachineArchitecture.ARCHITECTURE_ATTRIBUTE !in keySet())
                attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, host.architecture)
        }
    }

    // can be removed if not using shadow plugin
    pluginManager.withPlugin("com.gradleup.shadow") {
        val runtimeClasspath by configurations.getting

        configurations.register("shaded") {
            extendsFrom(runtimeClasspath)

            isCanBeConsumed = false
            isCanBeResolved = true
            isCanBeDeclared = true

            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))

                attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("all"))
                attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("all"))
            }
        }
    }
}

dependencyResolutionManagement {
    components {
        withModule<Brotli4JRule>("com.aayushatharva.brotli4j:natives")
    }
}
```

If you are using the shadow gradle plugin, then in your `build.gradle.kts` include:

```kotlin
dependencies {
    shaded("com.aayushatharva.brotli4j:natives:1.18.0")
}

tasks {
    shadowJar.configure {
        // shadow 8.x
        configurations = listOf(project.configurations.shaded).map { it.get() }
        // shadow 9.x
        configurations.add(project.configurations.shaded)
    }
}
```

</details>

<details>
<summary>Subproject-Specific Configuration</summary>

To configure the rule to be used in a specific subproject, in addition to the below `Brotli4JRule` class,
this must also be added to the `build.gradle.kts` file:

```kotlin
import org.gradle.nativeplatform.internal.DefaultTargetMachineFactory

val host = DefaultTargetMachineFactory(objects).host()

// This tells gradle that we want an artifact with the correct OperatingSystemFamily and MachineArchitecture
// Gradle will then attempt to find the artifact with the best matching value, or if one cannot be found,
// fall back to the default (i.e. in the case of any dependency other than brotli4j)
configurations.configureEach {
    // if the configuration is being published, don't set the attributes
    if (isCanBeConsumed)
        return@configureEach

    attributes {
        if (OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE !in keySet())
            attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, host.operatingSystemFamily)
        if (MachineArchitecture.ARCHITECTURE_ATTRIBUTE !in keySet())
            attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, host.architecture)
    }
}

dependencies {
    components {
        withModule<Brotli4JRule>("com.aayushatharva.brotli4j:natives")
    }
}
```

If you are using the shadow gradle plugin, then in your `build.gradle.kts` include:

```kotlin
val shaded by configurations.registering {
    extendsFrom(configurations.runtimeClasspath)

    isCanBeConsumed = false
    isCanBeResolved = true
    isCanBeDeclared = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))

        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("all"))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("all"))
    }
}

dependencies {
    shaded("com.aayushatharva.brotli4j:natives:1.18.0")
}

tasks {
    shadowJar.configure {
        // shadow 8.x
        configurations = listOf(shaded).map { it.get() }
        // shadow 9.x
        configurations.add(shaded)
    }
}
```

</details>

```kotlin
@CacheableRule
abstract class Brotli4JRule : ComponentMetadataRule {
    data class NativeVariant(val os: String, val arch: String, val classifier: String) {
        fun dependency(version: String) = "com.aayushatharva.brotli4j:native-${classifier}:${version}"
    }

    private val nativeVariants = listOf(
        NativeVariant(OperatingSystemFamily.WINDOWS, "aarch64", "windows-aarch64"),
        NativeVariant(OperatingSystemFamily.WINDOWS, "x86-64", "windows-x86_64"),
        NativeVariant(OperatingSystemFamily.MACOS, "x86-64", "osx-x86_64"),
        NativeVariant(OperatingSystemFamily.MACOS, "aarch64", "osx-aarch64"),
        NativeVariant(OperatingSystemFamily.LINUX, "x86-64", "linux-x86_64"),
        NativeVariant(OperatingSystemFamily.LINUX, "aarch64", "linux-aarch64"),
        NativeVariant(OperatingSystemFamily.LINUX, "arm-v7", "linux-armv7"),
        NativeVariant(OperatingSystemFamily.LINUX, "s390x", "linux-s390x"),
        NativeVariant(OperatingSystemFamily.LINUX, "riscv64", "linux-riscv64"),
        NativeVariant(OperatingSystemFamily.LINUX, "ppc64le", "linux-ppc64le"),
    )

    @get:Inject
    abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) {
        listOf("compile", "runtime").forEach { base -> addVariant(context, base) }
    }

    private fun addVariant(context: ComponentMetadataContext, base: String) {
        val version = context.details.id.version

        nativeVariants.forEach { variant ->
            context.details.addVariant("${variant.classifier}-${base}", base) {
                attributes {
                    attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(variant.os))
                    attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(variant.arch))
                }

                withDependencies {
                    add(variant.dependency(version))
                }
            }
        }

        context.details.addVariant("all-${base}", base) {
            attributes {
                attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("all"))
                attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("all"))
            }

            withDependencies {
                nativeVariants.forEach { variant ->
                    add(variant.dependency(version))
                }
            }
        }
    }
}
```

</details>

<details>
<summary><h4>Gradle Groovy DSL</h4></summary>

<details>
<summary>Project-Wide Configuration</summary>

To configure the rule to be used project-wide, in addition to the below `Brotli4JRule` class,
this must also be added to the `settings.gradle` file:

```groovy
gradle.beforeProject {
    def host = new DefaultTargetMachineFactory(objects).host()

    // This tells gradle that we want an artifact with the correct OperatingSystemFamily and MachineArchitecture
    // Gradle will then attempt to find the artifact with the best matching value, or if one cannot be found,
    // fall back to the default (i.e. in the case of any dependency other than brotli4j)
    configurations.configureEach {
        // if the configuration is being published, don't set the attributes
        if (canBeConsumed)
            return

        attributes {
            if (OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE !in keySet())
                attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, host.operatingSystemFamily)
            if (MachineArchitecture.ARCHITECTURE_ATTRIBUTE !in keySet())
                attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, host.architecture)
        }
    }

    // can be removed if not using shadow plugin
    pluginManager.withPlugin("com.gradleup.shadow") {
        configurations.register("shaded") {
            extendsFrom(configurations.runtimeClasspath)

            canBeConsumed = false
            canBeResolved = true
            canBeDeclared = true

            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, LibraryElements.JAR))

                attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(OperatingSystemFamily, "all"))
                attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(MachineArchitecture, "all"))
            }
        }
    }
}

dependencyResolutionManagement {
    components {
        withModule("com.aayushatharva.brotli4j:natives", Brotli4JRule)
    }
}
```

If you are using the shadow gradle plugin, then in your `build.gradle` include:

```kotlin
dependencies {
    shaded("com.aayushatharva.brotli4j:natives:1.18.0")
}

tasks {
    shadowJar.configure {
        configurations = [project.configurations.shaded]
    }
}
```

</details>

<details>
<summary>Subproject-Specific Configuration</summary>

To configure the rule to be used in a specific subproject, in addition to the below `Brotli4JRule` class,
this must also be added to the `build.gradle` file:

```groovy
def host = new DefaultTargetMachineFactory(objects).host()

// This tells gradle that we want an artifact with the correct OperatingSystemFamily and MachineArchitecture
// Gradle will then attempt to find the artifact with the best matching value, or if one cannot be found,
// fall back to the default (i.e. in the case of any dependency other than brotli4j)
configurations.configureEach {
    // if the configuration is being published, don't set the attributes
    if (canBeConsumed)
        return

    def isShadow = name.containsIgnoreCase("shadow")
    attributes {
        if (OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE !in keySet())
            attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, host.operatingSystemFamily)
        if (MachineArchitecture.ARCHITECTURE_ATTRIBUTE !in keySet())
            attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, host.architecture)
    }
}

dependencies {
    components {
        withModule("com.aayushatharva.brotli4j:natives", Brotli4JRule)
    }
}
```

If you are using the shadow gradle plugin, then in your `build.gradle` include:

```kotlin
configurations.register("shaded") {
    extendsFrom(configurations.runtimeClasspath)

    canBeConsumed = false
    canBeResolved = true
    canBeDeclared = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, LibraryElements.JAR))

        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(OperatingSystemFamily, "all"))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(MachineArchitecture, "all"))
    }
}

dependencies {
    shaded("com.aayushatharva.brotli4j:natives:1.18.0")
}

tasks {
    shadowJar.configure {
        configurations = [project.configurations.shaded]
    }
}
```

</details>

```groovy
@CacheableRule
abstract class Brotli4JRule implements ComponentMetadataRule {
    @Inject
    abstract ObjectFactory getObjects()

    void execute(ComponentMetadataContext context) {
        for (base in ["compile", "runtime"]) {
            addVariant(context, base)
        }
    }

    void addVariant(ComponentMetadataContext context, String base) {
        def nativeVariants = [
            [ os: OperatingSystemFamily.WINDOWS, arch: "aarch64", classifier: "windows-aarch64" ],
            [ os: OperatingSystemFamily.WINDOWS, arch: "x86-64",  classifier: "windows-x86_64"  ],
            [ os: OperatingSystemFamily.MACOS,   arch: "x86-64",  classifier: "osx-x86_64"      ],
            [ os: OperatingSystemFamily.MACOS,   arch: "aarch64", classifier: "osx-aarch64"     ],
            [ os: OperatingSystemFamily.LINUX,   arch: "x86-64",  classifier: "linux-x86_64"    ],
            [ os: OperatingSystemFamily.LINUX,   arch: "aarch64", classifier: "linux-aarch64"   ],
            [ os: OperatingSystemFamily.LINUX,   arch: "arm-v7",  classifier: "linux-armv7"     ],
            [ os: OperatingSystemFamily.LINUX,   arch: "s390x",   classifier: "linux-s390x"     ],
            [ os: OperatingSystemFamily.LINUX,   arch: "riscv64", classifier: "linux-riscv64"   ],
            [ os: OperatingSystemFamily.LINUX,   arch: "ppc64le", classifier: "linux-ppc64le"   ],
        ]

        def version = context.details.id.version

        nativeVariants.forEach { variant ->
            context.details.addVariant("${variant.classifier}-${base}", base) {
                attributes {
                    attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(OperatingSystemFamily, variant.os))
                    attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(MachineArchitecture, variant.arch))
                }

                withDependencies {
                    add("com.aayushatharva.brotli4j:native-${variant.classifier}:${version}")
                }
            }
        }

        context.details.addVariant("all-${base}", base) {
            attributes {
                attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(OperatingSystemFamily, "all"))
                attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(MachineArchitecture, "all"))
            }

            withDependencies {
                nativeVariants.forEach { variant ->
                    add("com.aayushatharva.brotli4j:native-${variant.classifier}:${version}")
                }
            }
        }
    }
}
```
</details>

## Usage

### Loading native library:


> [!WARNING]
> Call `Brotli4jLoader.ensureAvailability()` in your application once before using Brotli4j. This will load
> Brotli4j native library automatically using automatic dependency resolution.

> [!TIP]
> However, its possible to load native library manually from custom path by specifying System Property `"brotli4j.library.path"`.

### Direct API

```java
public class Example {
    public static void main(String[] args) {
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        // Compress data and get output in byte array
        byte[] compressed = Encoder.compress("Meow".getBytes());

        // Decompress data and get output in DirectDecompress
        DirectDecompress directDecompress = Decoder.decompress(compressed); // or DirectDecompress.decompress(compressed);

        if (directDecompress.getResultStatus() == DecoderJNI.Status.DONE) {
            System.out.println("Decompression Successful: " + new String(directDecompress.getDecompressedData()));
        } else {
            System.out.println("Some Error Occurred While Decompressing");
        }
    }
}
```

### Compressing a stream:

```java
public class Example {
    public static void main(String[] args) {
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        FileInputStream inFile = new FileInputStream(filePath);
        FileOutputStream outFile = new FileOutputStream(filePath + ".br");

        Encoder.Parameters params = new Encoder.Parameters.create(4);

        BrotliOutputStream brotliOutputStream = new BrotliOutputStream(outFile, params);

        int read = inFile.read();
        while (read > -1) {
            brotliOutputStream.write(read);
            read = inFile.read();
        }

        // Close the BrotliOutputStream. This also closes the FileOutputStream.
        brotliOutputStream.close();
        inFile.close();
    }
}
```

### Decompressing a stream:

```java
public class Example {
    public static void main(String[] args) {
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        FileInputStream inFile = new FileInputStream(filePath);
        FileOutputStream outFile = new FileOutputStream(decodedfilePath);

        BrotliInputStream brotliInputStream = new BrotliInputStream(inFile);

        int read = brotliInputStream.read();
        while (read > -1) {
            outFile.write(read);
            read = brotliInputStream.read();
        }

        // Close the BrotliInputStream. This also closes the FileInputStream.
        brotliInputStream.close();
        outFile.close();
    }
}
```

### Additional Notes

> [!NOTE]
> Windows-AArch64: Brotli4j is compiled with JDK 11 with JDK 8 as target because JDK 8 Windows Aarch64 builds are not available at the moment.
> However, it should not cause any problem on running it on JDK 8 or plus.

> [!IMPORTANT]
> RISC-V64: This platform is only supported by JDK 11+ (i.e. JDK 11, JDK 17, JDK 21, atm.).
> However, Since Brotli4j was always compiled with JDK 8, we're cross-compiling RISC-V64 native module bytecode with JDK 8.
> This should not break existing application using Broti4j.
> However, you should use JDK 11+ for using Brotli4j on RISC-V64 platform.

## Projects that use Brotli4j

- [Netty](https://github.com/netty/netty)
- [reactor-netty](https://github.com/reactor/reactor-netty)
- [Armeria](https://github.com/line/armeria)
- [vert.x](https://github.com/eclipse-vertx/vert.x)
- [gatling](https://github.com/gatling/gatling)
- [Netflix Zuul](https://github.com/netflix/zuul)
- [hbase](https://github.com/apache/hbase)
- [micronaut](https://github.com/micronaut-projects/micronaut-core)
- [async-http-client](https://github.com/AsyncHttpClient/async-http-client)
- [Apache NiFi](https://github.com/apache/nifi)
- [quarkus](https://github.com/quarkusio/quarkus)
- [sbt-web-brotli](https://github.com/dwickern/sbt-web-brotli)

## Sponsors

JProfiler is supporting Brotli4J with its full-featured Java Profiler. JProfiler's intuitive UI helps you resolve
performance bottlenecks, pin down memory leaks and understand threading issues. Click below to know more:

<a href="https://www.ej-technologies.com/products/jprofiler/overview.html" target="_blank" title="File Management">
  <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_large.png" alt="File Management">
</a>
