#version 110

precision highp float;

# define LOG2 0.3010299957

// Size and center of the view on the complex plane.
uniform vec2 size = vec2(3.5, 3);
uniform vec2 center = vec2(-0.75, 0);
// Size of the drawing surface.
uniform vec2 area = vec2(3.5, 3);
// Max number of iterations before considering a point pertains or not to M.
uniform int max_iteration = 256;

varying vec2 P;

vec2 mapCoordinates(in vec2 P) {
	return vec2(((P.x + (area.x/2)) / area.x) * size.x - (size.x/2) + center.x, 
		        ((P.y + (area.y/2)) / area.y) * size.y - (size.y/2) + center.y);
}

float modulus(in vec2 c) {
	return sqrt(c.x*c.x + c.y*c.y);
}

vec2 mult(in vec2 a, in vec2 b) {
	return vec2(a.x*b.x - a.y*b.y, a.x*b.y + a.y*b.x);
}

void main(void) {
	vec2 c = mapCoordinates(P);
	vec2 z = vec2(0, 0);
	float i = 0;

	while(modulus(z) < 2 && i < max_iteration) {
		z = mult(z, z) + c;
		i = i + 1;
	}

	if(i < max_iteration) {
		float zn = modulus(z);
		float nu = log(log(zn) / log(2)) / log(2);
		i = i + 1 - nu;
		float cc = i / max_iteration;
		cc = 1 - pow((1-cc), 8);
		gl_FragColor = vec4(cc, cc, cc, 1);		
	} else {
		gl_FragColor = vec4(1, 1, 1, 1);
	}
}