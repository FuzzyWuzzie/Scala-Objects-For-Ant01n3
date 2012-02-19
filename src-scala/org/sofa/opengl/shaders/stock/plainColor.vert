#version 330

// Input:
layout(location=0) in vec3 pos;			// Position.
layout(location=1) in vec4 color;		// Color.

// Global:
uniform mat4 MVP;						// Perspective * View * Model.
uniform mat4 MV;						// View * Model.
uniform mat3 MV3x3;						// Upper 3x3 MV, without scaling and translation.

smooth out vec4 C;						// Color out.

void main(void) {
	C           = color;
	gl_Position = MVP * vec4(pos, 1);
}
