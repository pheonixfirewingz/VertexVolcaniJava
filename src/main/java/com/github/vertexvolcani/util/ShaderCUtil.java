package com.github.vertexvolcani.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.KHRRayTracingPipeline.*;
import static org.lwjgl.vulkan.VK10.*;
/**
 * Utility functions for shaderc. taken from lwjgl3 demos
 * @author Kai Burjack
 */
public class ShaderCUtil {
    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null)
            throw new IOException("Classpath resource not found: " + resource);
        File file = new File(url.getFile());
        if (file.isFile()) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            fc.close();
            fis.close();
        } else {
            buffer = BufferUtils.createByteBuffer(bufferSize);
            InputStream source = url.openStream();
            try (source) {
                if (source == null)
                    throw new FileNotFoundException(resource);
                byte[] buf = new byte[8192];
                while (true) {
                    int bytes = source.read(buf, 0, buf.length);
                    if (bytes == -1)
                        break;
                    if (buffer.remaining() < bytes)
                        buffer = resizeBuffer(buffer, Math.max(buffer.capacity() * 2, buffer.capacity() - buffer.remaining() + bytes));
                    buffer.put(buf, 0, bytes);
                }
                buffer.flip();
            }
        }
        return buffer;
    }

    private static int vulkanStageToShadercKind(int stage) {
        return switch (stage) {
            case VK_SHADER_STAGE_VERTEX_BIT -> shaderc_vertex_shader;
            case VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT -> shaderc_tess_control_shader;
            case VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT -> shaderc_tess_evaluation_shader;
            case VK_SHADER_STAGE_GEOMETRY_BIT -> shaderc_geometry_shader;
            case VK_SHADER_STAGE_FRAGMENT_BIT -> shaderc_fragment_shader;
            case VK_SHADER_STAGE_COMPUTE_BIT -> shaderc_compute_shader;
            case VK_SHADER_STAGE_RAYGEN_BIT_KHR -> shaderc_raygen_shader;
            case VK_SHADER_STAGE_ANY_HIT_BIT_KHR -> shaderc_anyhit_shader;
            case VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR -> shaderc_closesthit_shader;
            case VK_SHADER_STAGE_MISS_BIT_KHR -> shaderc_miss_shader;
            case VK_SHADER_STAGE_INTERSECTION_BIT_KHR -> shaderc_intersection_shader;
            case VK_SHADER_STAGE_CALLABLE_BIT_KHR -> shaderc_callable_shader;
            default -> throw new IllegalArgumentException("Stage: " + stage);
        };
    }

    public static ByteBuffer glslToSpirv(String classPath, int vulkanStage,boolean debug) throws IOException {
        ByteBuffer src = ioResourceToByteBuffer(classPath, 1024);
        long compiler = shaderc_compiler_initialize();
        long options = shaderc_compile_options_initialize();
        ShadercIncludeResolve resolver;
        ShadercIncludeResultRelease releaser;
        shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2);
        shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_5);
        shaderc_compile_options_set_optimization_level(options, debug ?shaderc_optimization_level_zero:shaderc_optimization_level_performance);
        shaderc_compile_options_set_include_callbacks(options, resolver = new ShadercIncludeResolve() {
            public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
                ShadercIncludeResult res = ShadercIncludeResult.calloc();
                try {
                    String src = classPath.substring(0, classPath.lastIndexOf('/')) + "/" + memUTF8(requested_source);
                    res.content(ioResourceToByteBuffer(src, 1024));
                    res.source_name(memUTF8(src));
                    return res.address();
                } catch (IOException e) {
                    throw new AssertionError("Failed to resolve include: " + src);
                }
            }
        }, releaser = new ShadercIncludeResultRelease() {
            public void invoke(long user_data, long include_result) {
                ShadercIncludeResult result = ShadercIncludeResult.create(include_result);
                memFree(result.source_name());
                result.free();
            }
        }, 0L);
        long res;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            res = shaderc_compile_into_spv(compiler, src, vulkanStageToShadercKind(vulkanStage), stack.UTF8(classPath), stack.UTF8("main"), options);
            if (res == 0L)
                throw new AssertionError("Internal error during compilation!");
        }
        if (shaderc_result_get_compilation_status(res) != shaderc_compilation_status_success) {
            throw new AssertionError("Shader compilation failed: " + shaderc_result_get_error_message(res));
        }
        int size = (int) shaderc_result_get_length(res);
        ByteBuffer resultBytes = createByteBuffer(size);
        resultBytes.put(shaderc_result_get_bytes(res));
        resultBytes.flip();
        shaderc_result_release(res);
        shaderc_compiler_release(compiler);
        releaser.free();
        resolver.free();
        return resultBytes;
    }
}