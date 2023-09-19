#![feature(btreemap_alloc)]
#![feature(allocator_api)]

mod registry;
mod world;
mod utils;
mod pathfinding;
mod allocator;

use std::mem::transmute_copy;
use std::ptr::slice_from_raw_parts;
use jni::JNIEnv;
use jni::objects::JIntArray;
use jni::sys::{jclass, jint, jintArray, jlong, jsize};
use valence::BlockState;
use crate::pathfinding::BlockPosition;
use crate::registry::{GLOBAL_REGISTRY, initialize_global_registry, ThreadLocalRegistry};
use crate::utils::get_chunk_index;
use crate::world::{Chunk, SECTION_BLOCKS_LENGTH};


const VERSION: i32 = 0;

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_getVersion(env: JNIEnv, class: jclass) -> jint {
    return VERSION;
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_createGlobalRegistry(env: JNIEnv, class: jclass) {
    initialize_global_registry();
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_registerWorld(env: JNIEnv, class: jclass, world_id: jint, height: jint, min_y: jint) {
    let mut registry = GLOBAL_REGISTRY.get().unwrap().lock().unwrap();
    registry.create_world(world_id, height, min_y);
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_registerChunk(env: JNIEnv, class: jclass, world_id: jint, chunk_x: jint, chunk_z: jint, blocks: jintArray) {
    let mut registry = GLOBAL_REGISTRY.get().unwrap().lock().unwrap();
    let world = match registry.worlds.get_mut(&world_id) {
        Some(world) => world,
        _ => return
    };
    let min_section_y = world.min_y / 16;
    let max_section_y = (world.min_y + world.height) / 16;
    let section_size = (max_section_y - min_section_y) as usize;
    let mut chunk = Chunk::new(chunk_x, chunk_z, min_section_y, max_section_y, section_size);

    for i in 0..chunk.sections.len() {
        let section = chunk.sections.get_mut(i).unwrap();
        let palette_start_index = SECTION_BLOCKS_LENGTH * i;
        let mut buffer = [0; SECTION_BLOCKS_LENGTH];
        let original_array = unsafe { JIntArray::from_raw(blocks) };
        env.get_int_array_region(original_array, palette_start_index as jsize, &mut buffer).unwrap();

        for i in 0..buffer.len() {
            section.palette[i] = BlockState::from_raw(buffer[i] as u16).unwrap();
        }
    }
    world.register_chunk(chunk);
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_createThreadLocalRegistry(env: JNIEnv, class: jclass) -> jlong {
    return Box::into_raw(Box::new(ThreadLocalRegistry::new())) as usize as i64
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_unregisterChunk(env: JNIEnv, class: jclass, registry_address: jlong, world_id: jint, chunk_x: jint, chunk_z: jint) {
    let registry = unsafe { &mut *(registry_address as usize as *mut ThreadLocalRegistry) };
    registry.get_world(world_id).chunks.remove(&get_chunk_index(chunk_x, chunk_z));
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_runPathfinding(env: JNIEnv, class: jclass, registry_address: jlong, world_id: jint, args: jintArray, return_array: jintArray) -> jintArray {
    let registry = unsafe { &mut *(registry_address as usize as *mut ThreadLocalRegistry) };

    let mut arg_temp_array = [0; 9];
    let jint_args_array = unsafe { JIntArray::from_raw(args) };
    env.get_int_array_region(jint_args_array, 0, &mut arg_temp_array).unwrap();

    let start = BlockPosition::new(arg_temp_array[0], arg_temp_array[1], arg_temp_array[2]);
    let goal = BlockPosition::new(arg_temp_array[3], arg_temp_array[4], arg_temp_array[5]);
    let descending_height = arg_temp_array[6];
    let jump_height = arg_temp_array[7];
    let max_iteration = arg_temp_array[8] as usize;

    let paths = registry.run_pathfinding(world_id, &start, &goal, descending_height, jump_height, max_iteration);

    let return_array = unsafe { JIntArray::from_raw(return_array) };
    let array_size = (paths.len() * 3 + 1) as jsize;
    let mut return_array = if env.get_array_length(&return_array).unwrap() >= array_size {
        return_array
    } else {
        env.new_int_array(array_size).unwrap()
    };

    let ptr: *const i32 = unsafe { transmute_copy(&paths.as_slice().as_ptr()) };
    let slice = unsafe { &*slice_from_raw_parts(ptr, paths.len() * 3) };

    env.set_int_array_region(&mut return_array, 0, &[paths.len() as jint]).unwrap();
    env.set_int_array_region(&mut return_array, 1, slice).unwrap();

    return return_array.as_raw()
}

#[no_mangle]
#[allow(unused_variables, non_snake_case)]
pub extern "system" fn Java_com_github_bea4dev_vanilla_1source_natives_NativeBridge_removeThreadLocalRegistry(env: JNIEnv, class: jclass, registry_address: jlong) {
    drop(unsafe { Box::from_raw(registry_address as usize as *mut ThreadLocalRegistry) });
}