package com.github.vertexvolcani.graphics.vulkan.pipeline;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
/**
 * A class representing a Vulkan semaphore.
 * Semaphores are synchronization primitives used in Vulkan to coordinate operations between different queues.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Semaphore extends LibCleanable {
    /** The handle to the Vulkan semaphore. */
    private final DeviceHandle handle;
    /**
     * Constructs a new Semaphore instance associated with the given Vulkan device.
     *
     * @param device_in The Vulkan device to associate with the semaphore.
     */
    public Semaphore(@Nonnull Device device_in) {
        try (VkSemaphoreCreateInfo.Buffer pCreateInfo = VkSemaphoreCreateInfo.calloc(1)) {
            pCreateInfo.sType$Default().pNext(0).flags(0);
            handle = device_in.createSemaphore(pCreateInfo.get(0));
            if(device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR,"Vulkan: could not create semaphore");
                throw new IllegalStateException("could not create semaphore");
            }
        }
    }

    /**
     * Gets the handle to the Vulkan semaphore.
     *
     * @return The handle to the Vulkan semaphore.
     */
    public DeviceHandle getSemaphore() {
        return handle;
    }

    /**
     * Cleans up resources associated with the semaphore.
     * This method should be called when the semaphore is no longer needed to release Vulkan resources.
     */
    @Override
    public final void free() {
        handle.device().waitIdle();
        handle.device().destroySemaphore(handle);
    }
}
