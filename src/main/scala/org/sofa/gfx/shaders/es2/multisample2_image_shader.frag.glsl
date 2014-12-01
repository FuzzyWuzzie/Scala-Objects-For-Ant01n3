#version 110

varying vec2 vTexCoords;

uniform sampler2DMS texColor;

void main(void) {
	ivec2 s = textureSize(texColor);
	ivec2 p = ivec2(vTexCoords.x * s.x, vTexCoords.y * s.y);
//	ivec2 p = ivec2(gl_FragCoord.xy)	// efficient, but works only for fullscreen

	vec4 s0 = texelFetch(texColor, p, 0);
	vec4 s1 = texelFetch(texColor, p, 1);

	//gl_FragColor = mix(s0, s1, 0.5);
	// Faster ?
	gl_FragColor = vec4(s0.r * 0.5 + s1.r * 0.5,
	                    s0.g * 0.5 + s1.g * 0.5,
	                    s0.b * 0.5 + s1.b * 0.5,
	                    s0.a * 0.5 + s1.a * 0.5);
}