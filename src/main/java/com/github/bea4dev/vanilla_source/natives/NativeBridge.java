package com.github.bea4dev.vanilla_source.natives;

public class NativeBridge {

    static native int getVersion();
    static native void createGlobalRegistry();
    static native void registerWorld(int levelId, int height, int minY);
    static native void registerChunk(int levelId, int chunkX, int chunkZ, int[] blocks);
    static native long createThreadLocalRegistry();
    static native void unregisterChunk(long threadLocalRegistryAddress, int levelId, int chunkX, int chunkZ);
    static native int[] runPathfinding(long threadLocalRegistryAddress, int levelId, int[] args);

}
