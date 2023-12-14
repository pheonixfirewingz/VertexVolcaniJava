package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan Pipeline Cache used for caching pipeline objects.
 *
 * <p>This class manages the creation and destruction of Vulkan pipeline caches.</p>
 *
 * @author Your Name
 * @version 1.0
 * @since 2023-12-07
 */
public class PipelineCache extends LibCleanable {
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            VkPipelineCacheCreateInfo pCreateInfo = VkPipelineCacheCreateInfo.calloc(stack).sType$Default();
            if (device_in.createPipelineCache(pCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create pipeline cache");
                throw new IllegalStateException("failed to create pipeline cache");
            }
            handle = new DeviceHandle(device_in, buffer.get(0));
        }
    }

    /**
     * Retrieves the Vulkan handle of the pipeline cache.
     *
     * @return The Vulkan handle of the pipeline cache.
     */
    public long getPipelineCache() {
        return handle.handle();
    }

    /**
     * Closes and destroys the pipeline cache.
     * <p>
     * This method releases Vulkan resources associated with the pipeline cache.
     * </p>
     */
    @Override
    public final void free() {
        handle.device().destroyPipelineCache(handle.handle());
    }
}