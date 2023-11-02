# Brotli4j

[![Maven Central](https://img.shields.io/maven-central/v/com.aayushatharva.brotli4j/brotli4j-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aayushatharva.brotli4j%22%20AND%20a:%22brotli4j-parent%22)

Brotli4j provides Brotli compression and decompression for Java.

## Supported Platforms:

| Module                        | Architecture |                       Tested On |
|:------------------------------|:------------:|--------------------------------:|
| Windows (Windows Server 2022) |     x64      | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| Linux (CentOS 6)              |     x64      | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| Linux (Ubuntu 18.04)          |   Aarch64    | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| Linux (Ubuntu 18.04)          |    ARMv7     |         JDK 1.8, JDK 11, JDK 17 |
| Linux (Ubuntu 18.04)          |    s390x     |                 JDK 1.8, JDK 11 |
| Linux (Ubuntu 18.04)          |   ppc64le    |                 JDK 1.8, JDK 11 |
| Linux (Ubuntu 20.04)          |   RISC-v64   | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| macOS (Catalina)              |     x64      | JDK 1.8, JDK 11, JDK 17, JDK 21 |
| macOS (Catalina)              |   Aarch64    | JDK 1.8, JDK 11, JDK 17, JDK 21 |

#### *Install [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist?view=msvc-170) before running this library on Windows

## Download

### Maven

For maven, the natives will
[import automatically by your system family and architecture](https://github.com/hyperxpro/Brotli4j/blob/main/natives/pom.xml#L38-L114).

```xml
<dependency>
    <groupId>com.aayushatharva.brotli4j</groupId>
    <artifactId>brotli4j</artifactId>
    <version>1.13.0</version>
</dependency>
```

### Gradle

For gradle, we have to write some logic to import native automatically.
Of course, you can add native(s) as dependency manually also.

#### Kotlin DSL

```kotlin
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.nativeplatform.operatingsystem.OperatingSystem

val brotliVersion = "1.13.0"
val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
    runtimeOnly(
        "com.aayushatharva.brotli4j:native-" +
                if (operatingSystem.isWindows) {
                    "windows-x86_64"
                } else if (operatingSystem.isMacOsX) {
                    if (DefaultNativePlatform.getCurrentArchitecture().isArm()) {
                        "osx-aarch64"
                    } else {
                        "osx-x86_64"
                    }
                } else if (operatingSystem.isLinux) {
                    if (Architectures.ARM_V7.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                        "linux-armv7"
                    } else if (Architectures.AARCH64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                        "linux-aarch64"
                    } else if (Architectures.X86_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                        "linux-x86_64"
                    } else if (Architectures.S390X.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                        "linux-s390x"
                    } else if (Architectures.RISCV_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                        "linux-riscv64"
                    } else {
                        throw IllegalStateException("Unsupported architecture: ${DefaultNativePlatform.getCurrentArchitecture().name}")
                    }
                } else {
                    throw IllegalStateException("Unsupported operating system: $operatingSystem")
                } + ":$brotliVersion"
    )
}
```

#### Groovy

```groovy
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

def brotliVersion = "1.13.0"
def operatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

repositories {
    mavenCentral()
}

dependencies {
    implementation "com.aayushatharva.brotli4j:brotli4j:$brotliVersion"
    runtimeOnly("""com.aayushatharva.brotli4j:native-${
        if (operatingSystem.isWindows()) "windows-x86_64"
        else if (operatingSystem.isMacOsX())
            if (DefaultNativePlatform.getCurrentArchitecture().isArm()) "osx-aarch64"
            else "osx-x86_64"
        else if (operatingSystem.isLinux())
            if (Architectures.ARM_V7.isAlias(DefaultNativePlatform.getCurrentArchitecture().getName())) "linux-armv7"
            else if (Architectures.AARCH64.isAlias(DefaultNativePlatform.getCurrentArchitecture().getName())) "linux-aarch64"
            else if (Architectures.X86_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().getName())) "linux-x86_64"
            else if (Architectures.S390X.isAlias(DefaultNativePlatform.getCurrentArchitecture().getName())) "linux-s390x"
            else if (Architectures.RISCV_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().getName())) "linux-riscv64"
            else
                throw new IllegalStateException("Unsupported architecture: ${DefaultNativePlatform.getCurrentArchitecture().getName()}");
        else
            throw new IllegalStateException("Unsupported operating system: $operatingSystem");
    }:$brotliVersion""")
}
```

## Usage

### Loading native library:

Call `Brotli4jLoader.ensureAvailability()` in your application once before using Brotli4j. This will load
Brotli4j native library automatically using automatic dependency resolution.
However, its possible to load native library manually from custom path by specifying System Property `"brotli4j.library.path"`.

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

### Additional Notes

* RISC-V64: This platform is only supported by JDK 11+ (i.e. JDK 11, JDK 17, JDK 21, atm.). However, Since Brotli4j was always compiled
with JDK 8, we're cross-compiling RISC-V64 native module bytecode with JDK 8. This should not break existing application using
Broti4j. However, you should use JDK 11+ for using Brotli4j on RISC-V64 platform.
__________________________________________________________________

## Sponsors

JProfiler is supporting Brotli4J with its full-featured Java Profiler. JProfiler's intuitive UI helps you resolve
performance bottlenecks, pin down memory leaks and understand threading issues. Click below to know more:

<a href="https://www.ej-technologies.com/products/jprofiler/overview.html" target="_blank" title="File Management">
  <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_large.png" alt="File Management">
</a>
