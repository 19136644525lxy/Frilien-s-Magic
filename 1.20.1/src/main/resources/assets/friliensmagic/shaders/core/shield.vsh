#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec3 worldNormal;
out vec3 viewPosition;
out float fresnel;

void main() {
    vec4 worldPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * worldPos;

    vertexColor = Color;
    worldNormal = normalize(mat3(ModelViewMat) * Normal);
    viewPosition = worldPos.xyz;

    // 菲涅尔效果：视线与法线越接近垂直，边缘越亮
    vec3 viewDir = normalize(-viewPosition);
    fresnel = pow(1.0 - max(dot(viewDir, worldNormal), 0.0), 3.0);
}
