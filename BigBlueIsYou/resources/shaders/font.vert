#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;

uniform mat4 mProjection;
uniform mat4 mModel;
uniform vec3 color;

out vec2 outTexCoord;
out vec3 outColor;

void main()
{
    gl_Position = mProjection * mModel * vec4(position, 1.0);
    outTexCoord = texCoord;
    outColor = color;
}