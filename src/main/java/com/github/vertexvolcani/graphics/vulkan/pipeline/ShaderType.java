package com.github.vertexvolcani.graphics.vulkan.pipeline;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import static org.lwjgl.vulkan.VK10.*;

public enum ShaderType {
    VERTEX(VK_SHADER_STAGE_VERTEX_BIT),
    FRAGMENT(VK_SHADER_STAGE_FRAGMENT_BIT),
    GEOMETRY(VK_SHADER_STAGE_GEOMETRY_BIT),
    COMPUTE(VK_SHADER_STAGE_COMPUTE_BIT),
    VERTEX_AND_FRAGMENT(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT),
    VERTEX_AND_GEOMETRY(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_GEOMETRY_BIT),
    VERTEX_AND_COMPUTE(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_COMPUTE_BIT),
    FRAGMENT_AND_GEOMETRY(VK_SHADER_STAGE_FRAGMENT_BIT | VK_SHADER_STAGE_GEOMETRY_BIT),
    FRAGMENT_AND_COMPUTE(VK_SHADER_STAGE_FRAGMENT_BIT | VK_SHADER_STAGE_COMPUTE_BIT),
    GEOMETRY_AND_COMPUTE(VK_SHADER_STAGE_GEOMETRY_BIT | VK_SHADER_STAGE_COMPUTE_BIT);

    private final int value;

    ShaderType(int value_in) {
        value = value_in;
    }

    public int getValue() {
        return value;
    }
}
