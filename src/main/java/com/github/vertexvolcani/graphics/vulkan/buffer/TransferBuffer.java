package com.github.vertexvolcani.graphics.vulkan.buffer;

import com.github.vertexvolcani.graphics.vulkan.VmaAllocator;
import org.lwjgl.system.NativeType;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class TransferBuffer extends Buffer{
    /**
     * Constructs a new ABuffer instance.
     *
     * @param allocator_in The Vulkan Memory Allocator.
     * @param size         The size of the buffer.
     * @param sharing_mode The sharing mode.
     * @param to_gpu        should buffer move data to or from the gpu.
     */
    public TransferBuffer(VmaAllocator allocator_in, long size, boolean sharing_mode, @NativeType("VkBufferUsageFlags") int usage, boolean to_gpu) {
        super(allocator_in, size, sharing_mode, to_gpu ? usage | VK_BUFFER_USAGE_TRANSFER_SRC_BIT: usage | VK_BUFFER_USAGE_TRANSFER_DST_BIT, to_gpu?VMA_MEMORY_USAGE_CPU_ONLY:VMA_MEMORY_USAGE_GPU_ONLY);
    }
}
