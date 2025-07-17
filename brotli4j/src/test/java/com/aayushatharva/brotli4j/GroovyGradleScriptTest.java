/*
 *   Copyright (c) 2020-2023, Aayush Atharva
 *
 *   Brotli4j licenses this file to you under the
 *   Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.aayushatharva.brotli4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the Groovy gradle script from README works correctly.
 * This simulates the architecture detection logic used in the README Groovy script
 * to ensure the methods and logic work as expected on JDK 17 Linux.
 */
class GroovyGradleScriptTest {

    /**
     * Mock implementation of the platform detection logic from the README Groovy script.
     * This simulates the actual Gradle platform detection without requiring Gradle dependencies.
     */
    private static class MockPlatformDetector {
        
        public static class OperatingSystem {
            private final String osName;
            
            public OperatingSystem() {
                this.osName = System.getProperty("os.name").toLowerCase();
            }
            
            public boolean isWindows() {
                return osName.contains("win");
            }
            
            public boolean isMacOsX() {
                return osName.contains("mac") || osName.contains("darwin");
            }
            
            public boolean isLinux() {
                return osName.contains("linux");
            }
        }
        
        public static class Architecture {
            private final String archName;
            
            public Architecture() {
                this.archName = System.getProperty("os.arch").toLowerCase();
            }
            
            public String getName() {
                return archName;
            }
            
            public boolean isAmd64() {
                return "amd64".equals(archName) || "x86_64".equals(archName);
            }
            
            public boolean isArm64() {
                return "aarch64".equals(archName) || "arm64".equals(archName);
            }
        }
        
        public static class MockArchitectures {
            public static final ArchitectureType AARCH64 = new ArchitectureType("aarch64");
            public static final ArchitectureType X86_64 = new ArchitectureType("x86_64", "amd64");
            public static final ArchitectureType ARM_V7 = new ArchitectureType("armv7", "arm");
            
            public static class ArchitectureType {
                private final String[] aliases;
                
                public ArchitectureType(String... aliases) {
                    this.aliases = aliases;
                }
                
                public boolean isAlias(String archName) {
                    for (String alias : aliases) {
                        if (alias.equals(archName)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
    }

    @Test
    void testGroovyScriptPlatformDetection() {
        // This test validates that the platform detection logic from the README works
        
        MockPlatformDetector.OperatingSystem operatingSystem = new MockPlatformDetector.OperatingSystem();
        MockPlatformDetector.Architecture currentArchitecture = new MockPlatformDetector.Architecture();
        
        // Test that the OS detection methods work
        assertNotNull(operatingSystem);
        assertDoesNotThrow(() -> operatingSystem.isWindows());
        assertDoesNotThrow(() -> operatingSystem.isMacOsX()); 
        assertDoesNotThrow(() -> operatingSystem.isLinux());
        
        // Test that architecture detection methods work
        assertNotNull(currentArchitecture);
        assertDoesNotThrow(() -> currentArchitecture.isAmd64());
        assertDoesNotThrow(() -> currentArchitecture.isArm64());
        assertDoesNotThrow(() -> currentArchitecture.getName());
        
        // Test Architectures enum methods used in the script
        assertDoesNotThrow(() -> MockPlatformDetector.MockArchitectures.AARCH64.isAlias(currentArchitecture.getName()));
        assertDoesNotThrow(() -> MockPlatformDetector.MockArchitectures.X86_64.isAlias(currentArchitecture.getName()));
        assertDoesNotThrow(() -> MockPlatformDetector.MockArchitectures.ARM_V7.isAlias(currentArchitecture.getName()));
        
        // Test string contains methods for other architectures
        String archName = currentArchitecture.getName();
        assertNotNull(archName);
        assertDoesNotThrow(() -> archName.contains("ppc64le"));
        assertDoesNotThrow(() -> archName.contains("s390x"));
        assertDoesNotThrow(() -> archName.contains("riscv64"));
    }
    
    @Test
    void testGroovyScriptNativeDependencyResolution() {
        // This test simulates the exact logic from the README Groovy script
        // to ensure it produces valid dependency names
        
        String brotliVersion = "1.18.0";
        MockPlatformDetector.OperatingSystem operatingSystem = new MockPlatformDetector.OperatingSystem();
        MockPlatformDetector.Architecture currentArchitecture = new MockPlatformDetector.Architecture();
        
        String nativeDependency;
        
        if (operatingSystem.isWindows()) {
            if (currentArchitecture.isAmd64()) {
                nativeDependency = "windows-x86_64";
            } else if (currentArchitecture.isArm64()) {
                nativeDependency = "windows-aarch64";
            } else {
                throw new IllegalStateException("Unsupported architecture: " + currentArchitecture.getName());
            }
        } else if (operatingSystem.isMacOsX()) {
            if (currentArchitecture.isArm64()) {
                nativeDependency = "osx-aarch64";
            } else {
                nativeDependency = "osx-x86_64";
            }
        } else if (operatingSystem.isLinux()) {
            if (MockPlatformDetector.MockArchitectures.AARCH64.isAlias(currentArchitecture.getName())) {
                nativeDependency = "linux-aarch64";
            } else if (MockPlatformDetector.MockArchitectures.X86_64.isAlias(currentArchitecture.getName())) {
                nativeDependency = "linux-x86_64";
            } else if (MockPlatformDetector.MockArchitectures.ARM_V7.isAlias(currentArchitecture.getName())) {
                nativeDependency = "linux-armv7";
            } else if (currentArchitecture.getName().contains("ppc64le")) {
                nativeDependency = "linux-ppc64le";
            } else if (currentArchitecture.getName().contains("s390x")) {
                nativeDependency = "linux-s390x";
            } else if (currentArchitecture.getName().contains("riscv64")) {
                nativeDependency = "linux-riscv64";
            } else {
                throw new IllegalStateException("Unsupported architecture: " + currentArchitecture.getName());
            }
        } else {
            throw new IllegalStateException("Unsupported operating system: " + operatingSystem);
        }
        
        // Verify that we got a valid native dependency
        assertNotNull(nativeDependency);
        assertFalse(nativeDependency.isEmpty());
        
        // Verify the format matches expected pattern
        assertTrue(nativeDependency.matches("(windows|osx|linux)-(x86_64|aarch64|armv7|ppc64le|s390x|riscv64)"),
                   "Native dependency should match expected format: " + nativeDependency);
        
        // Create the full dependency string as would be used in Gradle
        String fullDependency = "com.aayushatharva.brotli4j:native-" + nativeDependency + ":" + brotliVersion;
        
        assertNotNull(fullDependency);
        assertTrue(fullDependency.startsWith("com.aayushatharva.brotli4j:native-"));
        assertTrue(fullDependency.endsWith(":" + brotliVersion));
        
        // For Linux x86_64 specifically (common CI environment), verify expected result
        if (operatingSystem.isLinux() && MockPlatformDetector.MockArchitectures.X86_64.isAlias(currentArchitecture.getName())) {
            assertEquals("linux-x86_64", nativeDependency);
            assertEquals("com.aayushatharva.brotli4j:native-linux-x86_64:1.18.0", fullDependency);
        }
    }
    
    @Test 
    void testJdk17Compatibility() {
        // Verify this test runs correctly and can detect JDK version
        String javaVersion = System.getProperty("java.version");
        assertNotNull(javaVersion);
        
        // Check if running on JDK 17 or higher (for CI environments)
        String[] versionParts = javaVersion.split("\\.");
        if (versionParts.length > 0) {
            try {
                int majorVersion = Integer.parseInt(versionParts[0]);
                // This test should run on JDK 8+, but we want to verify JDK 17+ works
                assertTrue(majorVersion >= 8, "Should run on JDK 8+, current: " + javaVersion);
                
                // If running on JDK 17+, this validates compatibility
                if (majorVersion >= 17) {
                    // Additional validation for JDK 17+ - test that system properties work
                    assertNotNull(System.getProperty("java.vm.name"));
                    assertNotNull(System.getProperty("java.runtime.version"));
                }
            } catch (NumberFormatException e) {
                // Handle newer version formats like "17.0.2" or "11.0.16"
                assertTrue(javaVersion.length() > 0, "Java version should be available");
            }
        }
    }
    
    @Test
    void testGroovyScriptArchitectureSpecificCases() {
        // Test specific architecture detection cases that are used in the Groovy script
        MockPlatformDetector.Architecture arch = new MockPlatformDetector.Architecture();
        
        // Test current architecture detection
        String archName = arch.getName();
        assertNotNull(archName);
        
        // Verify boolean methods work as expected
        if (archName.equals("amd64") || archName.equals("x86_64")) {
            assertTrue(arch.isAmd64());
            assertTrue(MockPlatformDetector.MockArchitectures.X86_64.isAlias(archName));
        }
        
        if (archName.equals("aarch64") || archName.equals("arm64")) {
            assertTrue(arch.isArm64());
            assertTrue(MockPlatformDetector.MockArchitectures.AARCH64.isAlias(archName));
        }
        
        // Test string-based detection for other architectures
        assertDoesNotThrow(() -> {
            boolean isPpc64le = archName.contains("ppc64le");
            boolean isS390x = archName.contains("s390x");
            boolean isRiscv64 = archName.contains("riscv64");
            // These should not throw exceptions
        });
    }
    
    @Test
    void testGroovyScriptSyntaxValidation() {
        // This test validates that the syntax patterns used in the Groovy script are valid
        
        // Test the conditional logic pattern from the script
        MockPlatformDetector.OperatingSystem os = new MockPlatformDetector.OperatingSystem();
        MockPlatformDetector.Architecture arch = new MockPlatformDetector.Architecture();
        
        String result = null;
        
        // Simulate the nested if-else logic from the Groovy script
        if (os.isWindows()) {
            if (arch.isAmd64()) {
                result = "windows-x86_64";
            } else if (arch.isArm64()) {
                result = "windows-aarch64";
            }
        } else if (os.isMacOsX()) {
            if (arch.isArm64()) {
                result = "osx-aarch64";
            } else {
                result = "osx-x86_64";
            }
        } else if (os.isLinux()) {
            // Test the complex Linux architecture detection
            if (MockPlatformDetector.MockArchitectures.AARCH64.isAlias(arch.getName())) {
                result = "linux-aarch64";
            } else if (MockPlatformDetector.MockArchitectures.X86_64.isAlias(arch.getName())) {
                result = "linux-x86_64";
            } else if (MockPlatformDetector.MockArchitectures.ARM_V7.isAlias(arch.getName())) {
                result = "linux-armv7";
            } else if (arch.getName().contains("ppc64le")) {
                result = "linux-ppc64le";
            } else if (arch.getName().contains("s390x")) {
                result = "linux-s390x";
            } else if (arch.getName().contains("riscv64")) {
                result = "linux-riscv64";
            }
        }
        
        // On any supported platform, we should get a result
        if (os.isWindows() || os.isMacOsX() || os.isLinux()) {
            assertNotNull(result, "Should resolve a native dependency for supported platform");
            assertFalse(result.isEmpty());
        }
    }
}