package com.github.vertexvolcani.graphics.vulkan.buffer;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import static org.lwjgl.util.vma.Vma.*;

public enum VmaMemoryUsage {
    UNKNOWN              (VMA_MEMORY_USAGE_UNKNOWN),
    GPU_ONLY             (VMA_MEMORY_USAGE_GPU_ONLY),
    CPU_ONLY             (VMA_MEMORY_USAGE_CPU_ONLY),
    CPU_TO_GPU          (VMA_MEMORY_USAGE_CPU_TO_GPU),
    GPU_TO_CPU          (VMA_MEMORY_USAGE_GPU_TO_CPU),
    CPU_COPY             (VMA_MEMORY_USAGE_CPU_COPY),
    GPU_LAZILY_ALLOCATED (VMA_MEMORY_USAGE_GPU_LAZILY_ALLOCATED),
    AUTO                 (VMA_MEMORY_USAGE_AUTO),
    AUTO_PREFER_DEVICE   (VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE),
    AUTO_PREFER_HOST     (VMA_MEMORY_USAGE_AUTO_PREFER_HOST);

    private final int memory_type;

    VmaMemoryUsage(int memory_type_in) {
        memory_type = memory_type_in;
    }

    public int getMemoryType() {
        return memory_type;
    }
}
