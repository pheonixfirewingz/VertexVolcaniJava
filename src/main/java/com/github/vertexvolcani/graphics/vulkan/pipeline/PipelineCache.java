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
import com.github.vertexvolcani.util.Nonnull;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan Pipeline Cache used for caching pipeline objects.
 *
 * <p>This class manages the creation and destruction of Vulkan pipeline caches.</p>
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-07
 */
public final class PipelineCache extends LibCleanable {
    /**
     * The handle to the Vulkan pipeline cache.
     */
    private final DeviceHandle handle;

    /**
     * Creates a new PipelineCache object for the specified Vulkan device.
     *
     * @param device_in The Vulkan device associated with the pipeline cache.
     */
    public PipelineCache(@Nonnull Device device_in) {
        try(VkPipelineCacheCreateInfo.Buffer pCreateInfo = VkPipelineCacheCreateInfo.calloc(1)) {
            pCreateInfo.sType$Default();
            handle = device_in.createPipelineCache(pCreateInfo.get(0));
            if (device_in.getResult() != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create pipeline cache");
                throw new IllegalStateException("failed to create pipeline cache");
            }
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: created pipeline cache");
    }

    /**
     * Retrieves the Vulkan handle of the pipeline cache.
     *
     * @return The Vulkan handle of the pipeline cache.
     */
    public DeviceHandle getPipelineCache() {
        return handle;
    }

    /**
     * Closes and destroys the pipeline cache.
     * <p>
     * This method releases Vulkan resources associated with the pipeline cache.
     * </p>
     */
    @Override
    protected void free() {
        handle.device().destroyPipelineCache(handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: destroyed pipeline cache");
    }
}