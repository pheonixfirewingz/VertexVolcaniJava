package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan pipeline layout.
 * A pipeline layout is used to define the layout of descriptor sets used by a pipeline.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public class PipelineLayout extends CleanerObject {
    /**
     * The Vulkan device associated with this pipeline layout.
     */
    private final Device device;
    /**
     * The handle to the Vulkan pipeline layout.
     */
    private final long handle;
    /**
     * Constructs a new PipelineLayout object.
     *
     * @param device_in      The Vulkan device associated with this pipeline layout.
     * @param layout_count   The number of descriptor sets.
     * @param layouts        A LongBuffer containing the handles of the descriptor sets.
     * @param push_constant  A VkPushConstantRange.Buffer specifying the push constant ranges.
     * @throws IllegalStateException If the creation of the pipeline layout fails.
     */
    public PipelineLayout(Device device_in, int layout_count, @Nullable LongBuffer layouts, @Nullable VkPushConstantRange.Buffer push_constant) {
        super();
        device = device_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo create_info = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .setLayoutCount(layout_count).pSetLayouts(layouts).pPushConstantRanges(push_constant);
            LongBuffer buffer = stack.callocLong(1);
            if (device.createPipelineLayout(create_info, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create pipeline layout");
                throw new IllegalStateException("failed to create pipeline layout");
            }
            handle = buffer.get(0);
        }
    }
    /**
     * Gets the handle of the Vulkan pipeline layout.
     *
     * @return The handle of the Vulkan pipeline layout.
     */
    public long getLayout() {
        return handle;
    }
    /**
     * Cleans up and destroys the Vulkan pipeline layout.
     */
    @Override
    public void cleanup() {
        if(handle == VK_NULL_HANDLE) {
            return;
        }
        device.destroyPipelineLayout(handle);
    }
}