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

use cl_tds::ClTds;
use lz4_flex::compress_prepend_size;
use lz4_flex::decompress_size_prepended;
use std::ffi::c_void;

#[no_mangle]
pub extern "C" fn sketch_new(interval_ms: u64) -> *mut c_void {
    let sketch = if interval_ms == 0 {
        Box::new(ClTds::new())
    } else {
        Box::new(ClTds::with_epoch_interval(interval_ms))
    };
    Box::into_raw(sketch) as *mut c_void
}

#[no_mangle]
pub unsafe extern "C" fn sketch_increment(ptr: *mut c_void, id: u64) {
    if ptr.is_null() {
        return;
    }
    let sketch = &*(ptr as *mut ClTds);
    sketch.increment(id);
}

#[no_mangle]
pub unsafe extern "C" fn sketch_increment_batch(ptr: *mut c_void, keys_ptr: *const u64, len: usize) {
    if ptr.is_null() || keys_ptr.is_null() || len == 0 {
        return;
    }
    let sketch = &*(ptr as *mut ClTds);
    let keys = std::slice::from_raw_parts(keys_ptr, len);
    for &key in keys {
        sketch.increment(key);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sketch_query(ptr: *mut c_void, id: u64) -> u32 {
    if ptr.is_null() {
        return 0;
    }
    let sketch = &*(ptr as *mut ClTds);
    sketch.query(id)
}

#[no_mangle]
pub unsafe extern "C" fn sketch_free(ptr: *mut c_void) {
    if !ptr.is_null() {
        let _ = Box::from_raw(ptr as *mut ClTds);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sketch_export_compressed(ptr: *mut c_void, out_size: *mut usize) -> *mut u8 {
    if ptr.is_null() {
        if !out_size.is_null() { *out_size = 0; }
        return std::ptr::null_mut();
    }
    let sketch = &*(ptr as *mut ClTds);
    let bytes = sketch.to_bytes(); // Gets the 1 MB state
    
    // Compress the 1 MB state (will become very small)
    let compressed = compress_prepend_size(&bytes);
    
    let mut boxed_slice = compressed.into_boxed_slice();
    let len = boxed_slice.len();
    let ptr_bytes = boxed_slice.as_mut_ptr();
    
    // Leak the slice so Java can safely read it, we provide sketch_free_bytes for cleanup
    std::mem::forget(boxed_slice);
    
    if !out_size.is_null() {
        *out_size = len;
    }
    ptr_bytes
}

#[no_mangle]
pub unsafe extern "C" fn sketch_free_bytes(ptr: *mut u8, len: usize) {
    if !ptr.is_null() {
        let _ = Box::from_raw(std::slice::from_raw_parts_mut(ptr, len));
    }
}

/// Merges an LZ4 compressed sketch state into the current sketch.
/// Returns true on success, false on failure.
#[no_mangle]
pub unsafe extern "C" fn sketch_merge_compressed(ptr: *mut c_void, compressed_bytes: *const u8, size: usize) -> bool {
    if ptr.is_null() || compressed_bytes.is_null() || size == 0 {
        return false;
    }
    let sketch = &*(ptr as *mut ClTds);
    let slice = std::slice::from_raw_parts(compressed_bytes, size);
    
    // 1. Decompress back to 1 MB
    match decompress_size_prepended(slice) {
        Ok(decompressed) => {
            // 2. Load into a temporary sketch
            if let Some(other_sketch) = ClTds::from_bytes(&decompressed) {
                // TODO: cl_tds currently does not have a `merge` method in lib.rs.
                return false; 
            }
            false
        }
        Err(_) => false,
    }
}
