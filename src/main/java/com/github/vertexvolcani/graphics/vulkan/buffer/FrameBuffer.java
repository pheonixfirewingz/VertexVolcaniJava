package com.github.vertexvolcani.graphics.vulkan.buffer;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.graphics.vulkan.Image;
import com.github.vertexvolcani.graphics.vulkan.Surface;
import com.github.vertexvolcani.graphics.vulkan.pipeline.RenderPass;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
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
        handle = create(device_in, currentExtent.width(), currentExtent.height(), render_pass, image_views);
    }

    /**
     * Creates a FrameBuffer instance with the specified parameters.
     *
     * @param device_in    The Vulkan device.
     * @param width        The width of the frame buffer.
     * @param height       The height of the frame buffer.
     * @param render_pass  The Vulkan render pass.
     * @param image_views   The Vulkan list of image view.
     */
    public FrameBuffer(Device device_in, int width, int height, RenderPass render_pass,Image[] image_views) {
        handle = create(device_in, width, height, render_pass, image_views);
    }

    private DeviceHandle create(Device device_in, int width, int height, RenderPass render_pass, Image[] image_views) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer attachments = stack.callocLong(image_views.length);
            for (int i = 0; i < image_views.length; i++) {
                attachments.put(i, image_views[i].getImageView().handle());
            }
            VkFramebufferCreateInfo frame_buffer_create_info = VkFramebufferCreateInfo.calloc(stack).sType$Default().pAttachments(attachments)
                    .height(height).width(width).layers(1).renderPass(render_pass.getRenderPass().handle());
            DeviceHandle handle = device_in.createFramebuffer(frame_buffer_create_info);
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Failed to create frame buffer");
                throw new IllegalStateException("Failed to create frame buffer");
            }
            return handle;
        }
    }
    /**
     * Gets the Vulkan handle of the frame buffer.
     *
     * @return The Vulkan handle of the frame buffer.
     */
    public DeviceHandle getFrameBuffer() {
        return handle;
    }
    /**
     * Closes the frame buffer, destroying associated Vulkan resources.
     */
    @Override
    public final void free() {
        handle.device().destroyFramebuffer(handle);
    }
}
