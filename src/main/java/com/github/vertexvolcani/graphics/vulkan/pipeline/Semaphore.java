package com.github.vertexvolcani.graphics.vulkan.pipeline;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
/**
 * A class representing a Vulkan semaphore.
 * Semaphores are synchronization primitives used in Vulkan to coordinate operations between different queues.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Semaphore extends LibCleanable {
    /** The Vulkan device associated with this semaphore. */
    private final Device device;
    /** The handle to the Vulkan semaphore. */
    private final LongBuffer handle;
    private final long handle_deref;
    /**
     * Constructs a new Semaphore instance associated with the given Vulkan device.
     *
     * @param device_in The Vulkan device to associate with the semaphore.
     */
    public Semaphore(@Nonnull Device device_in) {
        super();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            handle = MemoryUtil.memAllocLong(1);
            device = device_in;
            VkSemaphoreCreateInfo pCreateInfo = VkSemaphoreCreateInfo.calloc(stack);
            pCreateInfo.sType$Default().pNext(NULL).flags(0);
            if(device.createSemaphore(pCreateInfo, handle) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: could not create semaphore");
                throw new IllegalStateException("could not create semaphore");
            }
            handle_deref = handle.get(0);
        }
    }

    /**
     * Gets the handle to the Vulkan semaphore.
     *
     * @return The handle to the Vulkan semaphore.
     */
    public long getSemaphore() {
        return handle_deref;
    }


    /**
     * Gets the handle pointer to the Vulkan semaphore.
     *
     * @return The handle pointer to the Vulkan semaphore.
     */
    public LongBuffer getSemaphorePtr() {
        return handle;
    }

    /**
     * Cleans up resources associated with the semaphore.
     * This method should be called when the semaphore is no longer needed to release Vulkan resources.
     */
    @Override
    public final void free() {
        device.deviceWaitIdle();
        device.destroySemaphore(handle_deref);
        MemoryUtil.memFree(handle);
    }
}
