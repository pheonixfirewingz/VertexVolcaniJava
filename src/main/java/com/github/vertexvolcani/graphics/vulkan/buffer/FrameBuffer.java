package com.github.vertexvolcani.graphics.vulkan.buffer;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.RenderPass;
import com.github.vertexvolcani.graphics.vulkan.Surface;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;

/**
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-05
 */
public class FrameBuffer extends CleanerObject {
    /**
     * The Vulkan device associated with this frame buffer.
     */
    private final Device device;
    /**
     * The handle to the Vulkan frame buffer.
     */
    private final long handle;

    //FIXME: update when add images
    public FrameBuffer(Device device_in, RenderPass render_pass, long image_view, Surface surface) {
        device = device_in;
        VkExtent2D currentExtent = surface.getSurfaceSize();
        handle = create(currentExtent.width(),currentExtent.height(),render_pass,image_view);
    }

    public FrameBuffer(Device device_in, int width, int height, RenderPass render_pass, long image_view) {
        device = device_in;
        handle = create(width,height,render_pass,image_view);
    }

    private long create(int width, int height, RenderPass render_pass, long image_view) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            LongBuffer attachments = stack.callocLong(1);
            VkFramebufferCreateInfo frame_buffer_create_info = VkFramebufferCreateInfo.calloc(stack).sType$Default().pAttachments(attachments)
                    .height(height).width(width).layers(1).renderPass(render_pass.getRenderPass());
            attachments.put(0, image_view);
            if (device.createFramebuffer(frame_buffer_create_info, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Failed to create frame buffer");
                throw new IllegalStateException("Failed to create frame buffer");
            }
            return buffer.get(0);
        }
    }

    public long getFrameBuffer() {
        return handle;
    }

    @Override
    public void cleanup() {
        device.destroyFramebuffer(handle);
    }
}