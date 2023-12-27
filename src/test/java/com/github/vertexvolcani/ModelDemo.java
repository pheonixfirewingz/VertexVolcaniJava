package com.github.vertexvolcani;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2022, 2023
 */

import com.github.vertexvolcani.graphics.Window;
import com.github.vertexvolcani.graphics.events.KeyEvent;
import com.github.vertexvolcani.graphics.vulkan.*;
import com.github.vertexvolcani.graphics.vulkan.buffer.*;
import com.github.vertexvolcani.graphics.vulkan.pipeline.*;
import com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors.*;
import com.github.vertexvolcani.util.Camera;
import com.github.vertexvolcani.util.Log;
import com.github.vertexvolcani.util.Vertices;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class ModelDemo {
    private final Camera camera = new Camera();
    private Window window;
    private FrameBuffer[] frame_buffers;
    private CommandBuffer[] command_buffers;
    private Buffer uniform_buffer;

    public static void main(String[] args) throws Exception {
        System.setProperty("LWJGL_DISABLE_RENDEROCD", "false");
        ModelDemo demo = new ModelDemo();
        demo.run();
    }

    private FrameBuffer[] createFrameBuffers(Device device, Surface surface, SwapChain SwapChain, RenderPass renderPass) {
        FrameBuffer[] frame_buffers = new FrameBuffer[SwapChain.getImages().length];
        for (int i = 0; i < SwapChain.getImages().length; i++) {
            frame_buffers[i] = new FrameBuffer(device, renderPass, new Image[]{SwapChain.getImagesViews()[i]}, surface);
        }
        return frame_buffers;
    }

    private RenderPass createRenderPass(Device device, Surface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack).format(surface.getColourFormat()).samples(VK_SAMPLE_COUNT_1_BIT).loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE).stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkSubpassDescription.Buffer sub_pass = VkSubpassDescription.calloc(1, stack).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).colorAttachmentCount(colorReference.remaining()).pColorAttachments(colorReference);
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack).srcSubpass(VK_SUBPASS_EXTERNAL).srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack).sType$Default().pAttachments(attachments).pSubpasses(sub_pass).pDependencies(dependency);
            return new RenderPass(device, renderPassInfo);
        }
    }

    private DescriptorLayout createDescriptorLayout(Device device) {
        LayoutBinding[] bindings = new LayoutBinding[]{
                new LayoutBinding(0, 1, DescriptorType.UNIFORM_BUFFER, ShaderType.VERTEX,null),
        };
        return new DescriptorLayout(device, bindings, 0);
    }

    private Vertices createVertices(VmaAllocator allocator) throws Exception {
        Obj obj;
        try (InputStream file_data = Thread.currentThread().getContextClassLoader().getResourceAsStream("./model/sponza/sponza.obj")) {
            assert file_data != null;
            obj = ObjReader.read(file_data);
            obj = ObjUtils.triangulate(obj);
            obj = ObjUtils.makeTexCoordsUnique(obj);
            obj = ObjUtils.makeVertexIndexed(obj);
            obj = ObjUtils.convertToRenderable(obj);

        }
        IntBuffer index = ObjData.getFaceVertexIndices(obj, 4);
        Buffer buffer = new VertexBuffer(allocator, ObjData.getTotalNumFaceVertices(obj) * 3L, false, VmaMemoryUsage.CPU_TO_GPU).write(ObjData.getVertices(obj));
        Buffer index_buffer = new IndexBuffer(allocator, index.remaining(), false, VmaMemoryUsage.CPU_TO_GPU).write(index);

        VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.calloc(1);
        bindingDescriptor.binding(0).stride(3 * Float.BYTES).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1);
        attributeDescriptions.get(0).binding(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);

        return new Vertices(buffer, index_buffer, bindingDescriptor, attributeDescriptions);
    }

    private Pipeline createPipeline(Device device, RenderPass renderPass, Vertices vertices, DescriptorLayout descriptorLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PipelineLayout.PushConstant[] pushConstant = new PipelineLayout.PushConstant[1];
            pushConstant[0] = new PipelineLayout.PushConstant(ShaderType.VERTEX, 0, Float.BYTES * ((4 * 4) * 2));
            PipelineLayout layout = new PipelineLayout(device, new DescriptorLayout[]{descriptorLayout}, pushConstant);
            Shader[] shaders = new Shader[2];
            shaders[0] = new Shader(device, "shader.vert", ShaderType.VERTEX);
            shaders[1] = new Shader(device, "shader.frag", ShaderType.FRAGMENT);
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

    private CommandBuffer[] createCommandBuffers(Device device, Surface surface, CommandPool commandPool, RenderPass renderPass, Pipeline pipeline, Vertices buffer, DescriptorSets[] descriptorSets) {
        CommandBuffer[] renderCommandBuffers = new CommandBuffer[frame_buffers.length];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < frame_buffers.length; i++) {
                renderCommandBuffers[i] = new CommandBuffer(device, commandPool, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            }

            VkExtent2D extent = VkExtent2D.calloc(stack);
            extent.set(surface.getSurfaceSize());
            VkOffset2D offset = VkOffset2D.calloc(stack);

            for (int i = 0; i < renderCommandBuffers.length; ++i) {
                if (renderCommandBuffers[i].begin(0) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to begin render command buffer");
                    throw new IllegalStateException("Failed to begin render command buffer");
                }

                renderCommandBuffers[i].beginRenderPass(renderPass, extent, offset, frame_buffers[i], VK_SUBPASS_CONTENTS_INLINE);

                VkExtent2D size = surface.getSurfaceSize();
                VkViewport.Buffer viewport = VkViewport.calloc(1, stack).height(size.height()).width(size.width()).minDepth(0.0f).maxDepth(1.0f);
                renderCommandBuffers[i].setViewport(0, viewport);

                VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);

                scissor.extent().set(surface.getSurfaceSize());
                scissor.offset().set(0, 0);
                renderCommandBuffers[i].setScissor(0, scissor);

                renderCommandBuffers[i].bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());
                LongBuffer pDescriptorSets = stack.callocLong(1);
                pDescriptorSets.put(0, descriptorSets[i].getHandle(0));
                renderCommandBuffers[i].bindDescriptorSets(VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, pDescriptorSets, null);

                PushConstants pushConstants = new PushConstants();
                pushConstants.projection = window.getProjection();
                pushConstants.view = camera.getViewMatrix();

                var data = pushConstants.toFloatBuffer();
                renderCommandBuffers[i].pushConstants(pipeline.getLayout(), ShaderType.VERTEX, 0, data);
                MemoryUtil.memFree(data);

                LongBuffer offsets = stack.callocLong(1);
                offsets.put(0, 0L);
                LongBuffer pBuffers = stack.callocLong(1);
                pBuffers.put(0, buffer.buffer().getBuffer().handle());
                renderCommandBuffers[i].bindVertexBuffers(0, pBuffers, offsets);
                renderCommandBuffers[i].bindIndexBuffer(buffer.index_buffer().getBuffer(), 0, VK_INDEX_TYPE_UINT32);
                // this is done like this due to the way buffer size is stored
                renderCommandBuffers[i].drawIndexed((int) buffer.index_buffer().getSize() / 4, 1, 0, 0, 0);
                renderCommandBuffers[i].endRenderPass();

                if (renderCommandBuffers[i].end() != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to begin render command buffer");
                    throw new IllegalStateException("Failed to begin render command buffer");
                }
            }
        }
        return renderCommandBuffers;
    }

    public void run() throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            window = new Window(800, 600, "GLFW Vulkan Demo", (event) -> {
                if (event.getID() == KeyEvent.ID) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.key == GLFW_KEY_W) {
                        camera.update(0, 0, 0.1f, 0, 0, 0);
                    } else if (keyEvent.key == GLFW_KEY_S) {
                        camera.update(0, 0, -0.1f, 0, 0, 0);
                    } else if (keyEvent.key == GLFW_KEY_A) {
                        camera.update(0.1f, 0, 0, 0, 0, 0);
                    } else if (keyEvent.key == GLFW_KEY_D) {
                        camera.update(0.1f, 0, 0, 0, 0, 0);
                    } else if (keyEvent.key == GLFW_KEY_Q) {
                        camera.update(0, 0, 0, 0, 0.1f, 0);
                    } else if (keyEvent.key == GLFW_KEY_E) {
                        camera.update(0, 0, 0, 0, -0.1f, 0);
                    }
                }
            });
            // Create the Vulkan instance
            final Instance instance = new Instance(true, "TriangleDemo");
            final Device device = new Device(instance);
            final VmaAllocator allocator = new VmaAllocator(instance, device);
            final Surface surface = new Surface(window, instance, device);
            // Create static Vulkan resources
            DescriptorPoolSize [] descriptorPoolSizes = {
                    new DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 3),
            };

            final DescriptorPool descriptorPool = new DescriptorPool(device, descriptorPoolSizes, 4,0);
            final DescriptorLayout descriptorLayout = createDescriptorLayout(device);
            final DescriptorSets[] descriptorSets = new DescriptorSets[3];
            for (int i = 0; i < 3; i++) {
                descriptorSets[i] = new DescriptorSets(device, descriptorPool, new DescriptorLayout[]{descriptorLayout});
            }
            final CommandPool commandPool = new CommandPool(device, device.getGraphicsIndex(), true);
            final Queue queue = new Queue(device, device.getGraphicsIndex(), 0);
            final RenderPass renderPass = createRenderPass(device, surface);
            final Vertices vertices = createVertices(allocator);
            final Pipeline pipeline = createPipeline(device, renderPass, vertices, descriptorLayout);
            SwapChain.SwapChainBuilder builder = new SwapChain.SwapChainBuilder();
            builder.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageArrayLayers(1).presentMode(VK_PRESENT_MODE_FIFO_KHR)
                    .clipped().compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);

            final SwapChain SwapChain = new SwapChain(device, surface, allocator, builder);

            final class SwapChainHelper {
                void recreate() {
                    SwapChain.recreate();
                    queue.waitIdle();
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
                    frame_buffers = createFrameBuffers(device, surface, SwapChain, renderPass);
                    if (command_buffers != null) {
                        for (var c : command_buffers) {
                            c.close();
                        }
                        command_buffers = null;
                        commandPool.reset(0);
                    }

                    if(uniform_buffer != null) {
                        uniform_buffer.close();
                    }
                    {
                        final Matrix4f model = new Matrix4f().identity().scale(1.0f);
                        FloatBuffer modelBuffer = MemoryUtil.memCallocFloat(16);
                        modelBuffer = model.get(modelBuffer);
                        uniform_buffer = new UniformBuffer(allocator, (Float.BYTES * 16), false, VmaMemoryUsage.CPU_TO_GPU).write(modelBuffer);
                        MemoryUtil.memFree(modelBuffer);
                        for (int i = 0; i < 3; i++) {
                            VkDescriptorBufferInfo.Buffer buffer_info = VkDescriptorBufferInfo.calloc(1);
                            buffer_info.buffer(uniform_buffer.getBuffer().handle());
                            buffer_info.offset(0);
                            buffer_info.range((Float.BYTES * 16));
                            descriptorSets[i].writeBuffer(0, 0, 0, 1,false, buffer_info);
                            buffer_info.free();
                        }
                    }
                    command_buffers = createCommandBuffers(device, surface, commandPool, renderPass, pipeline, vertices, descriptorSets);
                }
            }
            final SwapChainHelper swap_chain_helper = new SwapChainHelper();
            swap_chain_helper.recreate();
            final IntBuffer pImageIndex = stack.callocInt(1);
            final PointerBuffer pCommandBuffers = stack.callocPointer(1);
            final Semaphore image_acquired = new Semaphore(device);
            final Semaphore render_complete = new Semaphore(device);
            final IntBuffer pWaitDstStageMask = stack.callocInt(1);

            pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

            while (!window.ShouldClose()) {
                window.poll();
                SwapChain.acquireNextImage(null, image_acquired, pImageIndex);
                pCommandBuffers.put(0, command_buffers[pImageIndex.get(0)].getCommandBuffer());
                if (queue.submit(pCommandBuffers, pWaitDstStageMask, new Semaphore[]{image_acquired}, new Semaphore[]{render_complete}, null) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to submit render queue");
                    throw new IllegalStateException("Failed to submit render queue");
                }
                if (SwapChain.queuePresent(queue.getQueue(), new Semaphore[]{render_complete}, pImageIndex) != VK_SUCCESS) {
                    swap_chain_helper.recreate();
                }
                device.waitIdle();
            }
            image_acquired.close();
            render_complete.close();
            for (var f : frame_buffers) {
                f.close();
            }
            uniform_buffer.close();
            commandPool.close();
            for (var d : descriptorSets) {
                d.close();
            }
            descriptorLayout.close();
            descriptorPool.close();
            renderPass.close();
            pipeline.close();
            vertices.buffer().close();
            vertices.index_buffer().close();
            vertices.attributeDescriptions().close();
            vertices.bindingDescriptor().close();
            SwapChain.close();
            surface.close();
            allocator.close();
            device.close();
            instance.close();
            window.close();
        }
    }

    private static class PushConstants {
        Matrix4f view;
        Matrix4f projection;
        //crash fix help from thechubu, gudenau & hilligans on lwjgl discord
        public FloatBuffer toFloatBuffer() {
            FloatBuffer buffer = MemoryUtil.memCallocFloat(32);
            buffer = view.get(buffer);
            buffer = projection.get(16,buffer);
            return buffer;
        }
    }
}