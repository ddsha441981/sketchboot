/*
 * Copyright (c) 2024-2026 Deendayal Kumawat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ddsha441981.sketchboot.core;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FFM API (Project Panama) bridge for the Rust cl-tds engine.
 * Handles loading the native shared library and mapping C-style symbols to Java MethodHandles.
 */
public class ClTdsNative {

    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String libName = "libsketchboot_native.so"; // Default Linux
            String extension = ".so";
            
            if (os.contains("win")) {
                libName = "sketchboot_native.dll";
                extension = ".dll";
            } else if (os.contains("mac")) {
                libName = "libsketchboot_native.dylib";
                extension = ".dylib";
            }

            // 1. Try to load from inside the JAR (Production mode via CI)
            java.io.InputStream is = ClTdsNative.class.getResourceAsStream("/native/" + libName);
            if (is != null) {
                Path tempLib = java.nio.file.Files.createTempFile("sketchboot_native_", extension);
                tempLib.toFile().deleteOnExit();
                java.nio.file.Files.copy(is, tempLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.load(tempLib.toAbsolutePath().toString());
            } else {
                // 2. Fallback for IDE local development
                Path localLib = Paths.get(System.getProperty("user.dir"), "sketchboot-native", "target", "release", libName);
                System.load(localLib.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native sketchboot library", e);
        }
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    // -------------------------------------------------------------------------
    // Method Handles for our Native Rust Functions
    // -------------------------------------------------------------------------

    // sketch_new(interval_ms: u64) -> *mut c_void
    public static final MethodHandle SKETCH_NEW = findFunction("sketch_new", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // sketch_increment(ptr: *mut c_void, id: u64) -> void
    public static final MethodHandle SKETCH_INCREMENT = findFunction("sketch_increment", 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // sketch_query(ptr: *mut c_void, id: u64) -> u32
    public static final MethodHandle SKETCH_QUERY = findFunction("sketch_query", 
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    // sketch_free(ptr: *mut c_void) -> void
    public static final MethodHandle SKETCH_FREE = findFunction("sketch_free", 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // sketch_increment_batch(ptr: *mut c_void, keys_ptr: *const u64, len: usize) -> void
    public static final MethodHandle SKETCH_INCREMENT_BATCH = findFunction("sketch_increment_batch", 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    /**
     * Helper to lookup and link a native symbol.
     */
    private static MethodHandle findFunction(String name, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(
                LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Cannot find Rust function: " + name)), 
                descriptor
        );
    }
}
