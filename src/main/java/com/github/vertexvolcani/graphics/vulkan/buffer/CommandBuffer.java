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
import com.github.vertexvolcani.graphics.vulkan.pipeline.PipelineLayout;
import com.github.vertexvolcani.graphics.vulkan.pipeline.RenderPass;
import com.github.vertexvolcani.graphics.vulkan.pipeline.ShaderType;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;

import com.github.vertexvolcani.util.Nonnull;
import com.github.vertexvolcani.util.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRDynamicRendering.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents a Vulkan command buffer.
 * This class provides a convenient Java interface for Vulkan command buffer operations.
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-04
 */
public final class CommandBuffer extends LibCleanable {
    private static final float[] colours = new float[]{0.392156863f, 0.584313725f, 0.929411765f, 1.0f};
    /**
     * The Vulkan device associated with this command buffer.
     */
    private final Device device;
    /**
     * The Vulkan command pool associated with this command pool.
     */
    private final CommandPool command_pool;
    /**
     * The handle to the Vulkan command buffer.
     */
    private final VkCommandBuffer handle;

    /**
     * Constructs a new CommandBuffer.
     *
     * @param device_in       The Vulkan device associated with the command buffer.
     * @param command_pool_in The command pool from which the command buffer is allocated.
     * @param level           The command buffer level (primary or secondary).
     */
    public CommandBuffer(Device device_in, CommandPool command_pool_in, int level) {
        super();
        device = device_in;
        command_pool = command_pool_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer buffer = stack.mallocPointer(1);
            VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack).sType$Default()
                    .commandPool(command_pool.getCommandPool().handle()).level(level).commandBufferCount(1);
            device.allocateCommandBuffers(cmdBufAllocateInfo, buffer);
            if (device.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create command buffer");
                throw new IllegalStateException("could not create command buffer");
            }
            handle = new VkCommandBuffer(buffer.get(0), device.getDevice());
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: created command buffer");
    }

    /**
     * make primary command buffer
     *
     * @param device The Vulkan device associated with the command buffer.
     * @param pool   The command pool from which the command buffer is allocated.
     * @return the created command buffer
     */
    public static CommandBuffer createPrimeryCommandBuffer(Device device, CommandPool pool) {
        return new CommandBuffer(device, pool, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
    }

    /**
     * make secondary command buffer
     *
     * @param device The Vulkan device associated with the command buffer.
     * @param pool   The command pool from which the command buffer is allocated.
     * @return the created command buffer
     */
    public static CommandBuffer createSecondaryCommandBuffer(Device device, CommandPool pool) {
        return new CommandBuffer(device, pool, VK_COMMAND_BUFFER_LEVEL_SECONDARY);
    }

    /**
     * make multiple command buffers
     *
     * @param device The Vulkan device associated with the command buffer.
     * @param pool   The command pool from which the command buffer is allocated.
     * @param count  The number of command buffers to create.
     * @param level  The command buffer level (primary or secondary).
     * @return the created command buffers
     */
    public static CommandBuffer[] createCommandBuffers(Device device, CommandPool pool, int count, int level) {
        CommandBuffer[] buffers = new CommandBuffer[count];
        for (int i = 0; i < count; i++) {
            buffers[i] = new CommandBuffer(device, pool, level);
        }
        return buffers;
    }

    /**
     *  destroy command buffers
     * @param buffers the command buffers to destroy
     */
    public static void destroyCommandBuffers(CommandBuffer[] buffers) {
        for (CommandBuffer buffer : buffers) {
            buffer.close();
        }
    }
    /**
     * Submits the command buffer to a Vulkan queue.
     *
     * @param queue The Vulkan queue to submit the command buffer to.
     * @return The result of the queue submission.
     */
    public int submit(VkQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType$Default();
            PointerBuffer pCommandBuffers = stack.mallocPointer(1).put(handle).flip();
            submitInfo.pCommandBuffers(pCommandBuffers);
            return vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
        }
    }

    /**
     * Begins recording commands into the command buffer.
     *
     * @param flags Flags specifying the behavior of the command buffer recording.
     * @return The result of the command buffer recording initiation.
     */
    public int begin(int flags) {
        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc().sType$Default().flags(flags);
        int ret = vkBeginCommandBuffer(handle, cmdBufInfo);
        cmdBufInfo.free();
        return ret;
    }

    public void beginDynamicRendering(VkRect2D renderArea, int layerCount, int viewMask, @Nullable @NativeType("VkRenderingAttachmentInfo const *") VkRenderingAttachmentInfo.Buffer colorAttachment, @Nullable @NativeType("VkRenderingAttachmentInfo const *") VkRenderingAttachmentInfo depthAttachment, @Nullable @NativeType("VkRenderingAttachmentInfo const *") VkRenderingAttachmentInfo stencilAttachment) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingInfoKHR renderingInfo = VkRenderingInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_RENDERING_INFO_KHR)
                    .renderArea(renderArea).layerCount(layerCount).viewMask(viewMask).pColorAttachments(colorAttachment)
                    .pDepthAttachment(depthAttachment).pStencilAttachment(stencilAttachment);
            vkCmdBeginRenderingKHR(handle, renderingInfo);
        }
    }

    public void endDynamicRendering() {
        vkCmdEndRenderingKHR(handle);
    }

    /**
     * Ends the recording of commands into the command buffer.
     *
     * @return The result of ending the command buffer recording.
     */
    public int end() {
        return vkEndCommandBuffer(handle);
    }

    /**
     * Binds a pipeline to the command buffer.
     *
     * @param pipelineBindPoint The bind point for the pipeline (e.g., VK_PIPELINE_BIND_POINT_GRAPHICS).
     * @param pipeline          The pipeline to bind.
     */
    public void bindPipeline(@NativeType("VkPipelineBindPoint") int pipelineBindPoint, DeviceHandle pipeline) {
        vkCmdBindPipeline(handle, pipelineBindPoint, pipeline.handle());
    }

    /**
     * Sets the viewport state dynamically.
     *
     * @param width        The width of the viewport.
     * @param height       The height of the viewport.
     * @param minDepth     The minimum depth of the viewport.
     * @param maxDepth     The maximum depth of the viewport.
     */
    public void setViewport(int width, int height, float minDepth, float maxDepth) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer viewport = VkViewport.calloc(1,stack).x(0).y(0).width(width).height(height).minDepth(minDepth).maxDepth(maxDepth);
            vkCmdSetViewport(handle, 0, viewport);
        }
    }

    /**
     * Sets the viewport state dynamically.
     *
     * @param firstViewport The index of the first viewport.
     * @param pViewports    A pointer to an array of viewport structures.
     */
    public void setViewports(@NativeType("uint32_t") int firstViewport, @NativeType("VkViewport const *") VkViewport.Buffer pViewports) {
        vkCmdSetViewport(handle, firstViewport, pViewports);
    }

    /**
     * Sets the scissor state dynamically.
     *
     * @param offset_x     The x-coordinate of the scissor rectangle.
     * @param offset_y     The y-coordinate of the scissor rectangle.
     * @param width        The width of the scissor rectangle.
     * @param height       The height of the scissor rectangle.
     */
    public void setScissor(int offset_x, int offset_y, int width, int height) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D extent = VkExtent2D.calloc(stack).width(width).height(height);
            VkOffset2D offset = VkOffset2D.calloc(stack).x(offset_x).y(offset_y);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1,stack).extent(extent).offset(offset);
            vkCmdSetScissor(handle, 0, scissor);
        }
    }


    /**
     * Sets the scissor state dynamically.
     *
     * @param firstScissor The index of the first scissor.
     * @param pScissors    A pointer to an array of scissor structures.
     */
    public void setScissors(@NativeType("uint32_t") int firstScissor, @NativeType("VkRect2D const *") VkRect2D.Buffer pScissors) {
        vkCmdSetScissor(handle, firstScissor, pScissors);
    }

    /**
     * Sets the line width dynamically.
     *
     * @param lineWidth The width of rasterized line segments.
     */
    public void setLineWidth(float lineWidth) {
        vkCmdSetLineWidth(handle, lineWidth);
    }

    /**
     * Sets the depth bias dynamically.
     *
     * @param depthBiasConstantFactor The constant factor of the depth bias.
     * @param depthBiasClamp          The maximum clamped depth bias factor.
     * @param depthBiasSlopeFactor    The slope factor of the depth bias.
     */
    public void setDepthBias(float depthBiasConstantFactor, float depthBiasClamp, float depthBiasSlopeFactor) {
        vkCmdSetDepthBias(handle, depthBiasConstantFactor, depthBiasClamp, depthBiasConstantFactor);
    }

    /**
     * Sets the blend constants dynamically.
     *
     * @param blendConstants A pointer to an array of blend constants.
     */
    public void setBlendConstants(@NativeType("float const *") FloatBuffer blendConstants) {
        vkCmdSetBlendConstants(handle, blendConstants);
    }

    /**
     * Sets the depth bounds dynamically.
     *
     * @param minDepthBounds The lower bounds of the depth range used in depth bounds test.
     * @param maxDepthBounds The upper bounds of the depth range used in depth bounds test.
     */
    public void setDepthBounds(float minDepthBounds, float maxDepthBounds) {
        vkCmdSetDepthBounds(handle, minDepthBounds, maxDepthBounds);
    }

    /**
     * Sets the stencil compare mask dynamically.
     *
     * @param faceMask    A bitmask specifying which set of stencil compare masks to update.
     * @param compareMask The new value to use as the stencil compare mask.
     */
    public void setStencilCompareMask(@NativeType("VkStencilFaceFlags") int faceMask, @NativeType("uint32_t") int compareMask) {
        vkCmdSetStencilCompareMask(handle, faceMask, compareMask);
    }

    /**
     * Sets the stencil write mask dynamically.
     *
     * @param faceMask  A bitmask specifying which set of stencil write masks to update.
     * @param writeMask The new value to use as the stencil write mask.
     */
    public void setStencilWriteMask(@NativeType("VkStencilFaceFlags") int faceMask, @NativeType("uint32_t") int writeMask) {
        VK10.vkCmdSetStencilWriteMask(handle, faceMask, writeMask);
    }

    /**
     * Sets the stencil reference value dynamically.
     *
     * @param faceMask  A bitmask specifying which set of stencil reference values to update.
     * @param reference The new value to use as the stencil reference value.
     */
    public void setStencilReference(@NativeType("VkStencilFaceFlags") int faceMask, @NativeType("uint32_t") int reference) {
        vkCmdSetStencilReference(handle, faceMask, reference);
    }

    /**
     * Binds descriptor sets to a command buffer.
     *
     * @param pipelineBindPoint The bind point for the pipeline (e.g., VK_PIPELINE_BIND_POINT_GRAPHICS).
     * @param layout            The pipeline layout used to program the bindings.
     * @param firstSet          The first set number.
     * @param pDescriptorSets   An array of descriptor sets to bind.
     * @param pDynamicOffsets   An array of dynamic offsets.
     */
    public void bindDescriptorSets(@NativeType("VkPipelineBindPoint") int pipelineBindPoint, @Nonnull PipelineLayout layout, @NativeType("uint32_t") int firstSet, @NativeType("VkDescriptorSet const *") LongBuffer pDescriptorSets, @Nullable @NativeType("uint32_t const *") IntBuffer pDynamicOffsets) {
        vkCmdBindDescriptorSets(handle, pipelineBindPoint, layout.getLayout().handle(), firstSet, pDescriptorSets, pDynamicOffsets);
    }

    /**
     * Binds an index buffer to the command buffer.
     *
     * @param buffer    The buffer to bind as the index buffer.
     * @param offset    The byte offset into the buffer.
     * @param indexType The type of indices in the buffer.
     */
    public void bindIndexBuffer(DeviceHandle buffer, @NativeType("VkDeviceSize") long offset, @NativeType("VkIndexType") int indexType) {
        vkCmdBindIndexBuffer(handle, buffer.handle(), offset, indexType);
    }

    /**
     * Binds vertex buffers to the command buffer.
     *
     * @param firstBinding The index of the first vertex input binding.
     * @param pBuffers     An array of buffers to bind.
     * @param pOffsets     An array of buffer offsets.
     */
    public void bindVertexBuffers(@NativeType("uint32_t") int firstBinding, @NativeType("VkBuffer const *") LongBuffer pBuffers, @NativeType("VkDeviceSize const *") LongBuffer pOffsets) {
        vkCmdBindVertexBuffers(handle, firstBinding, pBuffers, pOffsets);
    }

    /**
     * Binds a vertex buffer to the command buffer.
     *
     * @param firstBinding The index of the first vertex input binding.
     * @param buffer       The buffer to bind.
     * @param pOffset      The byte offset into the buffer.
     */
    public void bindVertexBuffer(@NativeType("uint32_t") int firstBinding, DeviceHandle buffer, @NativeType("VkDeviceSize const") long pOffset) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffers = stack.callocLong(1);
            LongBuffer pOffsets = stack.callocLong(1);
            pBuffers.put(buffer.handle());
            pOffsets.put(pOffset);
            bindVertexBuffers(firstBinding, pBuffers, pOffsets);
        }
    }

    /**
     * Draws non-indexed geometry.
     *
     * @param vertexCount   The number of vertices to draw.
     * @param instanceCount The number of instances to draw.
     * @param firstVertex   The index of the first vertex.
     * @param firstInstance The instance ID of the first instance.
     */
    public void draw(@NativeType("uint32_t") int vertexCount, @NativeType("uint32_t") int instanceCount, @NativeType("uint32_t") int firstVertex, @NativeType("uint32_t") int firstInstance) {
        vkCmdDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance);
    }

    /**
     * Draws indexed geometry.
     *
     * @param indexCount    The number of indices to draw.
     * @param instanceCount The number of instances to draw.
     * @param firstIndex    The base index within the index buffer.
     * @param vertexOffset  The value added to the vertex index before indexing into the vertex buffer.
     * @param firstInstance The instance ID of the first instance.
     */
    public void drawIndexed(@NativeType("uint32_t") int indexCount, @NativeType("uint32_t") int instanceCount, @NativeType("uint32_t") int firstIndex, @NativeType("int32_t") int vertexOffset, @NativeType("uint32_t") int firstInstance) {
        vkCmdDrawIndexed(handle, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
    }

    /**
     * Draws non-indexed geometry with indirect parameters.
     *
     * @param buffer    The buffer containing draw parameters.
     * @param offset    The byte offset into the buffer.
     * @param drawCount The number of draws.
     * @param stride    The byte stride between successive sets of draw parameters.
     */
    public void drawIndirect(DeviceHandle buffer, @NativeType("VkDeviceSize") long offset, @NativeType("uint32_t") int drawCount, @NativeType("uint32_t") int stride) {
        vkCmdDrawIndirect(handle, buffer.handle(), offset, drawCount, stride);
    }

    /**
     * Draws indexed geometry with indirect parameters.
     *
     * @param buffer    The buffer containing draw parameters.
     * @param offset    The byte offset into the buffer.
     * @param drawCount The number of draws.
     * @param stride    The byte stride between successive sets of draw parameters.
     */
    public void drawIndexedIndirect(DeviceHandle buffer, @NativeType("VkDeviceSize") long offset, @NativeType("uint32_t") int drawCount, @NativeType("uint32_t") int stride) {
        vkCmdDrawIndexedIndirect(handle, buffer.handle(), offset, drawCount, stride);
    }

    /**
     * Dispatches compute work items.
     *
     * @param groupCountX The number of local workgroups to dispatch in the X dimension.
     * @param groupCountY The number of local workgroups to dispatch in the Y dimension.
     * @param groupCountZ The number of local workgroups to dispatch in the Z dimension.
     */
    public void dispatch(@NativeType("uint32_t") int groupCountX, @NativeType("uint32_t") int groupCountY, @NativeType("uint32_t") int groupCountZ) {
        vkCmdDispatch(handle, groupCountX, groupCountY, groupCountZ);
    }

    /**
     * Dispatches compute work items with indirect parameters.
     *
     * @param buffer The buffer containing dispatch parameters.
     * @param offset The byte offset into the buffer.
     */
    public void dispatchIndirect(DeviceHandle buffer, @NativeType("VkDeviceSize") long offset) {
        vkCmdDispatchIndirect(handle, buffer.handle(), offset);
    }

    /**
     * Copies data between buffer regions.
     *
     * @param srcBuffer The source buffer.
     * @param dstBuffer The destination buffer.
     * @param pRegions  An array of regions to copy.
     */
    public void copyBuffer(DeviceHandle srcBuffer, DeviceHandle dstBuffer, @NativeType("VkBufferCopy const *") VkBufferCopy.Buffer pRegions) {
        vkCmdCopyBuffer(handle, srcBuffer.handle(), dstBuffer.handle(), pRegions);
    }

    /**
     * Copies the content of one image to another.
     *
     * @param srcImage       The source image handle.
     * @param srcImageLayout The layout of the source image.
     * @param dstImage       The destination image handle.
     * @param dstImageLayout The layout of the destination image.
     * @param pRegions       Buffer containing regions to copy within the images.
     */
    public void copyImage(DeviceHandle srcImage, @NativeType("VkImageLayout") int srcImageLayout, DeviceHandle dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkImageCopy const *") VkImageCopy.Buffer pRegions) {
        vkCmdCopyImage(handle, srcImage.handle(), srcImageLayout, dstImage.handle(), dstImageLayout, pRegions);
    }

    /**
     * Blits the content of one image to another.
     *
     * @param srcImage       The source image handle.
     * @param srcImageLayout The layout of the source image.
     * @param dstImage       The destination image handle.
     * @param dstImageLayout The layout of the destination image.
     * @param pRegions       Buffer containing regions to blit within the images.
     * @param filter         The filtering algorithm for the blit.
     */
    public void blitImage(DeviceHandle srcImage, @NativeType("VkImageLayout") int srcImageLayout, DeviceHandle dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkImageBlit const *") VkImageBlit.Buffer pRegions, @NativeType("VkFilter") int filter) {
        vkCmdBlitImage(handle, srcImage.handle(), srcImageLayout, dstImage.handle(), dstImageLayout, pRegions, filter);
    }

    /**
     * Copies the content of a buffer to an image.
     *
     * @param srcBuffer      The source buffer handle.
     * @param dstImage       The destination image handle.
     * @param dstImageLayout The layout of the destination image.
     * @param pRegions       Buffer containing regions to copy within the buffer and image.
     */
    public void copyBufferToImage(DeviceHandle srcBuffer, DeviceHandle dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkBufferImageCopy const *") VkBufferImageCopy.Buffer pRegions) {
        vkCmdCopyBufferToImage(handle, srcBuffer.handle(), dstImage.handle(), dstImageLayout, pRegions);
    }

    /**
     * Copies the content of an image to a buffer.
     *
     * @param srcImage       The source image handle.
     * @param srcImageLayout The layout of the source image.
     * @param dstBuffer      The destination buffer handle.
     * @param pRegions       Buffer containing regions to copy within the image and buffer.
     */
    public void copyImageToBuffer(DeviceHandle srcImage, @NativeType("VkImageLayout") int srcImageLayout, DeviceHandle dstBuffer, @NativeType("VkBufferImageCopy const *") VkBufferImageCopy.Buffer pRegions) {
        vkCmdCopyImageToBuffer(handle, srcImage.handle(), srcImageLayout, dstBuffer.handle(), pRegions);
    }

    /**
     * Updates the content of a buffer with data from a ByteBuffer.
     *
     * @param dstBuffer The destination buffer handle.
     * @param dstOffset The offset within the destination buffer to start the update.
     * @param pData     The source data in the form of a ByteBuffer.
     */
    public void updateBuffer(DeviceHandle dstBuffer, @NativeType("VkDeviceSize") long dstOffset, @NativeType("void const *") ByteBuffer pData) {
        vkCmdUpdateBuffer(handle, dstBuffer.handle(), dstOffset, pData);
    }

    /**
     * Fills a region of a buffer with a specified 32-bit data value.
     *
     * @param dstBuffer The destination buffer handle.
     * @param dstOffset The offset within the destination buffer to start the fill.
     * @param size      The size of the region to fill.
     * @param data      The 32-bit data value to fill the region with.
     */
    public void fillBuffer(DeviceHandle dstBuffer, @NativeType("VkDeviceSize") long dstOffset, @NativeType("VkDeviceSize") long size, @NativeType("uint32_t") int data) {
        vkCmdFillBuffer(handle, dstBuffer.handle(), dstOffset, size, data);
    }

    /**
     * Clears a color image with the specified clear color values.
     *
     * @param image       The image handle.
     * @param imageLayout The layout of the image.
     * @param pColor      Optional clear color values.
     * @param pRanges     Buffer containing subresource ranges to clear within the image.
     */
    public void clearColorImage(DeviceHandle image, @NativeType("VkImageLayout") int imageLayout, @Nullable @NativeType("VkClearColorValue const *") VkClearColorValue pColor, @NativeType("VkImageSubresourceRange const *") VkImageSubresourceRange.Buffer pRanges) {
        vkCmdClearColorImage(handle, image.handle(), imageLayout, pColor, pRanges);
    }

    /**
     * Clears a depth-stencil image with the specified clear values.
     *
     * @param image         The image handle.
     * @param imageLayout   The layout of the image.
     * @param pDepthStencil Clear values for the depth and stencil aspects.
     * @param pRanges       Buffer containing subresource ranges to clear within the image.
     */
    public void clearDepthStencilImage(DeviceHandle image, @NativeType("VkImageLayout") int imageLayout, @NativeType("VkClearDepthStencilValue const *") VkClearDepthStencilValue pDepthStencil, @NativeType("VkImageSubresourceRange const *") VkImageSubresourceRange.Buffer pRanges) {
        vkCmdClearDepthStencilImage(handle, image.handle(), imageLayout, pDepthStencil, pRanges);
    }

    /**
     * Clears the specified attachments in the current render pass.
     *
     * @param pAttachments Buffer containing attachments to clear.
     * @param pRects       Buffer containing rectangles within the framebuffer to clear.
     */
    public void clearAttachments(@NativeType("VkClearAttachment const *") VkClearAttachment.Buffer pAttachments, @NativeType("VkClearRect const *") VkClearRect.Buffer pRects) {
        vkCmdClearAttachments(handle, pAttachments, pRects);
    }

    /**
     * Resolves multisampled regions from one image to another.
     *
     * @param srcImage       The source image handle.
     * @param srcImageLayout The layout of the source image.
     * @param dstImage       The destination image handle.
     * @param dstImageLayout The layout of the destination image.
     * @param pRegions       Buffer containing regions to resolve within the images.
     */
    public void resolveImage(DeviceHandle srcImage, @NativeType("VkImageLayout") int srcImageLayout, DeviceHandle dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkImageResolve const *") VkImageResolve.Buffer pRegions) {
        vkCmdResolveImage(handle, srcImage.handle(), srcImageLayout, dstImage.handle(), dstImageLayout, pRegions);
    }

    /**
     * Sets an event in the command buffer.
     *
     * @param event     The event to be set.
     * @param stageMask The pipeline stage at which the event is signaled.
     */
    public void setEvent(DeviceHandle event, @NativeType("VkPipelineStageFlags") int stageMask) {
        vkCmdSetEvent(handle, event.handle(), stageMask);
    }

    /**
     * Resets an event in the command buffer.
     *
     * @param event     The event to be reset.
     * @param stageMask The pipeline stage at which the event is unsignaled.
     */
    public void resetEvent(DeviceHandle event, @NativeType("VkPipelineStageFlags") int stageMask) {
        vkCmdResetEvent(handle, event.handle(), stageMask);
    }

    /**
     * Waits on the host for one or more events to be signaled.
     *
     * @param events                An array of events to wait for.
     * @param srcStageMask          The source pipeline stage flags.
     * @param dstStageMask          The destination pipeline stage flags.
     * @param pMemoryBarriers       Optional memory barriers.
     * @param pBufferMemoryBarriers Optional buffer memory barriers.
     * @param pImageMemoryBarriers  Optional image memory barriers.
     */
    public void waitEvents(DeviceHandle[] events, @NativeType("VkPipelineStageFlags") int srcStageMask, @NativeType("VkPipelineStageFlags") int dstStageMask, @Nullable @NativeType("VkMemoryBarrier const *") VkMemoryBarrier.Buffer pMemoryBarriers, @Nullable @NativeType("VkBufferMemoryBarrier const *") VkBufferMemoryBarrier.Buffer pBufferMemoryBarriers, @Nullable @NativeType("VkImageMemoryBarrier const *") VkImageMemoryBarrier.Buffer pImageMemoryBarriers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pEvents = stack.callocLong(events.length);
            for (var e : events)
                pEvents.put(e.handle());
            vkCmdWaitEvents(handle, pEvents, srcStageMask, dstStageMask, pMemoryBarriers, pBufferMemoryBarriers, pImageMemoryBarriers);
        }
    }

    /**
     * Inserts a memory dependency with specified barriers into the command buffer.
     *
     * @param srcStageMask          The source pipeline stage flags.
     * @param dstStageMask          The destination pipeline stage flags.
     * @param dependencyFlags       Dependency flags.
     * @param pMemoryBarriers       Optional memory barriers.
     * @param pBufferMemoryBarriers Optional buffer memory barriers.
     * @param pImageMemoryBarriers  Optional image memory barriers.
     */
    public void pipelineBarrier(@NativeType("VkPipelineStageFlags") int srcStageMask, @NativeType("VkPipelineStageFlags") int dstStageMask, @NativeType("VkDependencyFlags") int dependencyFlags, @Nullable @NativeType("VkMemoryBarrier const *") VkMemoryBarrier.Buffer pMemoryBarriers, @Nullable @NativeType("VkBufferMemoryBarrier const *") VkBufferMemoryBarrier.Buffer pBufferMemoryBarriers, @Nullable @NativeType("VkImageMemoryBarrier const *") VkImageMemoryBarrier.Buffer pImageMemoryBarriers) {
        vkCmdPipelineBarrier(handle, srcStageMask, dstStageMask, dependencyFlags, pMemoryBarriers, pBufferMemoryBarriers, pImageMemoryBarriers);
    }

    public void insertImageMemoryBarrier(Image image,
                                         int srcAccessMask,
                                         int dstAccessMask,
                                         int oldImageLayout,
                                         int newImageLayout,
                                         int srcStageMask,
                                         int dstStageMask,
                                         ImageSubResourceRange subresourceRange) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            if(image.getImage() == VK_NULL_HANDLE) {
                Log.print(Log.Severity.ERROR, "Vulkan: Image handle is null in insertImageMemoryBarrier this is not allowed");
                throw new IllegalStateException("Image handle is null in insertImageMemoryBarrier this is not allowed");
            }
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack)
                    .aspectMask(subresourceRange.aspectMask())
                    .baseMipLevel(subresourceRange.baseMipLevel())
                    .levelCount(subresourceRange.levelCount())
                    .baseArrayLayer(subresourceRange.baseArrayLayer())
                    .layerCount(subresourceRange.layerCount());
            VkImageMemoryBarrier.Buffer image_memory_barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType$Default()
                    .srcAccessMask(srcAccessMask)
                    .dstAccessMask(dstAccessMask)
                    .oldLayout(oldImageLayout)
                    .newLayout(newImageLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image.getImage())
                    .subresourceRange(range);
            vkCmdPipelineBarrier(handle, srcStageMask, dstStageMask, 0, null, null, image_memory_barrier);
        }
    }

    /**
     * Begins a query in the command buffer.
     *
     * @param queryPool The query pool handle.
     * @param query     The index of the query within the pool.
     * @param flags     Query control flags.
     */
    public void beginQuery(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int query, @NativeType("VkQueryControlFlags") int flags) {
        vkCmdBeginQuery(handle, queryPool, query, flags);
    }

    /**
     * Ends a query in the command buffer.
     *
     * @param queryPool The query pool handle.
     * @param query     The index of the query within the pool.
     */
    public void endQuery(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int query) {
        vkCmdEndQuery(handle, queryPool, query);
    }

    /**
     * Resets queries in a query pool.
     *
     * @param queryPool  The query pool handle.
     * @param firstQuery The index of the first query to reset.
     * @param queryCount The number of queries to reset.
     */
    public void resetQueryPool(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount) {
        vkCmdResetQueryPool(handle, queryPool, firstQuery, queryCount);
    }

    /**
     * Writes a timestamp into the command buffer.
     *
     * @param pipelineStage The pipeline stage at which the timestamp is recorded.
     * @param queryPool     The query pool handle.
     * @param query         The index of the query within the pool.
     */
    public void writeTimestamp(@NativeType("VkPipelineStageFlagBits") int pipelineStage, @NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int query) {
        vkCmdWriteTimestamp(handle, pipelineStage, queryPool, query);
    }

    /**
     * Copies the results of queries in a query pool to a buffer.
     *
     * @param queryPool  The query pool handle.
     * @param firstQuery The index of the first query to copy.
     * @param queryCount The number of queries to copy.
     * @param dstBuffer  The destination buffer.
     * @param dstOffset  The offset into the destination buffer.
     * @param stride     The stride between query results in the buffer.
     * @param flags      Flags specifying the type of queries and how results are written.
     */
    public void copyQueryPoolResults(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount, DeviceHandle dstBuffer, @NativeType("VkDeviceSize") long dstOffset, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        vkCmdCopyQueryPoolResults(handle, queryPool, firstQuery, queryCount, dstBuffer.handle(), dstOffset, stride, flags);
    }

    /**
     * Pushes constants to the command buffer.
     *
     * @param layout  The pipeline layout.
     * @param stage   The shader stage flags.
     * @param offset  The offset within the push constant range.
     * @param pValues The values to push.
     */
    public void pushConstants(@Nonnull PipelineLayout layout, ShaderType stage, int offset, @Nonnull FloatBuffer pValues) {
        vkCmdPushConstants(handle, layout.getLayout().handle(), stage.getValue(), offset, pValues);
    }

    /**
     * Advances to the next subpass in the current render pass.
     *
     * @param contents The contents of the next subpass.
     */
    public void nextSubpass(@NativeType("VkSubpassContents") int contents) {
        vkCmdNextSubpass(handle, contents);
    }

    public void beginRenderPass(@Nonnull RenderPass renderPass, @Nonnull float[] colours, float depth, int stencil, @Nonnull VkExtent2D extent, @Nonnull VkOffset2D offset, @Nonnull FrameBuffer frameBuffer, int contents) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderPassBeginInfo passBeginInfo = VkRenderPassBeginInfo.calloc(stack).sType$Default();
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack)
                    .color(c -> c.float32(stack.floats(colours[0], colours[1], colours[2], colours[3])));
            clearValues.depthStencil().depth(depth / 255.0f).stencil(stencil);
            passBeginInfo.renderPass(renderPass.getRenderPass().handle());
            passBeginInfo.framebuffer(frameBuffer.getFrameBuffer().handle());
            passBeginInfo.pClearValues(clearValues);
            passBeginInfo.clearValueCount(clearValues.remaining());
            VkRect2D area = passBeginInfo.renderArea();
            area.set(offset, extent);
            vkCmdBeginRenderPass(handle, passBeginInfo, contents);
        }
    }

    public void beginRenderPass(RenderPass renderPass, float[] colours, VkExtent2D extent, VkOffset2D offset, FrameBuffer frameBuffer, int contents) {
        beginRenderPass(renderPass, colours, 100, 1058379158, extent, offset, frameBuffer, contents);
    }

    public void beginRenderPass(RenderPass renderPass, VkExtent2D extent, VkOffset2D offset, FrameBuffer frameBuffer, int contents) {
        beginRenderPass(renderPass, colours, extent, offset, frameBuffer, contents);
    }

    /**
     * Ends the current render pass.
     */
    public void endRenderPass() {
        vkCmdEndRenderPass(handle);
    }

    /**
     * Executes secondary command buffers within the current command buffer.
     *
     * @param commandBuffers An array of secondary command buffers to execute.
     */
    public void executeCommands(CommandBuffer[] commandBuffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffers.length);
            for (var c : commandBuffers)
                pCommandBuffers.put(c.handle);
            vkCmdExecuteCommands(handle, pCommandBuffers);
        }
    }

    /**
     * Gets the underlying Vulkan command buffer handle.
     *
     * @return The Vulkan command buffer handle.
     */
    public VkCommandBuffer getCommandBuffer() {
        return handle;
    }

    /**
     * Cleans up resources associated with the command buffer.
     * This method should be called when the command buffer is no longer needed.
     */
    @Override
    protected final void free() {
        device.waitIdle();
        device.freeCommandBuffers(command_pool.getCommandPool(), handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: Done freeing command buffer");
    }
}