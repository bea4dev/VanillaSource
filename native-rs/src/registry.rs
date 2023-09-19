use std::collections::HashMap;
use std::sync::{Arc, Mutex, OnceLock};
use fxhash::FxHashMap;
use crate::allocator::{ManualAllocatorHolder, ManualArenaAllocatorRef};
use crate::pathfinding::{BlockPosition, run_pathfinding};
use crate::world::{Chunk, ThreadLocalWorld, World};


pub static GLOBAL_REGISTRY: OnceLock<Mutex<GlobalRegistry>> = OnceLock::new();

pub fn initialize_global_registry() {
    GLOBAL_REGISTRY.get_or_init(|| { Mutex::new(GlobalRegistry::new()) });
}


pub struct GlobalRegistry {
    pub worlds: HashMap<i32, World>,
    pub empty_chunk: Arc<Chunk>
}

impl GlobalRegistry {

    pub fn new() -> Self {
        return Self {
            worlds: HashMap::new(),
            empty_chunk: Arc::new(Chunk::new(0, 0, 0, 0, 0))
        }
    }

    pub fn create_world(&mut self, id: i32, height: i32, min_y: i32) {
        self.worlds.entry(id).or_insert_with(|| { World::new(id, height, min_y) });
    }

}


pub struct ThreadLocalRegistry {
    pub worlds: FxHashMap<i32, ThreadLocalWorld>,
    pub allocator_holder: ManualAllocatorHolder
}

impl ThreadLocalRegistry {

    pub fn new() -> Self {
        return Self {
            worlds: HashMap::default(),
            allocator_holder: ManualAllocatorHolder::new()
        }
    }

    pub fn get_world(&mut self, id: i32) -> &mut ThreadLocalWorld {
        return self.worlds.entry(id).or_insert_with(|| { ThreadLocalWorld::new(id) });
    }

    pub fn run_pathfinding(
        &mut self,
        world_id: i32,
        start: &BlockPosition,
        goal: &BlockPosition,
        descending_height: i32,
        jump_height: i32,
        max_iteration: usize
    ) -> Vec<BlockPosition, ManualArenaAllocatorRef> {
        let allocator_holder = self.allocator_holder.clone();
        allocator_holder.allocator.reset();
        unsafe { &mut *allocator_holder.cached_map }.clear();

        let world = self.get_world(world_id);
        return run_pathfinding(
            allocator_holder,
            world,
            start,
            goal,
            descending_height,
            jump_height,
            max_iteration
        );
    }

}
