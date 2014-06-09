#version 110

varying vec2 vTexCoords;

uniform sampler2D texColor;
uniform sampler2D texMask;
uniform vec3 lightDir;

void main(void) {
	vec4  col = texture2D(texColor, vTexCoords.st);
	vec3  msk = texture2D(texMask, vTexCoords.st).xyz;
	float dif = max(dot(msk,normalize(lightDir)),0.0);

	dif *= 0.5;
	dif += 0.5;

	dif = max(dif, 0.0);
	dif = min(dif, 1.0);

	gl_FragColor = vec4(col.r * dif, col.g * dif, col.b * dif, col.a);
}