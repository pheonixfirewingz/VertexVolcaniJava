package org.github.vertexvolcani;
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
import com.github.vertexvolcani.util.Log;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import org.github.vertexvolcani.util.Camera;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
        RenderPass renderPass;
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
            renderPass = new RenderPass(device, renderPassInfo);
        }
        return renderPass;
    }

    private Vertices createVertices(VmaAllocator allocator) throws Exception {
        Obj obj;
        try (InputStream file_data = Thread.currentThread().getContextClassLoader().getResourceAsStream("./model/sponza/sponza.obj")) {
            obj = ObjReader.read(file_data);
            obj = ObjUtils.triangulate(obj);
            obj = ObjUtils.makeTexCoordsUnique(obj);
            obj = ObjUtils.makeVertexIndexed(obj);
            obj = ObjUtils.convertToRenderable(obj);

        }
        IntBuffer index = ObjData.getFaceVertexIndices(obj, 4);
        Buffer buffer = new VertexBuffer(allocator, ObjData.getTotalNumFaceVertices(obj) * 3L, false, VmaMemoryUsage.CPU_TO_GPU).load(ObjData.getVertices(obj));
        Buffer index_buffer = new IndexBuffer(allocator, index.remaining(), false, VmaMemoryUsage.CPU_TO_GPU).load(index);

        VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.calloc(1);
        bindingDescriptor.binding(0).stride(3 * Float.BYTES).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1);
        attributeDescriptions.get(0).binding(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);

        Vertices ret = new Vertices();
        ret.buffer = buffer;
        ret.index_buffer = index_buffer;
        ret.bindingDescriptor = bindingDescriptor;
        ret.attributeDescriptions = attributeDescriptions;
        return ret;
    }

    private Pipeline createPipeline(Device device, RenderPass renderPass, Vertices vertices) throws IOException {
        Pipeline pipeline;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PipelineLayout.PushConstant[] pushConstant = new PipelineLayout.PushConstant[1];
            pushConstant[0] = new PipelineLayout.PushConstant(ShaderType.VERTEX, 0, (Float.BYTES * (4 * 4)) * 2);
            PipelineLayout layout = new PipelineLayout(device, 0, null, pushConstant);
            Shader[] shaders = new Shader[2];
            shaders[0] = new Shader(device, "shader.vert", ShaderType.VERTEX);
            shaders[1] = new Shader(device, "shader.frag", ShaderType.FRAGMENT);
            Pipeline.PipelineBuilder builder = new Pipeline.PipelineBuilder(shaders, layout, renderPass);
            builder.setPrimitiveTopology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            builder.setPolygonMode(VK_POLYGON_MODE_FILL);
            builder.setCullMode(VK_CULL_MODE_NONE);
            builder.setFrontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
            builder.setLineWidth(1.0f);
            builder.setViewport(null, 1);
            builder.setScissor(null, 1);
            // Describes blend modes and color masks
            VkPipelineColorBlendAttachmentState.Buffer colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1, stack).colorWriteMask(0xF); // <- RGBA
            builder.setColourBlendAttachments(colorWriteMask);
            builder.setDepthCompareOp(VK_COMPARE_OP_ALWAYS);
            VkStencilOpState op = VkStencilOpState.calloc(stack);
            op.failOp(VK_STENCIL_OP_KEEP);
            op.passOp(VK_STENCIL_OP_KEEP);
            op.compareOp(VK_COMPARE_OP_ALWAYS);
            builder.setDepthFront(op);
            builder.setDepthBack(op);
            builder.setVertexInputAttribute(vertices.attributeDescriptions);
            builder.setVertexInputBinding(vertices.bindingDescriptor);
            IntBuffer pDynamicStates = stack.callocInt(2);
            pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip();
            builder.setDynamicStates(pDynamicStates);
            builder.setSampleCount(VK_SAMPLE_COUNT_1_BIT);
            pipeline = new Pipeline(device, builder, null, false);
            builder.close();
        }
        return pipeline;
    }

    private CommandBuffer[] createCommandBuffers(Device device, Surface surface, CommandPool commandPool, RenderPass renderPass, Pipeline pipeline, Vertices buffer) {
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

                renderCommandBuffers[i].bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

                PushConstants pushConstants = new PushConstants();
                pushConstants.projection = window.getProjection();
                pushConstants.view = camera.getViewMatrix();

                renderCommandBuffers[i].pushConstants(pipeline, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants.toFloatBuffer(stack));

                LongBuffer offsets = stack.callocLong(1);
                offsets.put(0, 0L);
                LongBuffer pBuffers = stack.callocLong(1);
                pBuffers.put(0, buffer.buffer.getBuffer());
                renderCommandBuffers[i].bindVertexBuffers(0, pBuffers, offsets);
                renderCommandBuffers[i].bindIndexBuffer(buffer.index_buffer, 0, VK_INDEX_TYPE_UINT32);
                renderCommandBuffers[i].drawIndexed((int) buffer.index_buffer.getSize(), 1, 0, 0, 0);
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
        System.out.print(new Matrix4f().identity().scale(0.5f).toString());
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
            final CommandPool commandPool = new CommandPool(device, device.getGraphicsIndex(), true);
            final Queue queue = new Queue(device, device.getGraphicsIndex(), 0);
            final RenderPass renderPass = createRenderPass(device, surface);
            final Vertices vertices = createVertices(allocator);
            final Pipeline pipeline = createPipeline(device, renderPass, vertices);
            SwapChain.SwapChainBuilder builder = new SwapChain.SwapChainBuilder();
            builder.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT).imageArrayLayers(1)
                    .presentMode(VK_PRESENT_MODE_FIFO_KHR).clipped().compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);

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
                    command_buffers = createCommandBuffers(device, surface, commandPool, renderPass, pipeline, vertices);
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

            final Buffer[] uniform_buffer = new Buffer[command_buffers.length];
            for (int i = 0; i < uniform_buffer.length; ++i) {
                //size of uniform class
                uniform_buffer[i] = new UniformBuffer(allocator, (Float.BYTES * 16), false, VmaMemoryUsage.CPU_TO_GPU);
            }

            while (!window.ShouldClose()) {
                window.poll();
                SwapChain.acquireNextImage(null, image_acquired, pImageIndex);
                pCommandBuffers.put(0, command_buffers[pImageIndex.get(0)].getCommandBuffer());
                if (queue.submit(pCommandBuffers, pWaitDstStageMask, image_acquired.getSemaphorePtr(), render_complete.getSemaphorePtr(), null) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to submit render queue");
                    throw new IllegalStateException("Failed to submit render queue");
                }
                if (SwapChain.queuePresent(queue.getQueue(), render_complete, pImageIndex) != VK_SUCCESS) {
                    swap_chain_helper.recreate();
                }
                queue.waitIdle();
            }
            image_acquired.close();
            render_complete.close();
            for (var f : frame_buffers) {
                f.close();
            }
            for (var c : command_buffers) {
                c.close();
            }
            for (var u : uniform_buffer) {
                u.close();
            }
            commandPool.close();
            renderPass.close();
            pipeline.close();
            vertices.buffer.close();
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

        public ByteBuffer toFloatBuffer(MemoryStack stack) {
            ByteBuffer buffer = stack.calloc((Float.BYTES * (4 * 4)) * 2);
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    buffer.putFloat(view.get(i, j));
                }
            }
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    buffer.putFloat(projection.get(i, j));
                }
            }
            buffer.flip();
            return buffer;
        }
    }

    private static class UniformBufferData {
        Matrix4f model;
    }

    private static class Vertices {
        Buffer buffer;
        Buffer index_buffer;
        VkVertexInputBindingDescription.Buffer bindingDescriptor;
        VkVertexInputAttributeDescription.Buffer attributeDescriptions;
    }
}