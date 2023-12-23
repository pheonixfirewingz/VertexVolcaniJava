package com.github.vertexvolcani.graphics.vulkan.buffer;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.graphics.vulkan.VmaAllocator;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;
/**
 * A class representing a Vulkan buffer managed by Vulkan Memory Allocator (VMA).
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Buffer extends LibCleanable {
    /**
     * The Vulkan Memory Allocator used for managing the buffer.
     */
    private final VmaAllocator allocator;

    /**
     * The handle to the Vulkan buffer.
     */
    private final DeviceHandle handle;

    /**
     * The handle to the memory allocation associated with the buffer.
     */
    private final long allocation;
    /**
     *  stores the data object count;
     */
    private final long size;

    /**
     * Constructs a new ABuffer instance.
     * @param allocator_in The Vulkan Memory Allocator.
     * @param size_in         The size of the buffer.
     * @param sharing_mode        The sharing mode.
     * @param usage        The buffer usage flags.
     * @param vma_usage    The vma usage flags.
     */
    public Buffer(VmaAllocator allocator_in, @NativeType("VkDeviceSize") long size_in, boolean sharing_mode, @NativeType("VkBufferUsageFlags") int usage, @NativeType("VmaMemoryUsage") int vma_usage) {
        allocator = allocator_in;
        size = size_in;
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
            handle = new DeviceHandle(allocator.getDev(),pBuffer.get(0));
            allocation = pAllocation.get(0);
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: Created buffer with VMA. Buffer size: " + size_in + " bytes");
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
    public Buffer write(ByteBuffer src) {
        final var buffer = map();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src),MemoryUtil.memAddress(buffer),src.remaining());
        unmap();
        return this;
    }
    /**
     * transfer data to buffer
     * @param src this is the pointer to the data to be copied
     */
    public Buffer write(FloatBuffer src) {
        final var buffer = map();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src),MemoryUtil.memAddress(buffer), (long) src.remaining() * Float.BYTES);
        unmap();
        return this;
    }
    /**
     * transfer data to buffer
     * @param src this is the pointer to the data to be copied
     */
    public Buffer write(IntBuffer src) {
        final var buffer = map();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src),MemoryUtil.memAddress(buffer), (long) src.remaining() * Integer.BYTES);
        unmap();
        return this;
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
            handle.device().getBufferMemoryRequirements(handle, memory_requirements);
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
    public DeviceHandle getBuffer() {
        return handle;
    }
    /**
     * Cleans up resources associated with the buffer.
     */
    @Override
    public final void free() {
        handle.device().waitIdle();
        vmaDestroyBuffer(allocator.getVmaAllocator(), handle.handle(), allocation);
        Log.print(Log.Severity.DEBUG, "Vulkan: Done freeing buffer");
    }
}