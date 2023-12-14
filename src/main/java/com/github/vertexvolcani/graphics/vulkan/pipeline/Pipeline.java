package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan graphics or compute pipeline.
 * A pipeline encapsulates the configuration of the graphics or compute pipeline, including shader stages,
 * vertex input, rasterization settings, blending settings, and more.
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public class Pipeline extends LibCleanable {
    /**
     * The handle to the Vulkan pipeline.
     */
    private final DeviceHandle handle;

    private final PipelineLayout layout;

    /**
     * Constructs a new Pipeline object.
     *
     * @param device_in The Vulkan device associated with this pipeline.
     * @param builder   The builder used to create the pipeline configuration.
     * @param cache     The pipeline cache used for caching, or {@code null} if not used.
     * @param compute   {@code true} if the pipeline is a compute pipeline, {@code false} for a graphics pipeline.
     * @throws IllegalStateException If the creation of the Vulkan pipeline fails.
     */
    public Pipeline(@Nonnull Device device_in, @Nonnull PipelineBuilder builder, @Nullable PipelineCache cache, boolean compute) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            if (compute) {
                VkComputePipelineCreateInfo.Buffer pCreateInfo = builder.buildComputePipeline(stack);
                if (device_in.createComputePipelines(cache != null ? cache.getPipelineCache() : VK_NULL_HANDLE, pCreateInfo, buffer) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: failed to create compute pipeline");
                    throw new IllegalStateException("failed to create compute pipeline");
                }
            } else {
                VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo = builder.buildGraphicsPipeline(stack);
                if (device_in.createGraphicsPipelines(cache != null ? cache.getPipelineCache() : VK_NULL_HANDLE, pipelineCreateInfo, buffer) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: failed to create graphics pipeline");
                    throw new IllegalStateException("failed to create graphics pipeline");
                }
            }
            handle = new DeviceHandle(device_in,buffer.get(0));
            layout = builder.layout;
        }
    }

    /**
     * Gets the handle of the Vulkan pipeline.
     *
     * @return The handle of the Vulkan pipeline.
     */
    public long getPipeline() {
        return handle.handle();
    }


    /**
     * Gets the Vulkan pipeline layout.
     *
     * @return The Vulkan pipeline layout.
     */
    public PipelineLayout getLayout() {
        return layout;
    }

    /**
     * Cleans up and destroys the Vulkan pipeline.
     */
    @Override
    public final void free() {
        layout.free();
        handle.device().destroyPipeline(handle.handle());
    }

    /**
     * Builder class for configuring a Vulkan graphics or compute pipeline.
     */
    public static class PipelineBuilder extends LibCleanable {
        private final VkPipelineInputAssemblyStateCreateInfo input_assembly_state = VkPipelineInputAssemblyStateCreateInfo.calloc().sType$Default();
        private final VkPipelineVertexInputStateCreateInfo vertex_input_state = VkPipelineVertexInputStateCreateInfo.calloc().sType$Default();
        private final VkPipelineRasterizationStateCreateInfo rasterization_state = VkPipelineRasterizationStateCreateInfo.calloc().sType$Default();
        private final VkPipelineColorBlendStateCreateInfo color_blend_state = VkPipelineColorBlendStateCreateInfo.calloc().sType$Default();
        private final VkPipelineViewportStateCreateInfo viewport_state = VkPipelineViewportStateCreateInfo.calloc().sType$Default();
        private final VkPipelineDynamicStateCreateInfo dynamic_state = VkPipelineDynamicStateCreateInfo.calloc().sType$Default();
        private final VkPipelineDepthStencilStateCreateInfo depth_stencil_state = VkPipelineDepthStencilStateCreateInfo.calloc().sType$Default();
        private final VkPipelineMultisampleStateCreateInfo multi_sample_state = VkPipelineMultisampleStateCreateInfo.calloc().sType$Default();
        private final Shader[] shader_stages;
        private final PipelineLayout layout;
        private final RenderPass render_pass;

        private final ByteBuffer entry_name = MemoryUtil.memUTF8("main");

        /**
         * Constructs a new PipelineBuilder object.
         *
         * @param shader_stages_in The shader stages of the pipeline.
         * @param layout_in        The pipeline layout associated with the pipeline.
         * @param render_pass_in   The render pass associated with the pipeline.
         */
        public PipelineBuilder(@Nonnull Shader[] shader_stages_in, @Nonnull PipelineLayout layout_in, @Nonnull RenderPass render_pass_in) {
            super();
            shader_stages = shader_stages_in;
            layout = layout_in;
            render_pass = render_pass_in;
        }

        /**
         * Sets the primitive topology of the pipeline.
         *
         * @param topology The primitive topology.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setPrimitiveTopology(int topology) {
            input_assembly_state.topology(topology);
            return this;
        }

        public PipelineBuilder setLineWidth(float line_width) {
            rasterization_state.lineWidth(line_width);
            return this;
        }

        /**
         * Sets the vertex input bindings for the pipeline.
         *
         * @param binding The vertex input binding descriptions.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setVertexInputBinding(@Nonnull VkVertexInputBindingDescription.Buffer binding) {
            vertex_input_state.pVertexBindingDescriptions(binding);
            return this;
        }

        /**
         * Sets the vertex input attributes for the pipeline.
         *
         * @param attribute The vertex input attribute descriptions.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setVertexInputAttribute(@Nonnull VkVertexInputAttributeDescription.Buffer attribute) {
            vertex_input_state.pVertexAttributeDescriptions(attribute);
            return this;
        }

        /**
         * Sets the polygon mode for rasterization.
         *
         * @param polygonMode The polygon mode.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setPolygonMode(int polygonMode) {
            rasterization_state.polygonMode(polygonMode);
            return this;
        }

        /**
         * Sets the cull mode for rasterization.
         *
         * @param cullMode The cull mode.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setCullMode(int cullMode) {
            rasterization_state.cullMode(cullMode);
            return this;
        }

        /**
         * Sets the front face for rasterization.
         *
         * @param frontFace The front face.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setFrontFace(int frontFace) {
            rasterization_state.frontFace(frontFace);
            return this;
        }

        /**
         * Sets the blend constants for color blending.
         *
         * @param blendConstants The blend constants.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setBlendConstants(@Nonnull FloatBuffer blendConstants) {
            color_blend_state.blendConstants(blendConstants);
            return this;
        }

        public PipelineBuilder setColourBlendAttachments(@Nonnull VkPipelineColorBlendAttachmentState.Buffer attachments) {
            color_blend_state.pAttachments(attachments);
            return this;
        }

        /**
         * Sets the viewport for the pipeline.
         *
         * @param viewport The viewport.
         * @param count    The number of viewports.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setViewport(@Nullable VkViewport.Buffer viewport, int count) {
            viewport_state.pViewports(viewport);
            viewport_state.viewportCount(count);
            return this;
        }

        /**
         * Sets the scissor for the pipeline.
         *
         * @param scissor The scissor.
         * @param count   The number of scissors.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setScissor(@Nullable VkRect2D.Buffer scissor, int count) {
            viewport_state.pScissors(scissor);
            viewport_state.scissorCount(count);
            return this;
        }

        /**
         * Sets the dynamic states for the pipeline.
         *
         * @param dynamicStates The dynamic states.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setDynamicStates(@Nonnull IntBuffer dynamicStates) {
            dynamic_state.pDynamicStates(dynamicStates);
            return this;
        }

        /**
         * Enables or disables depth testing for the pipeline.
         *
         * @param depthTestEnable {@code true} to enable depth testing, {@code false} to disable.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setDepthTestEnable(boolean depthTestEnable) {
            depth_stencil_state.depthTestEnable(depthTestEnable);
            return this;
        }

        /**
         * Sets the sample count for rasterization.
         *
         * @param sampleCount The sample count.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setSampleCount(int sampleCount) {
            multi_sample_state.rasterizationSamples(sampleCount);
            return this;
        }

        /**
         * Sets the minimum sample shading value for multisampling.
         *
         * @param minSampleShading The minimum sample shading value.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setMinSampleShading(float minSampleShading) {
            multi_sample_state.minSampleShading(minSampleShading);
            return this;
        }

        /**
         * Sets the sample mask for the pipeline.
         *
         * @param sampleMask The sample mask.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setSampleMask(@Nonnull IntBuffer sampleMask) {
            multi_sample_state.pSampleMask(sampleMask);
            return this;
        }

        /**
         * Enables or disables depth writing for the pipeline.
         *
         * @param depthWriteEnable {@code true} to enable depth writing, {@code false} to disable.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setDepthWriteEnable(boolean depthWriteEnable) {
            depth_stencil_state.depthWriteEnable(depthWriteEnable);
            return this;
        }

        /**
         * Sets the depth compare operation for the pipeline.
         *
         * @param depthCompareOp The depth compare operation.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setDepthCompareOp(int depthCompareOp) {
            depth_stencil_state.depthCompareOp(depthCompareOp);
            return this;
        }

        public PipelineBuilder setDepthFront(VkStencilOpState state) {
            depth_stencil_state.front(state);
            return this;
        }

        public PipelineBuilder setDepthBack(VkStencilOpState state) {
            depth_stencil_state.front(state);
            return this;
        }

        /**
         * Enables or disables logic operations for color blending.
         *
         * @param logicOpEnable {@code true} to enable logic operations, {@code false} to disable.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setLogicOpEnable(boolean logicOpEnable) {
            color_blend_state.logicOpEnable(logicOpEnable);
            return this;
        }

        /**
         * Sets the logic operation for color blending.
         *
         * @param logicOp The logic operation.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setLogicOp(int logicOp) {
            color_blend_state.logicOp(logicOp);
            return this;
        }

        /**
         * Enables or disables alpha-to-coverage for multisampling.
         *
         * @param alphaToCoverageEnable {@code true} to enable alpha-to-coverage, {@code false} to disable.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setAlphaToCoverageEnable(boolean alphaToCoverageEnable) {
            multi_sample_state.alphaToCoverageEnable(alphaToCoverageEnable);
            return this;
        }

        /**
         * Enables or disables alpha-to-one for multisampling.
         *
         * @param alphaToOneEnable {@code true} to enable alpha-to-one, {@code false} to disable.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setAlphaToOneEnable(boolean alphaToOneEnable) {
            multi_sample_state.alphaToOneEnable(alphaToOneEnable);
            return this;
        }

        /**
         * Enables or disables stencil testing for the pipeline.
         *
         * @param stencilTestEnable {@code true} to enable stencil testing, {@code false} to disable.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setStencilTestEnable(boolean stencilTestEnable) {
            depth_stencil_state.stencilTestEnable(stencilTestEnable);
            return this;
        }

        /**
         * Sets the stencil operations for the pipeline.
         *
         * @param front The front stencil operation.
         * @param back  The back stencil operation.
         * @return This PipelineBuilder for method chaining.
         */
        public PipelineBuilder setStencilOp(@Nonnull VkStencilOpState front, @Nonnull VkStencilOpState back) {
            depth_stencil_state.front(front);
            depth_stencil_state.back(back);
            return this;
        }

        private VkPipelineShaderStageCreateInfo.Buffer getShaderInfo(ByteBuffer entry_name) {
            VkPipelineShaderStageCreateInfo.Buffer buffer = VkPipelineShaderStageCreateInfo.calloc(shader_stages.length);
            int index = 0;
            for (var shaders : shader_stages) {
                var temp = buffer.get(index);
                temp.sType$Default().pName(entry_name).stage(shaders.getStage().getValue()).module(shaders.getShader());
                buffer.put(index, temp);
                index++;
            }
            return buffer;
        }

        /**
         * Builds a Vulkan graphics pipeline using the specified memory stack.
         *
         * @param stack The MemoryStack to use for allocating temporary data.
         * @return A VkGraphicsPipelineCreateInfo.Buffer containing the configuration for the graphics pipeline.
         */
        protected VkGraphicsPipelineCreateInfo.Buffer buildGraphicsPipeline(@Nonnull MemoryStack stack) {
            VkPipelineShaderStageCreateInfo.Buffer shaders = getShaderInfo(entry_name);
            //Fixme: this leaks memory VkPipelineShaderStageCreateInfo.Buffer
            return VkGraphicsPipelineCreateInfo.calloc(1, stack).sType$Default().layout(layout.getLayout())
                    .renderPass(render_pass.getRenderPass()).pVertexInputState(vertex_input_state)
                    .pInputAssemblyState(input_assembly_state).pRasterizationState(rasterization_state)
                    .pColorBlendState(color_blend_state).pMultisampleState(multi_sample_state).pViewportState(viewport_state)
                    .pDepthStencilState(depth_stencil_state).pStages(shaders).pDynamicState(dynamic_state);
        }

        /**
         * Builds a Vulkan compute pipeline using the specified memory stack.
         *
         * @param stack The MemoryStack to use for allocating temporary data.
         * @return A VkComputePipelineCreateInfo.Buffer containing the configuration for the compute pipeline.
         */
        protected VkComputePipelineCreateInfo.Buffer buildComputePipeline(@Nonnull MemoryStack stack) {
            VkPipelineShaderStageCreateInfo.Buffer shaders = getShaderInfo(entry_name);
            //Fixme: this leaks memory
            return VkComputePipelineCreateInfo.calloc(1, stack).sType$Default().layout(layout.getLayout()).stage(shaders.get(0)).basePipelineIndex(-1);
        }

        /**
         * Cleans up and frees resources associated with this PipelineBuilder.
         */
        @Override
        public final void free() {
            input_assembly_state.free();
            vertex_input_state.free();
            rasterization_state.free();
            color_blend_state.free();
            viewport_state.free();
            dynamic_state.free();
            depth_stencil_state.free();
            multi_sample_state.free();
            MemoryUtil.memFree(entry_name);
            for (var s : shader_stages) {
                s.free();
            }
        }
    }
}