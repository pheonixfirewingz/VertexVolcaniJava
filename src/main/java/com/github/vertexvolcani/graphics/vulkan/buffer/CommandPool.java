package com.github.vertexvolcani.graphics.vulkan.buffer;
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
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents a Vulkan command pool used for allocating command buffers.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-04
 */
public class CommandPool extends LibCleanable {
    /**
     * The handle to the Vulkan command pool.
     */
    private final DeviceHandle handle;
    /**
     * Constructs a new CommandPool associated with the specified device.
     *
     * @param device_in          The Vulkan device associated with the command pool.
     * @param queue_node_index   The index of the queue family to which the command pool belongs.
     * @param reset_able          Is command pool reset-able.
     */
    public CommandPool(Device device_in, int queue_node_index, boolean reset_able) {
        try (VkCommandPoolCreateInfo.Buffer cmdPoolInfo = VkCommandPoolCreateInfo.calloc(1)) {
            cmdPoolInfo.sType$Default().queueFamilyIndex(queue_node_index).flags(reset_able ? VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT:0);
            handle = device_in.createCommandPool(cmdPoolInfo.get(0));
            if (device_in.getResult() != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create command pool");
                throw new IllegalStateException("failed to create command pool");
            }
        }
    }

    /**
     * Retrieves the Vulkan handle of the command pool.
     *
     * @return The Vulkan handle of the command pool.
     */
    public DeviceHandle getCommandPool() {
        return handle;
    }

    /**
     * Resets the command pool, freeing all previously allocated command buffers.
     *
     * @param flags Flags specifying the reset behavior.
     * @return VK_SUCCESS if the command pool is successfully reset, or an error code otherwise.
     */
    public int reset(int flags) {
        return vkResetCommandPool(handle.device().getDevice(), handle.handle(), flags);
    }

    /**
     * Cleans up and destroys the command pool, releasing associated resources.
     */
    @Override
    public final void free() {
        handle.device().waitIdle();
        handle.device().destroyCommandPool(handle);
    }
}
