package com.github.vertexvolcani.graphics.vulkan.buffer;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.graphics.vulkan.Image;
import com.github.vertexvolcani.graphics.vulkan.pipeline.RenderPass;
import com.github.vertexvolcani.graphics.vulkan.Surface;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;

/**
 * Represents a Vulkan framebuffer used for rendering.
 *
 * <p>This class encapsulates the creation and management of Vulkan framebuffers.</p>
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-05
 */
public class FrameBuffer extends LibCleanable {
    /**
     * The handle to the Vulkan frame buffer.
     */
    private final DeviceHandle handle;

    // FIXME: update when adding images
    /**
     * Creates a FrameBuffer instance with the specified parameters.
     *
     * @param device_in    The Vulkan device.
     * @param render_pass  The Vulkan render pass.
     * @param image_views   The Vulkan list of image view.
     * @param surface      The Vulkan surface.
     */
    public FrameBuffer(Device device_in, RenderPass render_pass, Image[] image_views, Surface surface) {
        VkExtent2D currentExtent = surface.getSurfaceSize();
        handle = new DeviceHandle(device_in, create(device_in, currentExtent.width(), currentExtent.height(), render_pass, image_views));
    }

    /**
     * Creates a FrameBuffer instance with the specified parameters.
     *
     * @param device_in    The Vulkan device.
     * @param width        The width of the framebuffer.
     * @param height       The height of the framebuffer.
     * @param render_pass  The Vulkan render pass.
     * @param image_views   The Vulkan list of image view.
     */
    public FrameBuffer(Device device_in, int width, int height, RenderPass render_pass,Image[] image_views) {
        handle = new DeviceHandle(device_in, create(device_in, width, height, render_pass, image_views));
    }

    private long create(Device device_in, int width, int height, RenderPass render_pass, Image[] image_views) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            LongBuffer attachments = stack.callocLong(1);
            VkFramebufferCreateInfo frame_buffer_create_info = VkFramebufferCreateInfo.calloc(stack).sType$Default().pAttachments(attachments)
                    .height(height).width(width).layers(1).renderPass(render_pass.getRenderPass());
            for (int i = 0; i < image_views.length; i++) {
                attachments.put(i, image_views[i].getImageView());
            }
            if (device_in.createFramebuffer(frame_buffer_create_info, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Failed to create frame buffer");
                throw new IllegalStateException("Failed to create frame buffer");
            }
            return buffer.get(0);
        }
    }
    /**
     * Gets the Vulkan handle of the frame buffer.
     *
     * @return The Vulkan handle of the frame buffer.
     */
    public long getFrameBuffer() {
        return handle.handle();
    }
    /**
     * Closes the frame buffer, destroying associated Vulkan resources.
     */
    @Override
    public final void free() {
        handle.device().destroyFramebuffer(handle.handle());
    }
}
