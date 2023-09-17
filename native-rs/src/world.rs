use std::collections::HashMap;
use std::sync::Arc;
use valence::BlockState;
use crate::registry::GLOBAL_REGISTRY;
use crate::utils::{get_chunk_index, get_palette_index};

pub struct World {
    pub id: i32,
    pub height: i32,
    pub min_y: i32,
    chunks: HashMap<i64, Arc<Chunk>>
}

impl World {

    pub fn new(id: i32, height: i32, min_y: i32) -> Self {
        return Self {
            id,
            height,
            min_y,
            chunks: HashMap::new()
        }
    }

    pub fn register_chunk(&mut self, chunk: Chunk) {
        let index = get_chunk_index(chunk.chunk_x, chunk.chunk_z);
        self.chunks.insert(index, Arc::new(chunk));
    }

}

pub struct ThreadLocalWorld {
    pub id: i32,
    pub chunks: HashMap<i64, Arc<Chunk>>
}

impl ThreadLocalWorld {

    pub fn new(id: i32) -> Self {
        return Self {
            id,
            chunks: HashMap::new()
        }
    }

    pub fn get_chunk(&mut self, chunk_x: i32, chunk_z: i32) -> &mut Arc<Chunk> {
        let index = get_chunk_index(chunk_x, chunk_z);
        return self.chunks.entry(index).or_insert_with(|| {
            let registry = GLOBAL_REGISTRY.get().unwrap().lock().unwrap();
            let world = match registry.worlds.get(&self.id) {
                Some(world) => world,
                None => return registry.empty_chunk.clone()
            };
            match world.chunks.get(&index) {
                Some(chunk) => chunk.clone(),
                _ => registry.empty_chunk.clone()
            }
        })
    }

    pub fn get_block(&mut self, x: i32, y: i32, z: i32) -> BlockState {
        let chunk = self.get_chunk(x >> 4, z >> 4);
        return chunk.get_block(x, y, z);
    }

}


pub struct Chunk {
    pub chunk_x: i32,
    pub chunk_z: i32,
    pub min_section_y: i32,
    pub max_section_y: i32,
    pub sections: Vec<ChunkSection>
}

impl Chunk {

    pub fn new(chunk_x: i32, chunk_z: i32, min_section_y: i32, max_section_y: i32, section_size: usize) -> Self {
        let mut sections = Vec::with_capacity(section_size);
        for _ in 0..section_size {
            sections.push(ChunkSection::new())
        }
        return Self {
            chunk_x,
            chunk_z,
            min_section_y,
            max_section_y,
            sections
        }
    }

    #[inline(always)]
    pub fn is_empty(&self) -> bool {
        return self.sections.is_empty()
    }

    pub fn get_block(&self, x: i32, y: i32, z: i32) -> BlockState {
        return if self.is_empty() {
            BlockState::AIR
        } else {
            let index = (y >> 4) - self.min_section_y;
            let section = &self.sections[index as usize];
            section.get_block(x, y, z)
        }
    }

    pub fn set_block(&mut self, x: i32, y: i32, z: i32, block: BlockState) {
        if !self.is_empty() {
            let index = (y >> 4) - self.min_section_y;
            let section = &mut self.sections[index as usize];
            section.set_block(x, y, z, block);
        }
    }

}



pub const SECTION_BLOCKS_LENGTH: usize = 16 * 16 * 16;

pub struct ChunkSection {
    pub palette: [BlockState; SECTION_BLOCKS_LENGTH]
}

impl ChunkSection {

    pub fn new() -> Self {
        return Self {
            palette: [BlockState::AIR; SECTION_BLOCKS_LENGTH]
        }
    }

    #[inline(always)]
    pub fn get_block(&self, x: i32, y: i32, z: i32) -> BlockState {
        return self.palette[get_palette_index(x, y, z)];
    }

    #[inline(always)]
    pub fn set_block(&mut self, x: i32, y: i32, z: i32, block: BlockState) {
        self.palette[get_palette_index(x, y, z)] = block;
    }

}