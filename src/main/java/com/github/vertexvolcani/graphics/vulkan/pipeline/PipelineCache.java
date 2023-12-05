package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class PipelineCache extends CleanerObject {
    private final Device device;
    private final long handle;
    public PipelineCache(@Nonnull Device device_in) {
        super();
        device = device_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            VkPipelineCacheCreateInfo pCreateInfo = VkPipelineCacheCreateInfo.calloc(stack).sType$Default();
            if(device.createPipelineCache(pCreateInfo,buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: failed to create pipeline cache");
                throw new IllegalStateException("failed to create pipeline cache");
            }
            handle = buffer.get(0);
        }
    }

    public long getPipelineCache() {
        return handle;
    }
    @Override
    public void cleanup() {
        if(handle == VK_NULL_HANDLE) {
            return;
        }
        device.destroyPipelineCache(handle);
    }
}
