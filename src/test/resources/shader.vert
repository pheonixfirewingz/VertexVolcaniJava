#version 450 core

layout(location=0) in vec3 position;

layout(set=0,binding=0) uniform UBO {
  mat4 model;
} ubo;

layout(push_constant) uniform PushConsts {
  mat4 view;
  mat4 proj;
} pushConsts;

void main(void) {
  // Transform the vertex position using the model, view, and projection matrices
  vec4 worldPosition = ubo.model * vec4(position, 1.0);  // Scale matrix with a scaling factor of 0.3
  vec4 viewPosition = pushConsts.view * worldPosition;
  gl_Position = pushConsts.proj * viewPosition;
}
