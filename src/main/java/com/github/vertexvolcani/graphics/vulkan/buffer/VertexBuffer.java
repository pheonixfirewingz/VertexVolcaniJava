package com.github.vertexvolcani.graphics.vulkan.buffer;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.vulkan.VmaAllocator;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class VertexBuffer extends Buffer{
    /**
     * Constructs a new ABuffer instance.
     *
     * @param allocator_in The Vulkan Memory Allocator.
     * @param size         The size of the buffer.
     * @param sharing_mode The sharing mode.
     * @param vma_usage    The vma usage flags.
     */
    public VertexBuffer(VmaAllocator allocator_in, long size, boolean sharing_mode, VmaMemoryUsage vma_usage) {
        super(allocator_in, size, sharing_mode, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, vma_usage.getMemoryType());
    }
}
