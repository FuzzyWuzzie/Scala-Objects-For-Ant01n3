package org.sofa.gfx.mesh


/** Predefined vertex attribute names.
  *
  * These are only indicative. Use them when defining vertex arrays and
  * associating attributes of the arrays with data in the shaders. See
  * the [[Mesh]] class. Most mesh class will create their attributes using
  * these names, therefore you are able to do things like:
  *
  *    import VertexAttribute._
  *    mesh.newVertexArray(gl, gl.STATIC_DRAW, myShader, Vertex -> "V", Normal -> "N")
  * 
  */
object VertexAttribute extends Enumeration {

	type VertexAttribute = Value

	/** Vertex position, often three components. The vertex attribute stores the mesh
	  * main geometry, positions of vertices. */
	val Position = Value("Position")
	
	/** Vertex normal vector, often three components. This vertex attribute describes
	  * a vector, most often of unit length (hence its name), that describe the
	  * vector perpendicular to the surface described by the mesh. This is for example
	  * used in lighting calculations. */
	val Normal = Value("Normal")
	
	/** Vertex tangent vector, complete the normal, often three components. This
	  * vertex attribute describtes a vector or unit length, parallel to the surface
	  * describted by the mesh, and perpendicular to the normal. */
	val Tangent = Value("Tangent")
	
	/** Vertex bi-tangent vector, complete the normal and tangent and form a base,
	  * often three components. This vectors goes with the normal and tangent and
	  * is perpendicular to both. It forms a base and a transformation. */
	val Bitangent = Value("Bitangent")
	
	/** Vertex color, often three or four components (fourth is transparency).
	  * The color ofent forms a vector in the RGB (red, green, blue) color
	  * space or RGBA (adding alpha aka transparency) color space. */
	val Color = Value("Color")
	
	/** Vertex texture coordinates, often two components (u and v) for 2D images.
	  * Coordinates of the associated vertex in the texture space. Most often
	  * textures are two dimensional and therefore these coordinates have
	  * two components. */
	val TexCoord = Value("TexCoord")
	
	/** Vertex bones indices, often one component but up to four. This is often seen as an
	  * unique integer, but several bones can influence a vertex and therefore,
	  * there can be several components. */
	val Bone = Value("Bone")
	
	/** Vertex bone weights. Associated to the vertex bones, this
	  * gives the influence of each bone on the vertex, the sum of
	  * these components should most of the time add to one.  */
	val Weight = Value("Weight")
	
	/** Vertex offset for instanced rendering, often three components. When doing
	  * instanced rendering, this attribute gives independent positions for each
	  * instance of the repeatedly drawn mesh and is added to the position. */
	val Offset = Value("Offset")

	/** Barycentric coordinates for a triangle, usualy (1,0,0), (0,1,0) or (0,0,1).
	  * Mostly used to draw wireframes. */
	val BaryCoord = Value("BaryCoord")

	/** Convert a pair (VertexAttribute,String) to a pair (String,String) as often used with Mesh.newVertexArray(). */
	implicit def VaStPair2StStPair(p:(VertexAttribute,String)):(String,String) = (p._1.toString, p._2)

	/** Convert a VertexAttribute to a String. */
	implicit def Va2St(v:VertexAttribute):String = v.toString
}