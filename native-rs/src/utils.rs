
#[inline(always)]
pub fn get_chunk_index(chunk_x: i32, chunk_z: i32) -> i64 {
    return ((chunk_x as i64) << 32) | ((chunk_z as i64) & 0xffffffff);
}

pub fn get_palette_index(x: i32, y: i32, z: i32) -> usize {
    let x = x & 0xF;
    let y = y & 0xF;
    let z = z & 0xF;
    let index = y << 8 | z << 4 | x;
    return index as usize;
}