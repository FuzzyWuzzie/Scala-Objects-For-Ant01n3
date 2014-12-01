#version 110

varying vec2 vTexCoords;

uniform sampler2DMS texColor;
//uniform vec3 texSize;

void main(void) {
	ivec2 s = textureSize(texColor);	// integral texture size, often we use only a smaller part of the framebuffer.
	ivec2 p = ivec2(vTexCoords.x * s.x, vTexCoords.y * s.y);
//	ivec2 p = ivec2(vTexCoords.x * (texSize.x - 1), vTexCoords.y * (texSize.y - 1));
//	ivec2 p = ivec2(gl_FragCoord.xy)	// efficient, but works only for fullscreen

	vec4 s0 = texelFetch(texColor, p, 0);
	vec4 s1 = texelFetch(texColor, p, 1);
	vec4 s2 = texelFetch(texColor, p, 2);
	vec4 s3 = texelFetch(texColor, p, 3);

	//gl_FragColor = mix(mix(s0, s1, 0.5), mix(s2, s3, 0.5), 0.5);
	// Faster ?
	gl_FragColor = vec4(s0.r*0.25 + s1.r*0.25 + s2.r*0.25 + s3.r*0.25,
		                s0.g*0.25 + s1.g*0.25 + s2.g*0.25 + s3.g*0.25,
		                s0.b*0.25 + s1.b*0.25 + s2.b*0.25 + s3.b*0.25,
		                s0.a*0.25 + s1.a*0.25 + s2.a*0.25 + s3.a*0.25);
}