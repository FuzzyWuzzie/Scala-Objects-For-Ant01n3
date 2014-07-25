#version 110
//precision highp float;
//precision highp int;


varying vec2 X;


uniform sampler2D texColor;
uniform sampler2D texMask;
uniform vec3 sunDir;


uniform float diffuse   = 0.5;
uniform float shininess = 2.0;
uniform float specular  = 0.1;
uniform float ambient   = 0.5;
uniform vec3 viewer = vec3(0.57735, 0.57735, 0.57735);
uniform mat3 rotn   = mat3(
         0.707, -0.408, 0.577,
         0.000,  0.816, 0.577,
        -0.707, -0.408, 0.577);


void main(void) {
	vec3  l = normalize(sunDir);								// Light direction (from light to objects).
	vec4  c = texture2D(texColor, X.st);						// Base color.
	vec4  t = texture2D(texMask, X.st);							// Normal from mask.
	vec3  n = normalize((2.0 * t.xyz - 1.0) * rotn);			// Rotate normals by 45 Y and 45 X (like the view).
	float d = dot(n, l);										// Diffuse coef.
	vec3  r = reflect(-l, n);									// Reflect vector.
	float s = pow(max(dot(r, viewer), 0.0), shininess);			// Specular coef.	Observer is constant.

	d = clamp(d * diffuse + ambient, 0.0, 1.0);					// Mix diffuse and ambient.
	s = s * specular;

	if(t.a > 0.95) 	// We consider a specular always white, so just add 's'. But only for non transparent pixels.
	     gl_FragColor = vec4(c.r * d + s, c.g * d + s, c.b * d + s, c.a);
	else gl_FragColor = vec4(c.r * d,     c.g * d,     c.b * d,     c.a);
}