#version 110

varying vec2 vTexCoords;

uniform sampler2DMS texColor;

void main(void) {
	ivec2 s = textureSize(texColor);
	ivec2 p = ivec2(round(vTexCoords.x * (s.x-1)), round(vTexCoords.y * (s.y-1)));
//	ivec2 p = ivec2(gl_FragCoord.xy)	// efficient, but works only for fullscreen

	vec4 s0 = texelFetch(texColor, p, 0);
	vec4 s1 = texelFetch(texColor, p, 1);
	vec4 s2 = texelFetch(texColor, p, 2);
	vec4 s3 = texelFetch(texColor, p, 3);
	vec4 s4 = texelFetch(texColor, p, 4);
	vec4 s5 = texelFetch(texColor, p, 5);
	vec4 s6 = texelFetch(texColor, p, 6);
	vec4 s7 = texelFetch(texColor, p, 7);

	gl_FragColor = mix(mix(mix(s0, s1, 0.5), mix(s2, s3, 0.5), 0.5), mix(mix(s4, s5, 0.5), mix(s6, s7, 0.5), 0.5), 0.5);
}