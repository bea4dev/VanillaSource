use std::cmp::Ordering;
use std::collections::BTreeSet;
use std::mem::transmute_copy;
use std::ops::{Deref, DerefMut};
use fxhash::FxHashMap;
use crate::allocator::{ManualAllocatorHolder, ManualArenaAllocatorRef};
use crate::world::ThreadLocalWorld;


#[derive(Copy, Clone, PartialEq, Eq, Hash)]
pub struct BlockPosition {
    pub x: i32,
    pub y: i32,
    pub z: i32
}

impl BlockPosition {

    pub fn new(x: i32, y: i32, z: i32) -> Self {
        return Self {
            x,
            y,
            z
        }
    }

}


pub struct Node {
    pub position: BlockPosition,
    pub is_closed: bool,
    pub actual_cost: i32,
    pub estimated_cost: i32,
    pub score: i32,
    pub origin: Option<NodeRef>
}

pub struct NodeRef {
    node: *mut Node
}

impl NodeRef {

    pub fn new(allocator: ManualArenaAllocatorRef, position: &BlockPosition, actual_cost: i32, estimated_cost: i32, origin: Option<NodeRef>) -> Self {
        let node = Node {
            position: position.clone(),
            is_closed: false,
            actual_cost,
            estimated_cost,
            score: actual_cost + estimated_cost,
            origin
        };
        return Self { node: Box::into_raw(Box::new_in(node, allocator)) }
    }

}

impl Eq for NodeRef {}

impl PartialEq<Self> for NodeRef {
    fn eq(&self, other: &Self) -> bool {
        return self.node as usize == other.node as usize
    }
}

impl PartialOrd<Self> for NodeRef {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        return Some(self.cmp(other))
    }
}

impl Ord for NodeRef {
    fn cmp(&self, other: &Self) -> Ordering {
        let self_node = self.deref();
        let other_node = other.deref();

        let score_cmp = self_node.score.cmp(&other_node.score);
        if score_cmp != Ordering::Equal {
            return score_cmp;
        }
        let estimated_cost_cmp = self_node.estimated_cost.cmp(&other_node.estimated_cost);
        if estimated_cost_cmp != Ordering::Equal {
            return estimated_cost_cmp;
        }
        return self_node.actual_cost.cmp(&other_node.actual_cost);
    }
}

impl Deref for NodeRef {
    type Target = Node;

    fn deref(&self) -> &Self::Target {
        return unsafe { &*self.node }
    }
}

impl DerefMut for NodeRef {
    fn deref_mut(&mut self) -> &mut Self::Target {
        return unsafe { &mut *self.node }
    }
}

impl Clone for NodeRef {
    fn clone(&self) -> Self {
        return unsafe { transmute_copy(self) }
    }
}




pub fn run_pathfinding(
    allocator_holder: ManualAllocatorHolder,
    world: &mut ThreadLocalWorld,
    start: &BlockPosition,
    goal: &BlockPosition,
    descending_height: i32,
    jump_height: i32,
    max_iteration: usize
) -> Vec<BlockPosition, ManualArenaAllocatorRef> {
    let allocator = allocator_holder.allocator;

    if start.eq(goal) {
        return Vec::new_in(allocator);
    }

    let mut snorted_node_set = BTreeSet::<NodeRef, ManualArenaAllocatorRef>::new_in(allocator.clone());
    let mut node_map = unsafe { &mut *allocator_holder.cached_map };

    //Open first position node
    let mut start_node = open_node(allocator.clone(), None, start, goal, node_map);
    start_node.is_closed = true;

    //Current node
    let mut current_node = start_node.clone();

    //Nearest node
    let mut nearest_node = current_node.clone();

    //Iteration count
    let mut iteration = 0;

    //Start pathfinding
    loop {
        iteration += 1;

        //Max iteration check
        if iteration >= max_iteration {
            //Give up!
            let mut paths: Vec<BlockPosition, ManualArenaAllocatorRef> = Vec::new_in(allocator.clone());
            paths.push(nearest_node.position.clone());
            get_paths(&nearest_node, &mut paths);
            paths.reverse();

            return paths;
        }

        let neighbours = current_node.get_neighbours(allocator.clone(), descending_height, jump_height, world);

        for block_position in neighbours.iter() {
            //Check if closed
            let new_node = open_node(allocator.clone(), Some(current_node.clone()), block_position, goal, &mut node_map);
            if new_node.is_closed {
                continue;
            }

            //Update nearest node
            if new_node.estimated_cost < nearest_node.estimated_cost {
                nearest_node = new_node.clone();
            }

            snorted_node_set.insert(new_node);
        }

        //Close node
        current_node.is_closed = true;
        snorted_node_set.remove(&current_node);

        //Choose next node
        current_node = match snorted_node_set.first() {
            None => {
                //I couldn't find a path...
                let mut paths = Vec::<BlockPosition, ManualArenaAllocatorRef>::new_in(allocator);
                paths.push(nearest_node.position.clone());
                get_paths(&nearest_node, &mut paths);
                paths.reverse();

                return paths;
            }
            Some(first) => first.clone()
        };

        //Check goal
        if current_node.position.eq(goal) {
            let mut paths = Vec::<BlockPosition, ManualArenaAllocatorRef>::new_in(allocator);
            paths.push(current_node.position.clone());
            get_paths(&current_node, &mut paths);
            paths.reverse();

            return paths;
        }
    }
}


fn get_paths(node: &NodeRef, paths: &mut Vec<BlockPosition, ManualArenaAllocatorRef>) {
    let origin = &node.origin;
    match origin {
        None => return,
        Some(origin) => {
            paths.push(origin.position.clone());
            get_paths(origin, paths);
        }
    }
}

fn open_node(
    allocator: ManualArenaAllocatorRef,
    origin: Option<NodeRef>,
    block_position: &BlockPosition,
    goal: &BlockPosition,
    node_map: &mut FxHashMap<BlockPosition, NodeRef>
) -> NodeRef {

    let node_data = node_map.get(block_position);
    if let Some(node) = node_data {
        return node.clone();
    }

    // Calculate actual cost
    let actual_cost = match &origin {
        Some(origin) => origin.actual_cost + 1,
        _ => 0
    };

    // Calculate estimated cost
    let estimated_cost = (goal.x - block_position.x).abs() + (goal.y - block_position.y).abs() + (goal.z - block_position.z).abs();

    let node = NodeRef::new(allocator, block_position, actual_cost, estimated_cost, origin);
    node_map.insert(block_position.clone(), node.clone());

    return node;
}


impl Node {
    pub fn get_neighbours(&self, allocator: ManualArenaAllocatorRef, down: i32, up: i32, world: &mut ThreadLocalWorld) -> Vec<BlockPosition, ManualArenaAllocatorRef> {
        let mut neighbour = Vec::<BlockPosition, ManualArenaAllocatorRef>::new_in(allocator);
        
        let position = self.position.clone();

        let p1 = self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x + 1, position.y, position.z), world);
        let p2 = self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x, position.y, position.z + 1), world);
        let p3 = self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x - 1, position.y, position.z), world);
        let p4 = self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x, position.y, position.z - 1), world);

        if p1 || p2 {
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x + 1, position.y, position.z + 1), world);
        }
        if p2 || p3 {
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x - 1, position.y, position.z + 1), world);
        }
        if p3 || p4 {
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x - 1, position.y, position.z - 1), world);
        }
        if p4 || p1 {
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x + 1, position.y, position.z - 1), world);
        }


        for uh in 1..up+1 {
            if !is_traversable(world, position.x, position.y + uh, position.z) {
                break;
            }
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x + 1, position.y + uh, position.z), world);
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x, position.y + uh, position.z + 1), world);
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x - 1, position.y + uh, position.z), world);
            self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x, position.y + uh, position.z - 1), world);
        }

        let mut da = true;
        let mut db = true;
        let mut dc = true;
        let mut dd = true;
        for dy in 1..down+1 {
            if da {
                if is_traversable(world, position.x + 1, position.y - dy + 1, position.z) {
                    self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x + 1, position.y - dy, position.z), world);
                }else {
                    da = false;
                }
            }
            if db {
                if is_traversable(world, position.x, position.y - dy + 1, position.z + 1) {
                    self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x, position.y - dy, position.z + 1), world);
                }else {
                    db = false;
                }
            }
            if dc {
                if is_traversable(world, position.x - 1, position.y - dy + 1, position.z) {
                    self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x - 1, position.y - dy, position.z), world);
                }else {
                    dc = false;
                }
            }
            if dd {
                if is_traversable(world, position.x, position.y - dy + 1, position.z - 1) {
                    self.add_if_can_stand(&mut neighbour, BlockPosition::new(position.x, position.y - dy, position.z - 1), world);
                }else {
                    dd = false;
                }
            }
        }

        return neighbour;
    }

    #[inline(always)]
    pub fn add_if_can_stand(&self, positions: &mut Vec<BlockPosition, ManualArenaAllocatorRef>, position: BlockPosition, world: &mut ThreadLocalWorld) -> bool {
        return if can_stand(world, position.x, position.y, position.z) {
            positions.push(position);
            true
        } else {
            false
        }
    }

}

#[inline(always)]
pub fn can_stand(world: &mut ThreadLocalWorld, x: i32, y: i32, z: i32) -> bool{
    let chunk = world.get_chunk(x >> 4, z >> 4);

    let block1 = chunk.get_block(x, y, z);
    let block2 = chunk.get_block(x, y + 1, z);
    let block3 = chunk.get_block(x, y - 1, z);

    return block1.is_replaceable() && block2.is_replaceable() && !block3.is_replaceable()
}

#[inline(always)]
pub fn is_traversable(world: &mut ThreadLocalWorld, x: i32, y: i32, z: i32) -> bool{
    let chunk = world.get_chunk(x >> 4, z >> 4);

    let block1 = chunk.get_block(x, y, z);
    let block2 = chunk.get_block(x, y + 1, z);

    return block1.is_replaceable() && block2.is_replaceable()
}