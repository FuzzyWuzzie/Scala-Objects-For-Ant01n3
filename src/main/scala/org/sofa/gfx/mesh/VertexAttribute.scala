package org.sofa.gfx.mesh


/** Predefined vertex attribute names. More are possible. */
object VertexAttribute extends Enumeration {
	type VertexAttribute = Value

	val Vertex    = Value("Vertex")
	val Normal    = Value("Normal")
	val Tangent   = Value("Tangent")
	val Bitangent = Value("Bitangent")
	val Color     = Value("Color")
	val TexCoord  = Value("TexCoord")
	val Bone      = Value("Bone")
	val Weight    = Value("Weight")

	/** Convert a pair (VertexAttribute,String) to a pair (String,String) as often used with Mesh.newVertexArray(). */
	implicit def VaStPair2StStPair(p:(VertexAttribute,String)):(String,String) = (p._1.toString, p._2)

	/** Convert a VertexAttribute to a String. */
	implicit def Va2St(v:VertexAttribute):String = v.toString
}