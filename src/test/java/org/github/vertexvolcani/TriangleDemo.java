package org.github.vertexvolcani;

import com.github.vertexvolcani.Window;
import com.github.vertexvolcani.graphics.vulkan.*;
import com.github.vertexvolcani.graphics.vulkan.buffer.Buffer;
import com.github.vertexvolcani.graphics.vulkan.buffer.CommandBuffer;
import com.github.vertexvolcani.graphics.vulkan.buffer.CommandPool;
import com.github.vertexvolcani.graphics.vulkan.buffer.FrameBuffer;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Pipeline;
import com.github.vertexvolcani.graphics.vulkan.pipeline.PipelineLayout;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Shader;
import com.github.vertexvolcani.message.Dispatcher;
import com.github.vertexvolcani.message.IHasDispatcher;
import com.github.vertexvolcani.message.IListener;
import com.github.vertexvolcani.message.WindowCanClosingEvent;
import com.github.vertexvolcani.message.events.IEvent;
import com.github.vertexvolcani.message.events.WindowIsClosingEvent;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.github.vertexvolcani.ShaderCUtil.glslToSpirv;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class TriangleDemo implements IListener, IHasDispatcher {
    private static boolean should_stop = false;
    private final Dispatcher dispatcher = new Dispatcher();
    private FrameBuffer[] frame_buffers;
    private Device device;
    private Surface surface;
    private VmaAllocator allocator;
    private SwapChain SwapChain;
    private CommandBuffer[] command_buffers;

    public static void main(String[] args) throws IOException {
        TriangleDemo demo = new TriangleDemo();
        demo.run();
    }

    private FrameBuffer[] createFrameBuffers(Device device, SwapChain SwapChain, RenderPass renderPass) {
        FrameBuffer[] frame_buffers = new FrameBuffer[SwapChain.getImages().length];
        for (int i = 0; i < SwapChain.getImages().length; i++) {
            frame_buffers[i] = new FrameBuffer(device, renderPass, SwapChain.getImagesViews()[i], surface);
        }
        return frame_buffers;
    }

    private Shader loadShader(Device device,String classPath, int stage) throws IOException {
        return new Shader(device,glslToSpirv(classPath, stage),stage);
    }

    @Override
    public void eventExe(IEvent event) {
        if (event.getID() == WindowIsClosingEvent.ID) {
            should_stop = true;
        }
    }

    private ColorFormatAndSpace getColorFormatAndSpace(Surface surface) {
        VkSurfaceFormatKHR.Buffer surfFormats = surface.getSurfaceFormats();
        int colorFormat;
        if (surfFormats.remaining() == 1 && surfFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
            colorFormat = VK_FORMAT_B8G8R8A8_UNORM;
        } else {
            colorFormat = surfFormats.get(0).format();
        }
        int colorSpace = surfFormats.get(0).colorSpace();
        ColorFormatAndSpace ret = new ColorFormatAndSpace();
        ret.colorFormat = colorFormat;
        ret.colorSpace = colorSpace;
        return ret;
    }

    private RenderPass createRenderPass(int colorFormat) {
        RenderPass renderPass;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack).format(colorFormat).samples(VK_SAMPLE_COUNT_1_BIT).loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE).stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkSubpassDescription.Buffer sub_pass = VkSubpassDescription.calloc(1, stack).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).colorAttachmentCount(colorReference.remaining()).pColorAttachments(colorReference);
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack).srcSubpass(VK_SUBPASS_EXTERNAL).srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack).sType$Default().pAttachments(attachments).pSubpasses(sub_pass).pDependencies(dependency);
            renderPass = new RenderPass(device, renderPassInfo);
        }
        return renderPass;
    }

    private Vertices createVertices() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertexBuffer = stack.calloc(3 * 2 * 4);
            FloatBuffer fb = vertexBuffer.asFloatBuffer();
            fb.put(-0.5f).put(-0.5f);
            fb.put(0.5f).put(-0.5f);
            fb.put(0.0f).put(0.5f);

            Buffer buffer = new Buffer(device, allocator, 65536, false, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VMA_MEMORY_USAGE_CPU_TO_GPU);
            buffer.load(vertexBuffer);

            VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.calloc(1);
            bindingDescriptor.binding(0).stride(2 * 4).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1);
            attributeDescriptions.get(0).binding(0).location(0).format(VK_FORMAT_R32G32_SFLOAT).offset(0);

            Vertices ret = new Vertices();
            ret.verticesBuf = buffer;
            ret.bindingDescriptor = bindingDescriptor;
            ret.attributeDescriptions = attributeDescriptions;
            return ret;
        }
    }

    private Pipeline createPipeline(RenderPass renderPass, Vertices vertices) throws IOException {
        Pipeline pipeline;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PipelineLayout layout = new PipelineLayout(device, 0, null, null);
            Shader[] shaders = new Shader[2];
            shaders[0] = loadShader(device, "shader.vert", VK_SHADER_STAGE_VERTEX_BIT);
            shaders[1] = loadShader(device, "shader.frag", VK_SHADER_STAGE_FRAGMENT_BIT);
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
            builder.cleanup();
        }
        return pipeline;
    }

    private CommandBuffer[] createCommandBuffers(CommandPool commandPool, RenderPass renderPass, Pipeline pipeline, long verticesBuf) {
        CommandBuffer[] renderCommandBuffers = new CommandBuffer[frame_buffers.length];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < frame_buffers.length; i++) {
                renderCommandBuffers[i] = new CommandBuffer(device, commandPool, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            }

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(0, 100 / 255.0f).float32(1, 149 / 255.0f).float32(2, 237 / 255.0f).float32(3, 1.0f);

            VkExtent2D extent = VkExtent2D.calloc(stack);
            extent.set(surface.getSurfaceSize());
            VkOffset2D offset = VkOffset2D.calloc(stack);

            for (int i = 0; i < renderCommandBuffers.length; ++i) {
                if (renderCommandBuffers[i].begin(0) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to begin render command buffer");
                    throw new IllegalStateException("Failed to begin render command buffer");
                }
                renderCommandBuffers[i].beginRenderPass(renderPass, clearValues, extent, offset, frame_buffers[i].getFrameBuffer(), VK_SUBPASS_CONTENTS_INLINE);

                VkExtent2D size = surface.getSurfaceSize();
                VkViewport.Buffer viewport = VkViewport.calloc(1, stack).height(size.height()).width(size.width()).minDepth(0.0f).maxDepth(1.0f);
                renderCommandBuffers[i].setViewport(0, viewport);

                VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);

                scissor.extent().set(surface.getSurfaceSize());
                scissor.offset().set(0, 0);
                renderCommandBuffers[i].setScissor(0, scissor);

                renderCommandBuffers[i].bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

                LongBuffer offsets = stack.callocLong(1);
                offsets.put(0, 0L);
                LongBuffer pBuffers = stack.callocLong(1);
                pBuffers.put(0, verticesBuf);
                renderCommandBuffers[i].bindVertexBuffers(0, pBuffers, offsets);
                renderCommandBuffers[i].draw(3, 1, 0, 0);
                renderCommandBuffers[i].endRenderPass();

                if (renderCommandBuffers[i].end() != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to begin render command buffer");
                    throw new IllegalStateException("Failed to begin render command buffer");
                }
            }
        }
        return renderCommandBuffers;
    }

    public void run() throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            new Window(800, 600, "GLFW Vulkan Demo").start();
            while (Window.getWindow() == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
            Window.getWindow().subscribe(this);
            subscribe(Window.getWindow());
            // Create the Vulkan instance
            final Instance instance = new Instance(true, "TriangleDemo");
            device = new Device(instance);
            allocator = new VmaAllocator(instance, device);
            surface = new Surface(Window.getWindow(), instance, device);
            // Create static Vulkan resources
            final ColorFormatAndSpace colorFormatAndSpace = getColorFormatAndSpace(surface);
            final CommandPool commandPool = new CommandPool(device, device.getGraphicsIndex(), VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            final VkQueue queue = device.getDeviceQueue(device.getGraphicsIndex(), 0);
            final RenderPass renderPass = createRenderPass(colorFormatAndSpace.colorFormat);
            final Vertices vertices = createVertices();
            final Pipeline pipeline = createPipeline(renderPass, vertices);

            SwapChain.SwapChainBuilder builder = new SwapChain.SwapChainBuilder();
            builder.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT).imageArrayLayers(1).presentMode(VK_PRESENT_MODE_FIFO_KHR).clipped().compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);

            SwapChain = new SwapChain(device, surface, builder, colorFormatAndSpace.colorFormat, colorFormatAndSpace.colorSpace);
            final class SwapChainHelper {
                void recreate() {
                    SwapChain.recreate();
                    vkQueueWaitIdle(queue);
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
                    frame_buffers = createFrameBuffers(device, SwapChain, renderPass);
                    if (command_buffers != null) {
                        for (var c:command_buffers){
                            c.cleanup();
                        }
                        command_buffers = null;
                        commandPool.reset(0);
                    }
                    command_buffers = createCommandBuffers(commandPool, renderPass, pipeline, vertices.verticesBuf.getBuffer());
                }
            }
            final SwapChainHelper swap_chain_helper = new SwapChainHelper();
            swap_chain_helper.recreate();
            IntBuffer pImageIndex = stack.callocInt(1);
            PointerBuffer pCommandBuffers = stack.callocPointer(1);
            Semaphore pImageAcquiredSemaphore = new Semaphore(device);
            Semaphore pRenderCompleteSemaphore = new Semaphore(device);

            IntBuffer pWaitDstStageMask = stack.callocInt(1);
            pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType$Default().waitSemaphoreCount(1).pWaitSemaphores(pImageAcquiredSemaphore.getSemaphorePtr()).pWaitDstStageMask(pWaitDstStageMask).pCommandBuffers(pCommandBuffers).pSignalSemaphores(pRenderCompleteSemaphore.getSemaphorePtr());

            while (!should_stop) {
                SwapChain.acquireNextImage(null, pImageAcquiredSemaphore, pImageIndex);
                int currentBuffer = pImageIndex.get(0);
                pCommandBuffers.put(0, command_buffers[currentBuffer].getCommandBuffer());
                if (vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: Failed to submit render queue");
                    throw new IllegalStateException("Failed to submit render queue");
                }
                if (SwapChain.queuePresent(queue, pRenderCompleteSemaphore, pImageIndex) != VK_SUCCESS) {
                    swap_chain_helper.recreate();
                }
                vkQueueWaitIdle(queue);
            }
            dispatcher.dispatch(new WindowCanClosingEvent());
            while (Window.getWindow() != null) {
                try {
                    Thread.sleep(50);
                } catch (Exception ignored) {
                }
            }
            pImageAcquiredSemaphore.cleanup();
            pRenderCompleteSemaphore.cleanup();
            for(var f:frame_buffers){
                f.cleanup();
            }
            for(var c: command_buffers){
                c.cleanup();
            }
            commandPool.cleanup();
            renderPass.cleanup();
            pipeline.cleanup();
            vertices.verticesBuf.cleanup();
            SwapChain.cleanup();
            surface.cleanup();
            allocator.cleanup();
            device.cleanup();
            instance.cleanup();
        }
    }

    @Override
    public void subscribe(IListener listener) {
        dispatcher.subscribe(listener);
    }

    @Override
    public void unSubscribe(IListener listener) {
        dispatcher.unSubscribe(listener);
    }

    private static class Vertices {
        Buffer verticesBuf;
        VkVertexInputBindingDescription.Buffer bindingDescriptor;
        VkVertexInputAttributeDescription.Buffer attributeDescriptions;
    }

    private static class ColorFormatAndSpace {
        int colorFormat;
        int colorSpace;
    }
}