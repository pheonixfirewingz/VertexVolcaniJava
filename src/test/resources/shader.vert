#version 450 core

layout(location=0) in vec3 position;

layout(push_constant) uniform PushConsts {
  mat4 view;
  mat4 proj;
} pushConsts;

void main(void) {
  // Transform the vertex position using the model, view, and projection matrices
  vec4 worldPosition = mat4(1.0) * vec4(position, 1.0);
  vec4 viewPosition = pushConsts.view * worldPosition;
  gl_Position = pushConsts.proj * viewPosition;
}