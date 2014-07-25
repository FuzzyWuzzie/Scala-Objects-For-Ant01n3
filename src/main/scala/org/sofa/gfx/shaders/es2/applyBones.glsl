/** Compute the bones displacement to a position, a normal for the 
  * current vertex.
  *
  * The current vertex position must be in a "vec3 position" attribute.
  * The current vertex normal must be in a "vec3 normal" attribute.
  * The bones indices must be in a "vec4 boneIndex" attribute.
  * The bones must be in a in a "Bone bone[N]" attribute.
  * The bones weights must be in a "vec4 boneWeight[N]" attribute.
  *
  * The inout parameter P will contain the new vertex position, as deformed by bones.
  * The inout parameter N will contain the new vertex normal, as deformed by bones.
  * The inout parameter C will contain the mix of bones color for the vertex proportionnal to bone influence.
  */
void applyBonesWithColor(inout vec4 P, inout vec3 N, inout vec4 C) {
	int   b0 = int(boneIndex.x);
	int   b1 = int(boneIndex.y);
	int   b2 = int(boneIndex.z);
	int   b3 = int(boneIndex.w);

	P = vec4(0,0,0,0);
	N = vec3(0,0,0);
	C = vec4(0,0,0,0);

	if(b0 >= 0) {
		P += bone[b0].MV * vec4(position,1) * boneWeight.x;
		N += bone[b0].MV3x3 * normal * boneWeight.x;
		C += bone[b0].color * boneWeight.x;
	}
	if(b1 >= 0) {
		P += bone[b1].MV * vec4(position,1) * boneWeight.y;
		N += bone[b1].MV3x3 * normal * boneWeight.y;
		C += bone[b1].color * boneWeight.y;
	} 
	if(b2 >= 0) {
		P += bone[b2].MV * vec4(position,1) * boneWeight.z;
		N += bone[b2].MV3x3 * normal * boneWeight.z;
		C += bone[b2].color * boneWeight.z;
	} 
	if(b3 >= 0) {
		P += bone[b3].MV * vec4(position,1) * boneWeight.w;
		N += bone[b3].MV3x3 * normal * boneWeight.w;
		C += bone[b3].color * boneWeight.w;
	}
}

/** Compute the bones displacement to a position, a normal for the 
  * current vertex.
  *
  * The current vertex position must be in a "vec3 position" attribute.
  * The current vertex normal must be in a "vec3 normal" attribute.
  * The bones indices must be in a "vec4 boneIndex" attribute.
  * The bones must be in a in a "Bone bone[N]" attribute.
  * The bones weights must be in a "vec4 boneWeight[N]" attribute.
  *
  * The inout parameter P will contain the new vertex position, as deformed by bones.
  * The inout parameter N will contain the new vertex normal, as deformed by bones.
  */
void applyBones(in vec3 position, in vec3 normal, out vec4 P, out vec3 N) {
	int   b0 = int(boneIndex.x);
	int   b1 = int(boneIndex.y);
	int   b2 = int(boneIndex.z);
	int   b3 = int(boneIndex.w);

	P = vec4(0,0,0,0);
	N = vec3(0,0,0);

	if(b0 >= 0) {
		P += bone[b0].MV * vec4(position,1) * boneWeight.x;
		N += bone[b0].MV3x3 * normal * boneWeight.x;
	}
	if(b1 >= 0) {
		P += bone[b1].MV * vec4(position,1) * boneWeight.y;
		N += bone[b1].MV3x3 * normal * boneWeight.y;
	} 
	if(b2 >= 0) {
		P += bone[b2].MV * vec4(position,1) * boneWeight.z;
		N += bone[b2].MV3x3 * normal * boneWeight.z;
	} 
	if(b3 >= 0) {
		P += bone[b3].MV * vec4(position,1) * boneWeight.w;
		N += bone[b3].MV3x3 * normal * boneWeight.w;
	}
}


/** Compute the bones displacement to a position, a normal for the 
  * current vertex.
  *
  * The bones indices must be in a "vec4 boneIndex" attribute.
  * The bones must be in a in a "Bone bone[N]" attribute.
  * The bones weights must be in a "vec4 boneWeight[N]" attribute.
  *
  * The inout parameter P will contain the new vertex position, as deformed by bones.
  * The inout parameter N will contain the new vertex normal, as deformed by bones.
  * The inout parameter B will contain the new vertex tangent, as deformed by bones.
  */
void applyBones(in vec3 position, in vec3 normal, in vec3 tangent, out vec4 P, out vec3 N, out vec3 T) {
	int   b0 = int(boneIndex.x);
	int   b1 = int(boneIndex.y);
	int   b2 = int(boneIndex.z);
	int   b3 = int(boneIndex.w);

	P = vec4(0,0,0,0);
	N = vec3(0,0,0);
	T = vec3(0,0,0);

	if(b0 >= 0) {
		P += bone[b0].MV * vec4(position,1) * boneWeight.x;
		N += bone[b0].MV3x3 * normal * boneWeight.x;
		T += bone[b0].MV3x3 * tangent * boneWeight.x;
	}
	if(b1 >= 0) {
		P += bone[b1].MV * vec4(position,1) * boneWeight.y;
		N += bone[b1].MV3x3 * normal * boneWeight.y;
		T += bone[b1].MV3x3 * tangent * boneWeight.y;
	} 
	if(b2 >= 0) {
		P += bone[b2].MV * vec4(position,1) * boneWeight.z;
		N += bone[b2].MV3x3 * normal * boneWeight.z;
		T += bone[b2].MV3x3 * tangent * boneWeight.z;
	} 
	if(b3 >= 0) {
		P += bone[b3].MV * vec4(position,1) * boneWeight.w;
		N += bone[b3].MV3x3 * normal * boneWeight.w;
		T += bone[b3].MV3x3 * tangent * boneWeight.w;
	}
}