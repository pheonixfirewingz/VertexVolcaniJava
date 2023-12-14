package com.github.vertexvolcani.util;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
public abstract class LibCleanable implements AutoCloseable{
    protected abstract void free();
    @Override
    public final void close() {
            free();
    }
}
