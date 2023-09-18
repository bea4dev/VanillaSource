use std::mem::transmute_copy;
use bumpalo::{Bump, ManualBumpRef};
use fxhash::FxHashMap;
use crate::pathfinding::{BlockPosition, NodeRef};
use ahash::HashMapExt;

pub struct ManualAllocatorHolder {
    pub allocator: ManualBumpRef,
    pub cached_map: *mut FxHashMap<BlockPosition, NodeRef>
}

impl ManualAllocatorHolder {

    pub fn new() -> Self {
        return Self {
            allocator: ManualBumpRef::new(Bump::new()),
            cached_map: Box::into_raw(Box::new(FxHashMap::<BlockPosition, NodeRef>::with_capacity(1000)))
        }
    }

}

impl Clone for ManualAllocatorHolder {
    fn clone(&self) -> Self {
        return unsafe { transmute_copy(self) }
    }
}