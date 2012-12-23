#version 110
//precision highp float;
//precision highp int;

varying vec4 vColor;

//uniform sampler2D texColor;

void main(void) {
//	vec4 color = texture2D(texColor, gl_PointCoord);
//	gl_FragColor = vec4(color.rgb, 1);
	gl_FragColor = vColor;
}
