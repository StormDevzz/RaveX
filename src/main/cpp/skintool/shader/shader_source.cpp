const char* vertSource() {
    return R"(
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;

uniform mat4 uProj;
uniform mat4 uView;
uniform mat4 uModel;

out vec3 FragPos;
out vec3 Normal;
out vec2 UV;

void main() {
    FragPos = vec3(uModel * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(uModel))) * aNormal;
    UV = aUV;
    gl_Position = uProj * uView * uModel * vec4(aPos, 1.0);
}
)";
}

const char* fragSource() {
    return R"(
#version 330 core
in vec3 FragPos;
in vec3 Normal;
in vec2 UV;

uniform sampler2D uTex;
uniform bool uWireframe;
uniform vec3 uLightDir;

out vec4 FragColor;

void main() {
    if (uWireframe) {
        FragColor = vec4(1.0, 1.0, 1.0, 1.0);
        return;
    }
    vec4 texColor = texture(uTex, UV);
    if (texColor.a < 0.01) discard;
    float diff = max(dot(normalize(Normal), normalize(uLightDir)), 0.15);
    FragColor = vec4(texColor.rgb * diff, texColor.a);
}
)";
}
