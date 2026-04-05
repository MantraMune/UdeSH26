#version 460
#extension GL_ARB_shader_draw_parameters : enable

layout(location = 0) in vec3 fragNormal;
layout(location = 1) flat in int inTypeID;

layout(location = 0) out vec4 outColor;

void main() {
    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    float diffuse = max(dot(normalize(fragNormal), lightDir), 0.0);

    vec3 baseColor = vec3(0.6, 0.6, 0.6);

    vec3 finalColor = baseColor * (0.2 + 0.8 * diffuse);

    outColor = vec4(finalColor, 1.0);
}
