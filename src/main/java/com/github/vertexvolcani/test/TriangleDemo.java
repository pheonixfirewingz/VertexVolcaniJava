package com.github.vertexvolcani.test;

import com.github.vertexvolcani.graphics.VVWindow;
import com.github.vertexvolcani.graphics.vulkan.*;
import com.github.vertexvolcani.graphics.vulkan.buffer.*;
import com.github.vertexvolcani.graphics.vulkan.pipeline.*;
import com.github.vertexvolcani.util.Log;
import com.github.vertexvolcani.test.util.Vertices;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK11.*;

public class TriangleDemo {
    private FrameBuffer[] frame_buffers;
    private CommandBuffer[] command_buffers;

    public static void main(String[] args) {
        TriangleDemo demo = new TriangleDemo();
        demo.run();
    }

    private FrameBuffer[] createFrameBuffers(Device device, VVWindow window, RenderPass renderPass) {
        FrameBuffer[] frame_buffers = new FrameBuffer[window.getSwapChain().getImages().length];
        for (int i = 0; i < window.getSwapChain().getImages().length; i++) {
            frame_buffers[i] = new FrameBuffer(device, renderPass, new Image[]{window.getSwapChain().getImages()[i]}, window.getSurface());
        }
        return frame_buffers;
    }

    private RenderPass createRenderPass(Device device, Surface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack).format(surface.getColourFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT).loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkSubpassDescription.Buffer sub_pass = VkSubpassDescription.calloc(1, stack).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(colorReference.remaining()).pColorAttachments(colorReference);
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack).srcSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack).sType$Default().pAttachments(attachments)
                    .pSubpasses(sub_pass).pDependencies(dependency);
            return new RenderPass(device, renderPassInfo);
        }
    }

    private Vertices createVertices(VmaAllocator allocator) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertexBuffer = stack.calloc(6 * Float.BYTES);
            FloatBuffer fb = vertexBuffer.asFloatBuffer();
            fb.put(-0.5f).put(-0.5f);
            fb.put(0.5f).put(-0.5f);
            fb.put(0.0f).put(0.5f);

            VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.calloc(1);
            bindingDescriptor.binding(0).stride(2 * 4).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1);
            attributeDescriptions.binding(0).location(0).format(VK_FORMAT_R32G32_SFLOAT).offset(0);

            Buffer buffer = new VertexBuffer(allocator, 65536, false, VmaMemoryUsage.CPU_TO_GPU).write(vertexBuffer);
            return new Vertices(buffer, null, bindingDescriptor, attributeDescriptions);
        }
    }

    private Pipeline createPipeline(Device device, RenderPass renderPass, Vertices vertices) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PipelineLayout layout = new PipelineLayout(device, null, null);
            Shader[] shaders = new Shader[2];
            shaders[0] = new Shader(device, "shader/triangle.vert", ShaderType.VERTEX);
            shaders[1] = new Shader(device, "shader/triangle.frag", ShaderType.FRAGMENT);
            try (Pipeline.PipelineBuilder builder = new Pipeline.PipelineBuilder(shaders, layout, renderPass, true)) {
                VkPipelineColorBlendAttachmentState.Buffer colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1, stack).colorWriteMask(0xF); // <- RGBA
                builder.setColourBlendAttachments(colorWriteMask);
                VkStencilOpState op = VkStencilOpState.calloc(stack);
                op.failOp(VK_STENCIL_OP_KEEP);
                op.passOp(VK_STENCIL_OP_KEEP);
                op.compareOp(VK_COMPARE_OP_ALWAYS);
                builder.setDepthFront(op);
                builder.setDepthBack(op);
                builder.setVertexInputAttribute(vertices.attributeDescriptions());
                builder.setVertexInputBinding(vertices.bindingDescriptor());
                IntBuffer pDynamicStates = stack.callocInt(2);
                pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip();
                builder.setDynamicStates(pDynamicStates);
                return new Pipeline(device, builder, null, false);
            }
        }
    }

    private void createCommandBuffers(Device device, VVWindow surface, CommandPool commandPool, RenderPass renderPass, Pipeline pipeline, Buffer buffer) {
        var img_view = surface.getSwapChain().getImages();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            command_buffers = CommandBuffer.createCommandBuffers(device, commandPool, img_view.length, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            VkExtent2D extent = VkExtent2D.calloc(stack).set(surface.getSurface().getSurfaceSize());
            VkOffset2D offset = VkOffset2D.calloc(stack);

            for (int i = 0; i < command_buffers.length; ++i) {
                if (command_buffers[i].begin(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to begin render command buffer");
                    throw new IllegalStateException("Failed to begin render command buffer");
                }

                command_buffers[i].beginRenderPass(renderPass, extent, offset, frame_buffers[i], VK_SUBPASS_CONTENTS_INLINE);

                VkExtent2D size = surface.getSurface().getSurfaceSize();
                command_buffers[i].setViewport(size.width(), size.height(), 0.0f, 1.0f);
                command_buffers[i].setScissor(0, 0, size.width(), size.height());
                command_buffers[i].setScissor(0, 0, size.width(), size.height());

                command_buffers[i].bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());

                LongBuffer offsets = stack.callocLong(1);
                offsets.put(0, 0L);
                LongBuffer pBuffers = stack.callocLong(1);
                pBuffers.put(0, buffer.getBuffer().handle());
                command_buffers[i].bindVertexBuffers(0, pBuffers, offsets);
                command_buffers[i].draw(3, 1, 0, 0);
                command_buffers[i].endRenderPass();

                if (command_buffers[i].end() != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to begin render command buffer");
                    throw new IllegalStateException("Failed to begin render command buffer");
                }
            }
        }
    }

    public void run() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VVWindow.PrimeGLFW();
            SwapChain.SwapChainBuilder builder = new SwapChain.SwapChainBuilder();
            builder.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT).imageArrayLayers(1)
                    .presentMode(VK_PRESENT_MODE_FIFO_KHR).clipped().compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            // Create the Vulkan instance
            final Instance instance = new Instance(true, "TriangleDemo");
            final Device device = new Device(instance, new Device.DeviceFeaturesToEnabled(false));
            final VVWindow window = new VVWindow(800, 600, "GLFW Vulkan Demo - triangle", (event) -> {
            }, instance, device, builder);

            // Create static Vulkan resources
            final CommandPool commandPool = new CommandPool(device, device.getGraphicsIndex(), true);
            final Queue queue = new Queue(device, device.getGraphicsIndex(), 0);
            final RenderPass renderPass = createRenderPass(device, window.getSurface());
            final Vertices vertices = createVertices(window.getAllocator());
            final Pipeline pipeline = createPipeline(device, renderPass, vertices);

            final class SwapChainHelper {
                void recreate() {
                    if (frame_buffers != null) {
                        for (var frame_buffer : frame_buffers) {
                            try {
                                frame_buffer.close();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        frame_buffers = null;
                    }
                    frame_buffers = createFrameBuffers(device, window, renderPass);
                    if (command_buffers != null) {
                        for (var c : command_buffers) {
                            c.close();
                        }
                        command_buffers = null;
                        commandPool.reset(0);
                    }
                    createCommandBuffers(device, window, commandPool, renderPass, pipeline, vertices.buffer());
                }
            }
            final SwapChainHelper swap_chain_helper = new SwapChainHelper();
            window.refreshSwapChain();
            swap_chain_helper.recreate();

            final IntBuffer pImageIndex = stack.callocInt(1);
            final PointerBuffer pCommandBuffers = stack.callocPointer(1);
            final Semaphore image_acquired = new Semaphore(device);
            final Semaphore render_complete = new Semaphore(device);
            final IntBuffer pWaitDstStageMask = stack.callocInt(1);
            pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

            while (!window.ShouldClose()) {
                window.poll();
                window.getSwapChain().acquireNextImage(null, image_acquired, pImageIndex);
                pCommandBuffers.put(0, command_buffers[pImageIndex.get(0)].getCommandBuffer());
                if (queue.submit(pCommandBuffers, pWaitDstStageMask, new Semaphore[]{image_acquired}, new Semaphore[]{render_complete}, null) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to submit render queue");
                    throw new IllegalStateException("Failed to submit render queue");
                }
                if (window.swapBuffers(queue, new Semaphore[]{render_complete}, pImageIndex) != VK_SUCCESS) {
                    swap_chain_helper.recreate();
                }
            }
            image_acquired.close();
            render_complete.close();
            for (var f : frame_buffers) {
                f.close();
            }
            for (var c : command_buffers) {
                c.close();
            }
            commandPool.close();
            renderPass.close();
            pipeline.close();
            vertices.buffer().close();
            vertices.attributeDescriptions().close();
            vertices.bindingDescriptor().close();
            window.close();
            device.close();
            instance.close();
        }
    }
}