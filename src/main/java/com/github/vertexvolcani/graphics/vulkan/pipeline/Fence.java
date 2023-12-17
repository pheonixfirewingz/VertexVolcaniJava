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
import org.lwjgl.vulkan.VkFenceCreateInfo;

import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;

/**
 * The Fence class represents a synchronization primitive in Vulkan graphics API.
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Fence extends LibCleanable {
    /**
     * The handle to the Vulkan fence object.
     */
    private final DeviceHandle handle;

    /**
     * Constructs a new Fence object for the specified Vulkan device.
     *
     * @param device_in    The Vulkan device associated with this fence.
     * @param pre_signaled should the fence be created as pre-signaled on creation
     */
    public Fence(@Nonnull Device device_in, boolean pre_signaled) {
        try(VkFenceCreateInfo.Buffer pCreateInfo = VkFenceCreateInfo.calloc(1)) {
            long pBuffer = 0;
            pCreateInfo.sType$Default().pNext(0);
            if (pre_signaled) {
                pCreateInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);
            } else {
                pCreateInfo.flags(0);
            }
            // Create the Vulkan fence object
            handle = device_in.createFence(pCreateInfo.get(0));
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create fence");
                throw new IllegalStateException("could not create fence");
            }
        }
    }

    /**
     * Waits for the fence to become signaled.
     */
    public void waitFor() {
        handle.device().waitForFences(handle, true, Long.MAX_VALUE);
    }

    /**
     * Resets the fence to the un-signaled state.
     */
    public void reset() {
        handle.device().resetFences(handle);
    }

    /**
     * Gets the status of the fence.
     */
    public void getStatus() {
        handle.device().getFenceStatus(handle);
    }

    /**
     * Gets the handle to the Vulkan fence object.
     *
     * @return The handle to the Vulkan fence.
     */
    public DeviceHandle getFence() {
        return handle;
    }

    /**
     * Cleans up and destroys the Vulkan fence object when it is no longer needed.
     */
    @Override
    public final void free() {
        handle.device().waitIdle();
        handle.device().destroyFence(handle);
    }
}