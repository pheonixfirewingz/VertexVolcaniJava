package com.github.vertexvolcani.graphics.vulkan.buffer;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.vulkan.VmaAllocator;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
/* @author Luke Shore
 * @version 1.0
 * @since 2023-12-14
 */
public class UniformBuffer extends Buffer{
    /**
     * Constructs a new ABuffer instance.
     *
     * @param allocator_in The Vulkan Memory Allocator.
     * @param size         The size of the buffer.
     * @param sharing_mode The sharing mode.
     * @param vma_usage    The vma usage flags.
     */
    public UniformBuffer(VmaAllocator allocator_in, long size, boolean sharing_mode, VmaMemoryUsage vma_usage) {
        super(allocator_in, size, sharing_mode, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, vma_usage.getMemoryType());
    }
}
