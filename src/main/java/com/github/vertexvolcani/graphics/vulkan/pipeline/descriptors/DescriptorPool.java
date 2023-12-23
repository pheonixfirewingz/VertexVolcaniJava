package com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors;
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
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

/**
 * Represents a Vulkan descriptor pool.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-20
 */
public final class DescriptorPool extends LibCleanable {
    /**
     * The handle to the Vulkan descriptor pool.
     */
    private final DeviceHandle handle;

    /**
     * Constructs a DescriptorPool object.
     *
     * @param device_in  the Vulkan device
     * @param poolSizes  an array of descriptor pool sizes
     * @param maxSets    the maximum number of descriptor sets
     * @param flags      flags for the descriptor pool creation
     */
    public DescriptorPool(Device device_in, DescriptorPoolSize[] poolSizes, int maxSets, int flags) {
        try (VkDescriptorPoolCreateInfo.Buffer poolCreateInfo = VkDescriptorPoolCreateInfo.calloc(1)) {
            poolCreateInfo.sType$Default();
            poolCreateInfo.flags(flags);
            poolCreateInfo.maxSets(maxSets);
            try (VkDescriptorPoolSize.Buffer pPoolSizes = VkDescriptorPoolSize.calloc(poolSizes.length)) {
                for (int i = 0; i < poolSizes.length; i++) {
                    pPoolSizes.get(i).type(poolSizes[i].type().getDescriptorType()).descriptorCount(poolSizes[i].size());
                }
                poolCreateInfo.pPoolSizes(pPoolSizes);
                handle = device_in.createDescriptorPool(poolCreateInfo.get(0));
            }
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create descriptor pool");
                throw new IllegalStateException("Failed to create descriptor pool");
            }
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: Created descriptor pool");
    }

    /**
     * Retrieves the handle of the descriptor pool.
     * @return the descriptor pool handle
     */
    public DeviceHandle getHandle() {
        return handle;
    }

    /**
     * Closes the descriptor pool, destroying associated Vulkan resources.
     */
    @Override
    protected void free() {
        handle.device().destroyDescriptorPool(handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing descriptor pool");
    }
}