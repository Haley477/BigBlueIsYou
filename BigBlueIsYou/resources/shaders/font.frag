#version 330

in vec2 outTexCoord;
in vec3 outColor;
out vec4 fragColor;

uniform sampler2D texSampler;

void main()
{
    fragColor = texture(texSampler, outTexCoord) * vec4(outColor, 1);
}
