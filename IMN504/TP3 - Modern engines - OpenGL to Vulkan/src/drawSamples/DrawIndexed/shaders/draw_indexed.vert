#version 460
#extension GL_ARB_shader_draw_parameters : enable

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in mat4 instanceModel;
layout(location = 6) in int inTypeID;

layout(location = 0) out vec3 fragNormal;
layout(location = 1) flat out int outTypeID;

layout(binding = 0, set = 0) uniform ViewUBO {
    mat4 view;
    mat4 projection;
    vec4 cameraPosition;
    float near;
    float far;
} viewUbo;

void main() {
    outTypeID = inTypeID;

    vec4 worldPos = instanceModel * vec4(inPosition, 1.0);
    gl_Position = viewUbo.projection * viewUbo.view * worldPos;

    fragNormal = mat3(instanceModel) * inNormal;
}
