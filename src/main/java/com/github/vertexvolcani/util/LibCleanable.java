package com.github.vertexvolcani.util;

public abstract class LibCleanable implements AutoCloseable{
    protected abstract void free();
    @Override
    public final void close() {
            free();
    }
}
