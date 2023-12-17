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
import jakarta.annotation.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.nio.LongBuffer;

/**
 * Represents a Vulkan pipeline layout.
 * A pipeline layout is used to define the layout of descriptor sets used by a pipeline.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public class PipelineLayout extends LibCleanable {
    /**
     * The handle to the Vulkan pipeline layout.
     */
    private final DeviceHandle handle;
    /**
     * Constructs a new PipelineLayout object.
     *
     * @param device_in      The Vulkan device associated with this pipeline layout.
     * @param layouts        A LongBuffer containing the handles of the descriptor sets.
     * @param push_constant  A VkPushConstantRange.Buffer specifying the push constant ranges.
     * @throws IllegalStateException If the creation of the pipeline layout fails.
     */
    public PipelineLayout(Device device_in, @Nullable LongBuffer layouts, @Nullable PushConstant[] push_constant) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo create_info = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .setLayoutCount(layouts == null ? 0 : layouts.remaining()).pSetLayouts(layouts);
                    if(push_constant != null) {
                        VkPushConstantRange.Buffer push_constant_range = VkPushConstantRange.calloc(push_constant.length, stack);
                        final boolean is_debug = device_in.isDebug();
                        for (PushConstant pushConstant : push_constant) {
                            push_constant_range.stageFlags(pushConstant.stage.getValue())
                                    .offset(pushConstant.offset).size(pushConstant.size);
                        }
                        create_info.pPushConstantRanges(push_constant_range);
                    } else {
                        create_info.pPushConstantRanges(null);
                    }
            handle = device_in.createPipelineLayout(create_info);
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create pipeline layout");
                throw new IllegalStateException("failed to create pipeline layout");
            }
        }
    }
    /**
     * Gets the handle of the Vulkan pipeline layout.
     *
     * @return The handle of the Vulkan pipeline layout.
     */
    public DeviceHandle getLayout() {
        return handle;
    }

    /**
     * Cleans up and destroys the Vulkan pipeline layout.
     */
    @Override
    public final void free() {
        handle.device().destroyPipelineLayout(handle);
    }

    public record PushConstant(ShaderType stage, int offset, int size) {
    }
}