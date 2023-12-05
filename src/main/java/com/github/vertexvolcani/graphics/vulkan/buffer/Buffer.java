package com.github.vertexvolcani.graphics.vulkan.buffer;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.VmaAllocator;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * A class representing a Vulkan buffer managed by Vulkan Memory Allocator (VMA).
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Buffer extends CleanerObject {
    /**
     * The Vulkan device associated with this buffer.
     */
    private final Device device;
    /**
     * The Vulkan Memory Allocator used for managing the buffer.
     */
    private final VmaAllocator allocator;

    /**
     * The handle to the Vulkan buffer.
     */
    private final long handle;

    /**
     * The handle to the memory allocation associated with the buffer.
     */
    private final long allocation;
    /**
     *  stores the data object count;
     */
    private long size;

    /**
     * Constructs a new ABuffer instance.
     *
     * @param device_in    The Vulkan device associated with this buffer.
     * @param allocator_in The Vulkan Memory Allocator.
     * @param size         The size of the buffer.
     * @param sharing_mode        The sharing mode.
     * @param usage        The buffer usage flags.
     * @param vma_usage    The vma usage flags.
     */
    public Buffer(Device device_in, VmaAllocator allocator_in, @NativeType("VkDeviceSize") long size, boolean sharing_mode, @NativeType("VkBufferUsageFlags") int usage, @NativeType("VmaMemoryUsage") int vma_usage) {
        super();
        device = device_in;
        allocator = allocator_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.callocLong(1);
            PointerBuffer pAllocation = stack.callocPointer(1);

            VkBufferCreateInfo buffer_create_info = VkBufferCreateInfo.calloc(stack)
                    .sType$Default().pNext(0).flags(0).usage(usage)
                    .sharingMode(sharing_mode? VK_SHARING_MODE_CONCURRENT:VK_SHARING_MODE_EXCLUSIVE).size(size);

            VmaAllocationCreateInfo allocation_create_info = VmaAllocationCreateInfo.calloc(stack);
            allocation_create_info.usage(vma_usage);

            if (vmaCreateBuffer(allocator.getVmaAllocator(), buffer_create_info, allocation_create_info, pBuffer, pAllocation, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create Vulkan buffer with VMA.");
                throw new RuntimeException("Failed to create Vulkan buffer with VMA.");
            }
            handle = pBuffer.get(0);
            allocation = pAllocation.get(0);
        }
    }

    /**
     * Maps the buffer memory into the application's address space.
     *
     * @return A mapped ByteBuffer.
     */
    private ByteBuffer map() {
            PointerBuffer ppData = MemoryUtil.memCallocPointer(1);
            vmaMapMemory(allocator.getVmaAllocator(), allocation, ppData);
            ByteBuffer buffer = ppData.getByteBuffer(0, (int) getMemorySize());
            MemoryUtil.memFree(ppData);
            return buffer;
    }
    /**
     * transfer data to buffer
     * @param src this is the pointer to the data to be copied
     */
    public void load(ByteBuffer src) {
        final var buffer = map();
        size = src.remaining();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src),MemoryUtil.memAddress(buffer),src.remaining());
        unmap();
    }
    /**
     * Unmaps the buffer memory.
     */
    private void unmap() {
        vmaUnmapMemory(allocator.getVmaAllocator(), allocation);
    }

    /**
     * Gets the size of the buffer memory.
     *
     * @return The size of the buffer memory.
     */
    protected long getMemorySize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements memory_requirements = VkMemoryRequirements.calloc(stack);
            device.getBufferMemoryRequirements(handle, memory_requirements);
            return memory_requirements.size();
        }
    }

    /**
     * Gets the size of the buffer.
     *
     * @return The size of the buffer.
     */
    public long getSize() {
        return size;
    }

    /**
     * returns the buffer handle
     * @return VkBuffer handle of the vulkan object
     */
    public long getBuffer() {
        return handle;
    }

    /**
     * Cleans up resources associated with the buffer.
     */
    @Override
    public final void cleanup() {
        device.deviceWaitIdle();
        vmaDestroyBuffer(allocator.getVmaAllocator(), handle, allocation);
    }
}
