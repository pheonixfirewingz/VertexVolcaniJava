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
import com.github.vertexvolcani.util.ShaderCUtil;
import jakarta.annotation.Nonnull;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

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
        try (VkShaderModuleCreateInfo.Buffer moduleCreateInfo = VkShaderModuleCreateInfo.calloc(1)) {
            moduleCreateInfo.sType$Default().pCode(spriv);
            handle = device_in.createShaderModule(moduleCreateInfo.get(0));
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create shader module");
                throw new IllegalStateException("Failed to create shader module");
            }
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
        try (VkShaderModuleCreateInfo.Buffer moduleCreateInfo = VkShaderModuleCreateInfo.calloc(1)) {
            moduleCreateInfo.sType$Default().pCode(ShaderCUtil.glslToSpirv(source_file_path,stage.getValue(),device_in.isDebug()));
            handle = device_in.createShaderModule(moduleCreateInfo.get(0));
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create shader module");
                throw new IllegalStateException("Failed to create shader module");
            }
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
    public DeviceHandle getShader() {
        return handle;
    }

    /**
     * Closes and destroys the shader module.
     * <p>
     * This method releases Vulkan resources associated with the shader module.
     * </p>
     */
    @Override
    public final void free() {
        handle.device().destroyShaderModule(handle);
    }
}