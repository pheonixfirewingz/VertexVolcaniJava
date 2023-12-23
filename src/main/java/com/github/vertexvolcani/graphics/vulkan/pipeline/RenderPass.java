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
import org.lwjgl.vulkan.VkRenderPassCreateInfo;

/**
 * Represents a Vulkan render pass.
 * A render pass describes a sequence of rendering commands that are executed in a specific order.
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public final class RenderPass extends LibCleanable {
    /**
     * The handle to the Vulkan render pass.
     */
    private final DeviceHandle handle;

    /**
     * Constructs a new RenderPass object.
     *
     * @param device_in The Vulkan device associated with this render pass.
     */
    public RenderPass(@Nonnull Device device_in, @Nonnull VkRenderPassCreateInfo pCreateInfo) {
        handle = device_in.createRenderPass(pCreateInfo);
        if (device_in.didErrorOccur()) {
            Log.print(Log.Severity.ERROR, "Vulkan: could not create Render pass");
            throw new IllegalStateException("could not create Render pass");
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: created Render pass");
    }

    /**
     * Gets the handle of the Vulkan render pass.
     *
     * @return The handle of the Vulkan render pass.
     */
    public DeviceHandle getRenderPass() {
        return handle;
    }

    /**
     * Cleans up and destroys the Vulkan render pass.
     */
    @Override
    protected void free() {
        handle.device().waitIdle();
        handle.device().destroyRenderPass(handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing render pass");
    }
}