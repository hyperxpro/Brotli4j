# Brotli4j

[![Maven Central](https://img.shields.io/maven-central/v/com.aayushatharva.brotli4j/brotli4j-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aayushatharva.brotli4j%22%20AND%20a:%22brotli4j-parent%22)

Brotli4j provides Brotli compression and decompression for Java.

## Supported Platforms:

| Module                 |  Architecture   |                                Tested On |
|:-----------------------|:---------------:|-----------------------------------------:|
| Windows (Server 2022)  |  x86_64 (x64)   |  JDK 8, JDK 11, JDK 17, JDK 21 (temurin) |
| Windows (Server 2022)  |   x86 (i386)    |          JDK 8, JDK 11, JDK 17 (temurin) |
| Windows (11)           | Aarch64 (arm64) |        JDK 11, JDK 17, JDK 21 (liberica) |
| Windows                |   ARMv7 (arm)   |                                 Untested |
| Linux (Ubuntu 24.04)   |  x86_64 (x64)   |  JDK 8, JDK 11, JDK 17, JDK 21 (temurin) |
| Linux (Ubuntu 22.04)   |   x86 (i386)    | JDK 8, JDK 11, JDK 17, JDK 21 (liberica) |
| Linux (Ubuntu 24.04)   | Aarch64 (arm64) |  JDK 8, JDK 11, JDK 17, JDK 21 (temurin) |
| Linux (Ubuntu 24.04)   |   ARMv7 (arm)   |                  JDK 8, JDK 11 (temurin) |
| Linux (Ubuntu 24.04)   |     ppc64le     |  JDK 8, JDK 11, JDK 17, JDK 21 (temurin) |
| Linux (Ubuntu 24.04)   |      s390x      |         JDK 11, JDK 17, JDK 21 (temurin) |
| Linux (Ubuntu 24.04)   |     riscv64     |                 JDK 17, JDK 21 (temurin) |
| macOS (14 Sonoma)      |  x86_64 (x64)   |  JDK 8, JDK 11, JDK 17, JDK 21 (temurin) |
| macOS (14 Sonoma)      | Aarch64 (arm64) |         JDK 11, JDK 17, JDK 21 (temurin) |

#### *Install [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist?view=msvc-170) before running this library on Windows

## Download

### Maven

For maven, the natives will
[import automatically by your system family and architecture](https://github.com/hyperxpro/Brotli4j/blob/main/natives/pom.xml#L38-L114).

```xml
<dependency>
    <groupId>com.aayushatharva.brotli4j</groupId>
    <artifactId>brotli4j</artifactId>
    <version>1.17.0</version>
</dependency>
```

### Gradle

For gradle, we have to write some logic to import native automatically.
Of course, you can add native(s) as dependency manually also.

#### Kotlin DSL

```kotlin
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

val brotliVersion = "1.17.0"
val operatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
val currentArchitecture = DefaultNativePlatform.getCurrentArchitecture().name

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.aayushatharva.brotli4j:brotli4j:${brotliVersion}")
    runtimeOnly(
        "com.aayushatharva.brotli4j:native-" +
                if (operatingSystem.isWindows()) {
                    if (Architectures.X86_64.isAlias(currentArchitecture)) "windows-x86_64"
                    else if (Architectures.X86.isAlias(currentArchitecture)) "windows-x86"
                    else if (Architectures.AARCH64.isAlias(currentArchitecture)) "windows-aarch64"
                    else if (Architectures.ARM_V7.isAlias(currentArchitecture)) "windows-armv7"
                    else throw new IllegalStateException ("Unsupported architecture: $currentArchitecture")
                } else if (operatingSystem.isMacOsX()) {
                    if (Architectures.X86_64.isAlias(currentArchitecture)) "osx-x86_64"
                    else if (Architectures.AARCH64.isAlias(currentArchitecture)) "osx-aarch64"
                    else throw new IllegalStateException ("Unsupported architecture: $currentArchitecture")
                } else if (operatingSystem.isLinux()) {
                    if (Architectures.X86_64.isAlias(currentArchitecture)) "linux-x86_64"
                    else if (Architectures.X86.isAlias(currentArchitecture)) "linux-x86"
                    else if (Architectures.AARCH64.isAlias(currentArchitecture)) "linux-aarch64"
                    else if (Architectures.ARM_V7.isAlias(currentArchitecture)) "linux-armv7"
                    else if ("ppc64le".equals(currentArchitecture, true)) "linux-ppc64le"
                    else if ("s390x".equals(currentArchitecture, true)) "linux-s390x"
                    else if ("riscv64".equals(currentArchitecture, true)) "linux-riscv64"
                    else throw new IllegalStateException ("Unsupported architecture: $currentArchitecture")
                } else {
                    throw new IllegalStateException("Unsupported operating system: $operatingSystem")
                } + ":$brotliVersion"
    )
}
```

#### Groovy

```groovy
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

def brotliVersion = "1.17.0"
def operatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
def currentArchitecture = DefaultNativePlatform.getCurrentArchitecture().name

repositories {
    mavenCentral()
}

dependencies {
    implementation "com.aayushatharva.brotli4j:brotli4j:$brotliVersion"
    runtimeOnly("""com.aayushatharva.brotli4j:native-${
        if (operatingSystem.isWindows())
            if (Architectures.X86_64.isAlias(currentArchitecture)) "windows-x86_64"
            else if (Architectures.X86.isAlias(currentArchitecture)) "windows-x86"
            else if (Architectures.AARCH64.isAlias(currentArchitecture)) "windows-aarch64"
            else if (Architectures.ARM_V7.isAlias(currentArchitecture)) "windows-armv7"
            else throw new IllegalStateException("Unsupported architecture: $currentArchitecture")
        else if (operatingSystem.isMacOsX())
            if (Architectures.X86_64.isAlias(currentArchitecture)) "osx-x86_64"
            else if (Architectures.AARCH64.isAlias(currentArchitecture)) "osx-aarch64"
            else throw new IllegalStateException("Unsupported architecture: $currentArchitecture")
        else if (operatingSystem.isLinux())
            if (Architectures.X86_64.isAlias(currentArchitecture)) "linux-x86_64"
            else if (Architectures.X86.isAlias(currentArchitecture)) "linux-x86"
            else if (Architectures.AARCH64.isAlias(currentArchitecture)) "linux-aarch64"
            else if (Architectures.ARM_V7.isAlias(currentArchitecture)) "linux-armv7"
            else if ("ppc64le".equalsIgnoreCase(currentArchitecture)) "linux-ppc64le"
            else if ("s390x".equalsIgnoreCase(currentArchitecture)) "linux-s390x"
            else if ("riscv64".equalsIgnoreCase(currentArchitecture)) "linux-riscv64"
            else throw new IllegalStateException("Unsupported architecture: $currentArchitecture")
        else throw new IllegalStateException("Unsupported operating system: $operatingSystem")
    }:$brotliVersion""")
}
```

## Usage

### Loading native library:

Call `Brotli4jLoader.ensureAvailability()` in your application once before using Brotli4j. This will load
Brotli4j native library automatically using automatic dependency resolution.
However, it's possible to load native library manually from custom path by specifying System Property `"brotli4j.library.path"`.

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

        Encoder.Parameters params = new Encoder.Parameters().setQuality(4);

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