package com.github.vertexvolcani.graphics.vulkan;

import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

/**
 * The Fence class represents a synchronization primitive in Vulkan graphics API.
 * It extends the CleanerObject class, ensuring proper cleanup when the object is no longer needed.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Fence extends CleanerObject {
    /**
     * The Vulkan device associated with this fence.
     */
    private final Device device;
    /**
     * The handle to the Vulkan fence object.
     */
    private final long handle;
    /**
     * Constructs a new Fence object for the specified Vulkan device.
     *
     * @param device_in The Vulkan device associated with this fence.
     * @param pre_signaled should the fence be created as pre-signaled on creation
     */
    public Fence(@Nonnull Device device_in, boolean pre_signaled) {
        super();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            device = device_in;

            VkFenceCreateInfo pCreateInfo = VkFenceCreateInfo.calloc(stack);
            pCreateInfo.sType$Default().pNext(NULL);

            if(pre_signaled){
                pCreateInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);
            } else {
                pCreateInfo.flags(0);
            }
            // Create the Vulkan fence object
            if(device.createFence(pCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: could not create fence");
                throw new IllegalStateException("could not create fence");
            }
            handle = buffer.get(0);
        }
    }
    /**
     * Waits for the fence to become signaled.
     * @return VkResult
     */
    public int waitFor() {
        return device.waitForFences(handle, true, Long.MAX_VALUE);
    }
    /**
     * Resets the fence to the unsignaled state.
     * @return VkResult
     */
    public int reset() {
        return device.resetFences(handle);
    }
    /**
     * Gets the status of the fence.
     * @return VkResult
     */
    public int getStatus() {
        return device.getFenceStatus(handle);
    }
    /**
     * Gets the handle to the Vulkan fence object.
     * @return The handle to the Vulkan fence.
     */
    public long getFence() {
        return handle;
    }
    /**
     * Cleans up and destroys the Vulkan fence object when it is no longer needed.
     */
    @Override
    public void cleanup() {
        device.deviceWaitIdle();
        device.destroyFence(handle);
    }
}