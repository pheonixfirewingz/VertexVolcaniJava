package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

/**
 * The Fence class represents a synchronization primitive in Vulkan graphics API.
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
     * @param device_in The Vulkan device associated with this fence.
     * @param pre_signaled should the fence be created as pre-signaled on creation
     */
    public Fence(@Nonnull Device device_in, boolean pre_signaled) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            VkFenceCreateInfo pCreateInfo = VkFenceCreateInfo.calloc(stack);
            pCreateInfo.sType$Default().pNext(NULL);

            if(pre_signaled){
                pCreateInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);
            } else {
                pCreateInfo.flags(0);
            }
            // Create the Vulkan fence object
            if(device_in.createFence(pCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: could not create fence");
                throw new IllegalStateException("could not create fence");
            }
            handle = new DeviceHandle(device_in,buffer.get(0));
        }
    }
    /**
     * Waits for the fence to become signaled.
     * @return VkResult
     */
    public int waitFor() {
        return handle.device().waitForFences(handle.handle(), true, Long.MAX_VALUE);
    }
    /**
     * Resets the fence to the unsignaled state.
     * @return VkResult
     */
    public int reset() {
        return handle.device().resetFences(handle.handle());
    }
    /**
     * Gets the status of the fence.
     * @return VkResult
     */
    public int getStatus() {
        return handle.device().getFenceStatus(handle.handle());
    }
    /**
     * Gets the handle to the Vulkan fence object.
     * @return The handle to the Vulkan fence.
     */
    public long getFence() {
        return handle.handle();
    }
    /**
     * Cleans up and destroys the Vulkan fence object when it is no longer needed.
     */
    @Override
    public final void free() {
        handle.device().deviceWaitIdle();
        handle.device().destroyFence(handle.handle());
    }
}