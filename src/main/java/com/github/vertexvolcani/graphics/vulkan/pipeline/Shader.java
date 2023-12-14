package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import com.github.vertexvolcani.util.ShaderCUtil;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan shader module used in the creation of a pipeline.
 *
 * <p>This class manages the creation and destruction of Vulkan shader modules.</p>
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public class Shader extends LibCleanable {
    /**
     * The handle to the Vulkan shader module.
     */
    private final DeviceHandle handle;

    /**
     * The shader stage of the shader module.
     */
    private final ShaderType stage;

    /**
     * Creates a new Shader object for the specified Vulkan device.
     *
     * @param device_in The Vulkan device associated with the shader module.
     * @param spriv The shader bytecode in SPIR-V format.
     * @param stage_in The shader stage.
     */
    public Shader(@Nonnull Device device_in, @Nonnull ByteBuffer spriv, ShaderType stage_in) {
        stage = stage_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType$Default()
                    .pCode(spriv);
            LongBuffer buffer = stack.callocLong(1);
            if (device_in.createShaderModule(moduleCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create shader module");
                throw new IllegalStateException("Failed to create shader module");
            }
            handle = new DeviceHandle(device_in, buffer.get(0));
        }
    }

    /**
     * Creates a new Shader object for the specified Vulkan device.
     *
     * @param device_in The Vulkan device associated with the shader module.
     * @param source_file_path The shader glsl.
     * @param stage_in The shader stage.
     */
    public Shader(@Nonnull Device device_in, @Nonnull String source_file_path, ShaderType stage_in) {
        stage = stage_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType$Default().pCode(ShaderCUtil.glslToSpirv(source_file_path,stage.getValue(),device_in.isDebug()));
            LongBuffer buffer = stack.callocLong(1);
            if (device_in.createShaderModule(moduleCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create shader module");
                throw new IllegalStateException("Failed to create shader module");
            }
            handle = new DeviceHandle(device_in, buffer.get(0));
        } catch (IOException e) {
            Log.print(Log.Severity.ERROR, "Vulkan: Failed to compile shader module");
            throw new IllegalStateException("Failed to compile shader module");
        }
    }

    /**
     * Retrieves the shader stage of the shader module.
     *
     * @return The shader stage.
     */
    public ShaderType getStage() {
        return stage;
    }

    /**
     * Retrieves the Vulkan handle of the shader module.
     *
     * @return The Vulkan handle of the shader module.
     */
    public long getShader() {
        return handle.handle();
    }

    /**
     * Closes and destroys the shader module.
     * <p>
     * This method releases Vulkan resources associated with the shader module.
     * </p>
     */
    @Override
    public final void free() {
        handle.device().destroyShaderModule(handle.handle());
    }
}