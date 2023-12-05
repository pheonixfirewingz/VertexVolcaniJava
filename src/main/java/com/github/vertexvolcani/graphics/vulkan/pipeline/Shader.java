package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;

public class Shader extends CleanerObject {
    /**
     * The Vulkan device associated with this swap chain.
     */
    private final Device device;
    /** The handle to the Vulkan shader. */
    private final long handle;
    private final int stage;

    public Shader(@Nonnull Device device_in,@Nonnull ByteBuffer spriv,int stage_in) {
        device = device_in;
        stage = stage_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc(stack).sType$Default().pCode(spriv);
            LongBuffer buffer = stack.callocLong(1);
            if (device.createShaderModule(moduleCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: Failed to create shader module");
                throw new IllegalStateException("Failed to create shader module");
            }
            handle = buffer.get(0);
        }
    }

    public int getStage() {
        return stage;
    }

    public long getShader() {
        return handle;
    }

    @Override
    public void cleanup() {
        device.destroyShaderModule(handle);
    }
}
