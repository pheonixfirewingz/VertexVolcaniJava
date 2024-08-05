package com.github.vertexvolcani.graphics.vulkan.buffer;

public record ImageSubResourceRange(int aspectMask, int baseMipLevel, int levelCount, int baseArrayLayer,
                                    int layerCount) {
}
