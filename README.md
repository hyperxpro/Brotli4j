# Brotli4j
Brotli4j provides Brotli compression and decompression for Java.

## Maven
[![Maven Central](https://img.shields.io/maven-central/v/com.aayushatharva.brotli4j/brotli4j-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aayushatharva.brotli4j%22%20AND%20a:%22brotli4j-parent%22)
```
<dependency>
    <groupId>com.aayushatharva.brotli4j</groupId>
    <artifactId>brotli4j</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Supported Platforms:
Windows 64-Bit (Tested on Windows 10 Pro) </br>
Linux 64-Bit (Tested on CentOS 7, CentOS 8 and Ubuntu 20.04 LTS)

## Loading native library:
Call `Brotli4jLoader.ensureAvailability()` in your application once before using Brotli4j.

## Examples
### Direct API
```java
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
```

### Compressing a stream:
```java
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        FileInputStream inFile = new FileInputStream(filePath);
        FileOutputStream outFile = new FileOutputStream(filePath + ".br");

        Encoder.Parameters params = new Encoder.Parameters().setQuality(4);
        
        BrotliOutputStream brotliOutputStream = new BrotliOutputStream(outFile, params);

        int read = inFile.read();
        while(read > -1) {
            brotliOutputStream.write(read);
            read = inFile.read();
        }

        // Close the BrotliOutputStream. This also closes the FileOutputStream.
        brotliOutputStream.close();
        inFile.close();
```

### Decompressing a stream:
```java
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        FileInputStream inFile = new FileInputStream(filePath);
        FileOutputStream outFile = new FileOutputStream(decodedfilePath);

        BrotliInputStream brotliInputStream = new BrotliInputStream(inFile);

        int read = brotliInputStream.read();
        while(read > -1) {
            outFile.write(read);
            read = brotliInputStream.read();
        }

        // Close the BrotliInputStream. This also closes the FileInputStream.
        brotliInputStream.close();
        outFile.close();
```
__________________________________________________________________
## Sponsors

JProfiler is supporting Brotli4J with its full-featured Java Profiler. JProfiler's intuitive UI helps you resolve performance bottlenecks,
pin down memory leaks and understand threading issues. Click below to know more:

<a href="https://www.ej-technologies.com/products/jprofiler/overview.html" target="_blank" title="File Management">
  <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_large.png" alt="File Management">
</a>
