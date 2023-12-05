package com.github.vertexvolcani.graphics.vulkan;

import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
/**
 * Represents a Vulkan render pass.
 * A render pass describes a sequence of rendering commands that are executed in a specific order.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public class RenderPass extends CleanerObject {
    /**
     * The Vulkan device associated with this render pass.
     */
    private final Device device;
    /**
     * The handle to the Vulkan render pass.
     */
    private final long handle;
    /**
     * Constructs a new RenderPass object.
     *
     * @param device_in The Vulkan device associated with this render pass.
     */
    public RenderPass(@Nonnull Device device_in, /*temp*/ VkRenderPassCreateInfo pCreateInfo) {
        super();
        device = device_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
           /* VkRenderPassCreateInfo pCreateInfo = VkRenderPassCreateInfo.calloc(stack);
            pCreateInfo.sType$Default();*/

            if(device.createRenderPass(pCreateInfo,buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: could not create Render pass");
                throw new IllegalStateException("could not create Render pass");
            }
            handle = buffer.get(0);
        }
    }
    /**
     * Gets the handle of the Vulkan render pass.
     *
     * @return The handle of the Vulkan render pass.
     */
    public long getRenderPass() {
        return handle;
    }
    /**
     * Cleans up and destroys the Vulkan render pass.
     */
    @Override
    public void cleanup() {
        device.deviceWaitIdle();
        device.destroyRenderPass(handle);
    }
}