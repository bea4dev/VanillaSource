use std::alloc::{Allocator, AllocError, Layout};
use std::mem::transmute_copy;
use std::ptr::{NonNull, null_mut, slice_from_raw_parts};
use fxhash::FxHashMap;
use crate::pathfinding::{BlockPosition, NodeRef};
use ahash::HashMapExt;
use libc::malloc;


pub struct ManualArenaAllocatorRef {
    pointer: *mut ManualArenaAllocator
}

impl ManualArenaAllocatorRef {

    pub fn new(chunk_allocation_size: usize) -> Self {
        return Self {
            pointer: Box::into_raw(Box::new(ManualArenaAllocator::new(chunk_allocation_size)))
        }
    }

    pub fn reset(&self) {
        unsafe { (*self.pointer).reset() };
    }

}

impl Clone for ManualArenaAllocatorRef {
    fn clone(&self) -> Self {
        return unsafe { transmute_copy(self) }
    }
}

unsafe impl Allocator for ManualArenaAllocatorRef {

    fn allocate(&self, layout: Layout) -> Result<NonNull<[u8]>, AllocError> {
        let address = unsafe { (*self.pointer).malloc(layout.size()) };
        return Ok(NonNull::from(unsafe { &*slice_from_raw_parts(address, layout.size()) }))
    }

    unsafe fn deallocate(&self, _: NonNull<u8>, _: Layout) {
        // Do nothing.
    }

}

pub struct ManualArenaAllocator {
    chunks: Vec<ArenaChunk>,
    chunk_allocation_size: usize,
    current_chunk_index: usize
}

impl ManualArenaAllocator {

    fn new(chunk_allocation_size: usize) -> Self {
        let mut allocator = Self {
            chunks: Vec::new(),
            chunk_allocation_size,
            current_chunk_index: 0
        };
        allocator.add_chunk();
        return allocator;
    }

    fn add_chunk(&mut self) {
        self.chunks.push(ArenaChunk::new(self.chunk_allocation_size));
    }

    #[inline(always)]
    fn malloc(&mut self, size: usize) -> *mut u8 {
        if size > self.chunk_allocation_size {
            panic!("Failed to allocate.(too big)")
        }

        loop {
            let chunk = &mut self.chunks[self.current_chunk_index];
            let address = chunk.malloc(size);

            if address == null_mut() {
                self.add_chunk();
                self.current_chunk_index += 1;
                continue;
            }

            return address;
        }
    }

    fn reset(&mut self) {
        for chunk in self.chunks.iter_mut() {
            chunk.reset();
        }
        self.current_chunk_index = 0;
    }

}

pub struct ArenaChunk {
    address: usize,
    chunk_end_address: usize,
    current_address: usize
}

impl ArenaChunk {

    fn new(allocation_size: usize) -> Self {
        let address = unsafe { malloc(allocation_size) } as usize;
        if address == 0 {
            panic!("Failed to allocate.")
        }

        return Self {
            address,
            chunk_end_address: address + allocation_size,
            current_address: address
        }
    }

    #[inline(always)]
    fn malloc(&mut self, size: usize) -> *mut u8 {
        let next_address = self.current_address + size;
        if next_address > self.chunk_end_address {
            return null_mut();
        }
        let current_address = self.current_address;
        self.current_address = next_address;
        return current_address as *mut u8;
    }

    #[inline(always)]
    fn reset(&mut self) {
        self.current_address = self.address;
    }

}


pub struct ManualAllocatorHolder {
    pub allocator: ManualArenaAllocatorRef,
    pub cached_map: *mut FxHashMap<BlockPosition, NodeRef>
}

impl ManualAllocatorHolder {

    pub fn new() -> Self {
        return Self {
            allocator: ManualArenaAllocatorRef::new(10000),
            cached_map: Box::into_raw(Box::new(FxHashMap::<BlockPosition, NodeRef>::with_capacity(1000)))
        }
    }

}

impl Clone for ManualAllocatorHolder {
    fn clone(&self) -> Self {
        return unsafe { transmute_copy(self) }
    }
}