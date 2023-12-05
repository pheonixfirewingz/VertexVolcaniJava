package com.github.vertexvolcani.util;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public abstract class CleanerObject implements AutoCloseable {
    public CleanerObject() {}


    public abstract void cleanup();

    @Override
    public final void close() throws Exception {
        try {
            cleanup();
        } catch (Exception ignored) {}
    }
}
