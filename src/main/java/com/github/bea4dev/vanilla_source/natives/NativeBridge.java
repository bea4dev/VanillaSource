package com.github.bea4dev.vanilla_source.natives;

public class NativeBridge {

    static native int getVersion();
    static native void createRegistry();
    static native void registerWorld(int levelId, int height, int minY);
    static native void registerChunk(int levelId, int chunkX, int chunkZ, int chunkY, int[] blocks);
    static native long createThreadLocalRegistry();
    static native int[] runPathfinding(long threadLocalRegistryId, int levelId, int[] args);

}
