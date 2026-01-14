#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec3 color;

uniform mat4 mProjection;
uniform mat4 mModel;

out vec3 outColor;

void main()
{
    gl_Position = mProjection * mModel * vec4(position, 1.0);
    outColor = color;
}