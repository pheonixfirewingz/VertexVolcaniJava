package com.github.vertexvolcani.graphics.vulkan.buffer;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.VmaAllocator;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents a Vulkan command pool used for allocating command buffers.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-04
 */
public class CommandPool extends CleanerObject {
    /**
     * The Vulkan device associated with this command pool.
     */
    private final Device device;

    /**
     * The handle to the Vulkan command pool.
     */
    private final long handle;

    /**
     * Constructs a new CommandPool associated with the specified device.
     *
     * @param device_in          The Vulkan device associated with the command pool.
     * @param queue_node_index   The index of the queue family to which the command pool belongs.
     * @param flags              Additional creation flags for the command pool.
     */
    public CommandPool(Device device_in, int queue_node_index, int flags) {
        super();
        device = device_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType$Default().queueFamilyIndex(queue_node_index).flags(flags);
            if (device.createCommandPool(cmdPoolInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create command pool");
                throw new IllegalStateException("failed to create command pool");
            }
            handle = buffer.get(0);
        }
    }

    /**
     * Retrieves the Vulkan handle of the command pool.
     *
     * @return The Vulkan handle of the command pool.
     */
    public long getCommandPool() {
        return handle;
    }

    /**
     * Resets the command pool, freeing all previously allocated command buffers.
     *
     * @param flags Flags specifying the reset behavior.
     * @return VK_SUCCESS if the command pool is successfully reset, or an error code otherwise.
     */
    public int reset(int flags) {
        return vkResetCommandPool(device.getDevice(), handle, flags);
    }

    /**
     * Cleans up and destroys the command pool, releasing associated resources.
     */
    @Override
    public void cleanup() {
        device.deviceWaitIdle();
        device.destroyCommandPool(handle);
    }
}
