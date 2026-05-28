/*
 *   Copyright (c) 2020-2026, Aayush Atharva
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Brotli4jLoaderWorkdirTest {

    private static final String WORKDIR_PROPERTY = "com.aayushatharva.brotli4j.native.workdir";

    private String previousWorkdir;

    @BeforeEach
    void saveProperty() {
        previousWorkdir = System.getProperty(WORKDIR_PROPERTY);
        System.clearProperty(WORKDIR_PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (previousWorkdir == null) {
            System.clearProperty(WORKDIR_PROPERTY);
        } else {
            System.setProperty(WORKDIR_PROPERTY, previousWorkdir);
        }
    }

    @Test
    void defaultsToJavaIoTmpdir() throws IOException {
        File workdir = Brotli4jLoader.resolveWorkdir();
        assertEquals(new File(System.getProperty("java.io.tmpdir")), workdir);
    }

    @Test
    void usesExistingCustomWorkdir(@TempDir Path tempDir) throws IOException {
        System.setProperty(WORKDIR_PROPERTY, tempDir.toString());

        File workdir = Brotli4jLoader.resolveWorkdir();

        assertTrue(workdir.isAbsolute());
        assertEquals(tempDir.toFile().getAbsoluteFile(), workdir);
        assertTrue(workdir.isDirectory());
    }

    @Test
    void createsMissingWorkdir(@TempDir Path tempDir) throws IOException {
        File missing = tempDir.resolve("nested/created/workdir").toFile();
        assertFalse(missing.exists());
        System.setProperty(WORKDIR_PROPERTY, missing.getPath());

        File workdir = Brotli4jLoader.resolveWorkdir();

        assertEquals(missing.getAbsoluteFile(), workdir);
        assertTrue(workdir.isDirectory());
    }

    @Test
    void throwsWhenWorkdirCannotBeCreated(@TempDir Path tempDir) throws IOException {
        Path blocker = tempDir.resolve("blocker");
        Files.createFile(blocker);
        Path unreachable = blocker.resolve("subdir");
        System.setProperty(WORKDIR_PROPERTY, unreachable.toString());

        IOException error = assertThrows(IOException.class, Brotli4jLoader::resolveWorkdir);
        assertTrue(error.getMessage().contains(unreachable.toString()));
    }
}
