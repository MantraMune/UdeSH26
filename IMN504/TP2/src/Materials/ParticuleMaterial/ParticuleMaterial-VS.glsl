#version 460

uniform mat4 Model;
uniform mat4 ViewProj;
uniform vec3 PosCam;
uniform vec3 PosLum;
uniform float Time;

 out gl_PerVertex {
        vec4 gl_Position;
        float gl_PointSize;
        float gl_ClipDistance[];
    };

layout (location = 0) in vec3 Position;
layout (location = 2) in vec3 Normal;
layout (location = 3) in vec3 texCoords;

out VTF {
vec3 vL;
vec3 vV;
vec3 vN;
vec3 v_Color;
vec2 uv;
};

// -- SSBO pour positions des particules --
layout(std430, binding = 0) readonly buffer PosSrc
{
    vec4 positions[];
};

// -- SSBO pour couleurs (debug) --
layout(std430, binding = 1) readonly buffer ColorSrc
{
    vec4 colors[];
};

void main()
{
    vec3 particulePos = positions[gl_InstanceID].xyz;

    // Ajout de la position de la particule à la position du sommet
    vec3 Pos = Position + particulePos;

    // Position finale
	gl_Position = ViewProj * Model * vec4(Pos,1.0);

 	vN =normalize(Normal);

    vL = (PosLum-Pos);
    vV = (PosCam-Pos);
    uv = texCoords.xy;

    // Couleur debug
    v_Color = colors[gl_InstanceID].xyz;

}
