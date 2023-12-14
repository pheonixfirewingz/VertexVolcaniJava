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

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

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
     * @param layout_count   The number of descriptor sets.
     * @param layouts        A LongBuffer containing the handles of the descriptor sets.
     * @param push_constant  A VkPushConstantRange.Buffer specifying the push constant ranges.
     * @throws IllegalStateException If the creation of the pipeline layout fails.
     */
    public PipelineLayout(Device device_in, int layout_count, @Nullable LongBuffer layouts, @Nullable PushConstant[] push_constant) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo create_info = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .setLayoutCount(layout_count).pSetLayouts(layouts);
                    if(push_constant != null) {
                        VkPushConstantRange.Buffer push_constant_range = VkPushConstantRange.calloc(push_constant.length, stack);
                        final boolean is_debug = device_in.isDebug();
                        for (PushConstant pushConstant : push_constant) {
                            if(is_debug) {
                                //offset must be less than VkPhysicalDeviceLimits::maxPushConstantsSize
                                if (pushConstant.offset >= device_in.getLimits().maxPushConstantsSize()) {
                                    Log.print(Log.Severity.ERROR, "Vulkan:offset must be less than VkPhysicalDeviceLimits::maxPushConstantsSize");
                                    throw new IllegalStateException("offset must be less than VkPhysicalDeviceLimits::maxPushConstantsSize");
                                }
                                //offset must be a multiple of 4
                                if (pushConstant.offset % 4 != 0) {
                                    Log.print(Log.Severity.ERROR, "Vulkan:offset must be a multiple of 4");
                                    throw new IllegalStateException("offset must be a multiple of 4");
                                }
                                //size must be greater than 0
                                if (pushConstant.size < 0) {
                                    Log.print(Log.Severity.ERROR, "Vulkan:size must be greater than 0");
                                    throw new IllegalStateException("size must be greater than 0");
                                }

                                //size must be a multiple of 4
                                if (pushConstant.size % 4 != 0) {
                                    Log.print(Log.Severity.ERROR, "Vulkan:size must be a multiple of 4");
                                    throw new IllegalStateException("size must be a multiple of 4");
                                }

                                //size must be less than or equal to VkPhysicalDeviceLimits::maxPushConstantsSize minus offset
                                if (pushConstant.size > device_in.getLimits().maxPushConstantsSize() - pushConstant.offset) {
                                    Log.print(Log.Severity.ERROR, "Vulkan:size must be less than or equal to VkPhysicalDeviceLimits::maxPushConstantsSize minus offset");
                                    throw new IllegalStateException("size must be less than or equal to VkPhysicalDeviceLimits::maxPushConstantsSize minus offset");
                                }

                                //stageFlags must not be 0
                                if (pushConstant.stage.getValue() == 0) {
                                    Log.print(Log.Severity.ERROR, "Vulkan:stageFlags must not be 0");
                                    throw new IllegalStateException("stageFlags must not be 0");
                                }
                            }
                            push_constant_range
                                    .stageFlags(pushConstant.stage.getValue())
                                    .offset(pushConstant.offset)
                                    .size(pushConstant.size);
                        }
                        create_info.pPushConstantRanges(push_constant_range);
                    } else {
                        create_info.pPushConstantRanges(null);
                    }
            LongBuffer buffer = stack.callocLong(1);
            if (device_in.createPipelineLayout(create_info, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to create pipeline layout");
                throw new IllegalStateException("failed to create pipeline layout");
            }
            handle = new DeviceHandle(device_in,buffer.get(0));
        }
    }
    /**
     * Gets the handle of the Vulkan pipeline layout.
     *
     * @return The handle of the Vulkan pipeline layout.
     */
    public long getLayout() {
        return handle.handle();
    }
    /**
     * Cleans up and destroys the Vulkan pipeline layout.
     */
    @Override
    public final void free() {
        handle.device().destroyPipelineLayout(handle.handle());
    }

    public record PushConstant(ShaderType stage, int offset, int size) {
    }
}