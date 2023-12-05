package com.github.vertexvolcani.graphics.vulkan.buffer;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.RenderPass;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Event;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Pipeline;
import com.github.vertexvolcani.graphics.vulkan.pipeline.PipelineLayout;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
/**
 * Represents a Vulkan command buffer.
 * This class provides a convenient Java interface for Vulkan command buffer operations.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-04
 */
public class CommandBuffer extends CleanerObject {
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
     * @param device_in        The Vulkan device associated with the command buffer.
     * @param command_pool_in  The command pool from which the command buffer is allocated.
     * @param level            The command buffer level (primary or secondary).
     */
    public CommandBuffer(Device device_in, CommandPool command_pool_in, int level) {
        super();
        device = device_in;
        command_pool = command_pool_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer buffer = stack.mallocPointer(1);
            VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack).sType$Default()
                    .commandPool(command_pool.getCommandPool()).level(level).commandBufferCount(1);
            if(device.allocateCommandBuffers(cmdBufAllocateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: could not create command buffer");
                throw new IllegalStateException("could not create command buffer");
            }
            handle = new VkCommandBuffer(buffer.get(0),device.getDevice());
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
    public void bindPipeline(@NativeType("VkPipelineBindPoint") int pipelineBindPoint, Pipeline pipeline) {
        vkCmdBindPipeline(handle, pipelineBindPoint, pipeline.getPipeline());
    }

    /**
     * Sets the viewport state dynamically.
     *
     * @param firstViewport The index of the first viewport.
     * @param pViewports    A pointer to an array of viewport structures.
     */
    public void setViewport(@NativeType("uint32_t") int firstViewport, @NativeType("VkViewport const *") VkViewport.Buffer pViewports) {
        vkCmdSetViewport(handle, firstViewport, pViewports);
    }

    /**
     * Sets the scissor state dynamically.
     *
     * @param firstScissor The index of the first scissor.
     * @param pScissors    A pointer to an array of scissor structures.
     */
    public void setScissor(@NativeType("uint32_t") int firstScissor, @NativeType("VkRect2D const *") VkRect2D.Buffer pScissors) {
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
     * @param faceMask     A bitmask specifying which set of stencil compare masks to update.
     * @param compareMask  The new value to use as the stencil compare mask.
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
    public void bindDescriptorSets(@NativeType("VkPipelineBindPoint") int pipelineBindPoint, @NativeType("VkPipelineLayout") long layout, @NativeType("uint32_t") int firstSet, @NativeType("VkDescriptorSet const *") LongBuffer pDescriptorSets, @Nullable @NativeType("uint32_t const *") IntBuffer pDynamicOffsets) {
        vkCmdBindDescriptorSets(handle, pipelineBindPoint, layout, firstSet, pDescriptorSets, pDynamicOffsets);
    }

    /**
     * Binds an index buffer to the command buffer.
     *
     * @param buffer    The buffer to bind as the index buffer.
     * @param offset    The byte offset into the buffer.
     * @param indexType The type of indices in the buffer.
     */
    public void bindIndexBuffer(Buffer buffer, @NativeType("VkDeviceSize") long offset, @NativeType("VkIndexType") int indexType) {
        vkCmdBindIndexBuffer(handle, buffer.getBuffer(), offset, indexType);
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
    public void bindVertexBuffer(@NativeType("uint32_t") int firstBinding, Buffer buffer, @NativeType("VkDeviceSize const") long pOffset) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffers = stack.callocLong(1);
            LongBuffer pOffsets = stack.callocLong(1);
            pBuffers.put(buffer.getBuffer());
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
    public void drawIndirect(Buffer buffer, @NativeType("VkDeviceSize") long offset, @NativeType("uint32_t") int drawCount, @NativeType("uint32_t") int stride) {
        vkCmdDrawIndirect(handle, buffer.getBuffer(), offset, drawCount, stride);
    }

    /**
     * Draws indexed geometry with indirect parameters.
     *
     * @param buffer    The buffer containing draw parameters.
     * @param offset    The byte offset into the buffer.
     * @param drawCount The number of draws.
     * @param stride    The byte stride between successive sets of draw parameters.
     */
    public void drawIndexedIndirect(Buffer buffer, @NativeType("VkDeviceSize") long offset, @NativeType("uint32_t") int drawCount, @NativeType("uint32_t") int stride) {
        vkCmdDrawIndexedIndirect(handle, buffer.getBuffer(), offset, drawCount, stride);
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
    public void dispatchIndirect(Buffer buffer, @NativeType("VkDeviceSize") long offset) {
        vkCmdDispatchIndirect(handle, buffer.getBuffer(), offset);
    }

    /**
     * Copies data between buffer regions.
     *
     * @param srcBuffer The source buffer.
     * @param dstBuffer The destination buffer.
     * @param pRegions  An array of regions to copy.
     */
    public void copyBuffer(Buffer srcBuffer, Buffer dstBuffer, @NativeType("VkBufferCopy const *") VkBufferCopy.Buffer pRegions) {
        vkCmdCopyBuffer(handle,srcBuffer.getBuffer(),dstBuffer.getBuffer(),pRegions);
    }
    /**
     * Copies the content of one image to another.
     *
     * @param srcImage       The source image handle.
     * @param srcImageLayout The layout of the source image.
     * @param dstImage       The destination image handle.
     * @param dstImageLayout The layout of the destination image.
     * @param pRegions       Buffer containing regions to copy within the images.
     *                       <strong>TODO: Update after creating image class.</strong>
     */
    public void copyImage(@NativeType("VkImage") long srcImage, @NativeType("VkImageLayout") int srcImageLayout, @NativeType("VkImage") long dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkImageCopy const *") VkImageCopy.Buffer pRegions) {
        vkCmdCopyImage(handle, srcImage, srcImageLayout, dstImage, dstImageLayout, pRegions);
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
     *                       <strong>TODO: Update after creating image class.</strong>
     */
    public void blitImage(@NativeType("VkImage") long srcImage, @NativeType("VkImageLayout") int srcImageLayout, @NativeType("VkImage") long dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkImageBlit const *") VkImageBlit.Buffer pRegions, @NativeType("VkFilter") int filter) {
        vkCmdBlitImage(handle, srcImage, srcImageLayout, dstImage, dstImageLayout, pRegions, filter);
    }

    /**
     * Copies the content of a buffer to an image.
     *
     * @param srcBuffer      The source buffer handle.
     * @param dstImage       The destination image handle.
     * @param dstImageLayout The layout of the destination image.
     * @param pRegions       Buffer containing regions to copy within the buffer and image.
     *                       <strong>TODO: Update after creating image class.</strong>
     */
    public void copyBufferToImage(Buffer srcBuffer, @NativeType("VkImage") long dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkBufferImageCopy const *") VkBufferImageCopy.Buffer pRegions) {
        vkCmdCopyBufferToImage(handle, srcBuffer.getBuffer(), dstImage, dstImageLayout, pRegions);
    }

    /**
     * Copies the content of an image to a buffer.
     *
     * @param srcImage       The source image handle.
     * @param srcImageLayout The layout of the source image.
     * @param dstBuffer      The destination buffer handle.
     * @param pRegions       Buffer containing regions to copy within the image and buffer.
     *                       <strong>TODO: Update after creating image class.</strong>
     */
    public void copyImageToBuffer(@NativeType("VkImage") long srcImage, @NativeType("VkImageLayout") int srcImageLayout,  Buffer dstBuffer, @NativeType("VkBufferImageCopy const *") VkBufferImageCopy.Buffer pRegions) {
        vkCmdCopyImageToBuffer(handle, srcImage, srcImageLayout, dstBuffer.getBuffer(), pRegions);
    }

    /**
     * Updates the content of a buffer with data from a ByteBuffer.
     *
     * @param dstBuffer The destination buffer handle.
     * @param dstOffset The offset within the destination buffer to start the update.
     * @param pData     The source data in the form of a ByteBuffer.
     */
    public void updateBuffer(Buffer dstBuffer, @NativeType("VkDeviceSize") long dstOffset, @NativeType("void const *") ByteBuffer pData) {
        vkCmdUpdateBuffer(handle, dstBuffer.getBuffer(), dstOffset, pData);
    }

    /**
     * Fills a region of a buffer with a specified 32-bit data value.
     *
     * @param dstBuffer The destination buffer handle.
     * @param dstOffset The offset within the destination buffer to start the fill.
     * @param size      The size of the region to fill.
     * @param data      The 32-bit data value to fill the region with.
     */
    public void fillBuffer(Buffer dstBuffer, @NativeType("VkDeviceSize") long dstOffset, @NativeType("VkDeviceSize") long size, @NativeType("uint32_t") int data) {
        vkCmdFillBuffer(handle, dstBuffer.getBuffer(), dstOffset, size, data);
    }

    /**
     * Clears a color image with the specified clear color values.
     *
     * @param image        The image handle.
     * @param imageLayout  The layout of the image.
     * @param pColor       Optional clear color values.
     *                     <strong>TODO: Update after creating image class.</strong>
     * @param pRanges      Buffer containing subresource ranges to clear within the image.
     *                     <strong>TODO: Update after creating image class.</strong>
     */
    public void clearColorImage(@NativeType("VkImage") long image, @NativeType("VkImageLayout") int imageLayout, @Nullable @NativeType("VkClearColorValue const *") VkClearColorValue pColor, @NativeType("VkImageSubresourceRange const *") VkImageSubresourceRange.Buffer pRanges) {
        vkCmdClearColorImage(handle, image, imageLayout, pColor, pRanges);
    }

    /**
     * Clears a depth-stencil image with the specified clear values.
     *
     * @param image        The image handle.
     * @param imageLayout  The layout of the image.
     * @param pDepthStencil Clear values for the depth and stencil aspects.
     *                     <strong>TODO: Update after creating image class.</strong>
     * @param pRanges      Buffer containing subresource ranges to clear within the image.
     *                     <strong>TODO: Update after creating image class.</strong>
     */
    public void clearDepthStencilImage(@NativeType("VkImage") long image, @NativeType("VkImageLayout") int imageLayout, @NativeType("VkClearDepthStencilValue const *") VkClearDepthStencilValue pDepthStencil, @NativeType("VkImageSubresourceRange const *") VkImageSubresourceRange.Buffer pRanges) {
        vkCmdClearDepthStencilImage(handle, image, imageLayout, pDepthStencil, pRanges);
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
     *                       <strong>TODO: Update after creating image class.</strong>
     */
    public void resolveImage(@NativeType("VkImage") long srcImage, @NativeType("VkImageLayout") int srcImageLayout, @NativeType("VkImage") long dstImage, @NativeType("VkImageLayout") int dstImageLayout, @NativeType("VkImageResolve const *") VkImageResolve.Buffer pRegions) {
        vkCmdResolveImage(handle, srcImage, srcImageLayout, dstImage, dstImageLayout, pRegions);
    }
    /**
     * Sets an event in the command buffer.
     *
     * @param event     The event to be set.
     * @param stageMask The pipeline stage at which the event is signaled.
     */
    public void setEvent(Event event, @NativeType("VkPipelineStageFlags") int stageMask) {
        vkCmdSetEvent(handle, event.getEvent(), stageMask);
    }

    /**
     * Resets an event in the command buffer.
     *
     * @param event     The event to be reset.
     * @param stageMask The pipeline stage at which the event is unsignaled.
     */
    public void resetEvent(Event event, @NativeType("VkPipelineStageFlags") int stageMask) {
        vkCmdResetEvent(handle, event.getEvent(), stageMask);
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
    public void waitEvents(Event[] events, @NativeType("VkPipelineStageFlags") int srcStageMask, @NativeType("VkPipelineStageFlags") int dstStageMask, @Nullable @NativeType("VkMemoryBarrier const *") VkMemoryBarrier.Buffer pMemoryBarriers, @Nullable @NativeType("VkBufferMemoryBarrier const *") VkBufferMemoryBarrier.Buffer pBufferMemoryBarriers, @Nullable @NativeType("VkImageMemoryBarrier const *") VkImageMemoryBarrier.Buffer pImageMemoryBarriers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pEvents = stack.callocLong(events.length);
            for (var e : events)
                pEvents.put(e.getEvent());
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
     * @param queryPool   The query pool handle.
     * @param firstQuery  The index of the first query to copy.
     * @param queryCount  The number of queries to copy.
     * @param dstBuffer   The destination buffer.
     * @param dstOffset   The offset into the destination buffer.
     * @param stride      The stride between query results in the buffer.
     * @param flags       Flags specifying the type of queries and how results are written.
     */
    public void copyQueryPoolResults(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount, Buffer dstBuffer, @NativeType("VkDeviceSize") long dstOffset, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        vkCmdCopyQueryPoolResults(handle, queryPool, firstQuery, queryCount, dstBuffer.getBuffer(), dstOffset, stride, flags);
    }

    /**
     * Pushes constants to the command buffer.
     *
     * @param layout    The pipeline layout.
     * @param stageFlags The shader stage flags.
     * @param offset     The offset within the push constant range.
     * @param pValues    The values to push.
     */
    public void pushConstants(PipelineLayout layout, @NativeType("VkShaderStageFlags") int stageFlags, @NativeType("uint32_t") int offset, @NativeType("void const *") ByteBuffer pValues) {
        vkCmdPushConstants(handle, layout.getLayout(), stageFlags, offset, pValues);
    }
    /**
     * Advances to the next subpass in the current render pass.
     *
     * @param contents The contents of the next subpass.
     */
    public void nextSubpass(@NativeType("VkSubpassContents") int contents) {
        vkCmdNextSubpass(handle, contents);
    }

    public void beginRenderPass(RenderPass renderPass, VkClearValue.Buffer clearValues, VkExtent2D extent, VkOffset2D offset, long frameBuffer, int vkSubpassContentsInline) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderPassBeginInfo passBeginInfo = VkRenderPassBeginInfo.calloc(stack).sType$Default();
            passBeginInfo.renderPass(renderPass.getRenderPass());
            passBeginInfo.framebuffer(frameBuffer);
            passBeginInfo.pClearValues(clearValues);
            passBeginInfo.clearValueCount(clearValues.remaining());
            VkRect2D area = passBeginInfo.renderArea();
            area.set(offset,extent);
            vkCmdBeginRenderPass(handle,passBeginInfo,vkSubpassContentsInline);
        }
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
     * Cleans up resources associated with the command buffer.
     * This method should be called when the command buffer is no longer needed.
     */
    @Override
    public void cleanup() {
        device.deviceWaitIdle();
        device.freeCommandBuffers(command_pool.getCommandPool(), handle);
    }

    /**
     * Gets the underlying Vulkan command buffer handle.
     *
     * @return The Vulkan command buffer handle.
     */
    public VkCommandBuffer getCommandBuffer() {
        return handle;
    }
}