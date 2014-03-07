package org.sofa.math

import org.scalatest.{FlatSpec, Matchers}

import org.sofa.math._
import scala.math._


class TestMath extends FlatSpec with Matchers {
    def testConstructor2(v:NumberSeq2) {
	    assertResult(1) { v.x }
	    assertResult(2) { v.y }
	    assertResult(2) { v.size }
	    assertResult((1, 2)) { v.xy }
	}
    
    def testConstructor3(v:NumberSeq3) {
	    assertResult(1) { v.x }
	    assertResult(2) { v.y }
	    assertResult(3) { v.z }
	    assertResult(3) { v.size }
	    assertResult((1, 2)) { v.xy }
	    assertResult((2, 3)) { v.yz }
	    assertResult((1, 2, 3)) { v.xyz }
	}
	    
    def testConstructor4(v:NumberSeq4) {
	    assertResult(1) { v.x }
	    assertResult(2) { v.y }
	    assertResult(3) { v.z }
	    assertResult(4) { v.w }
	    assertResult(4) { v.size }
	    assertResult((1, 2)) { v.xy }
	    assertResult((2, 3)) { v.yz }
	    assertResult((3, 4)) { v.zw }
	    assertResult((1, 2, 3)) { v.xyz }
	    assertResult((2, 3, 4)) { v.yzw }
	    assertResult((1, 2, 3, 4)) { v.xyzw }
	}
    
	"vectors and points" should "build correctly" in {
        testConstructor2(new Vector2(1, 2))
        testConstructor2(new Point2(1, 2))

        testConstructor3(new Vector3(1, 2, 3))
        testConstructor3(new Point3(1, 2, 3))

        testConstructor4(new Vector4(1, 2, 3, 4))
    }
    
    "vectors" should "allow sizling" in {
	    val a = Vector2(1, 2)
	    val b = Vector2(3, 4)
	    val v4 = Vector4(a.xy, b.xy)
	    
	    assertResult(1) { a.x }
	    assertResult(2) { a.y }
	    assertResult(3) { b.x }
	    assertResult(4) { b.y }
	    
	    assertResult(1) { v4.x }
	    assertResult(2) { v4.y }
	    assertResult(3) { v4.z }
	    assertResult(4) { v4.w }
	    
	    a.xy = v4.zw
	    b.xy = v4.xy
	    
	    assertResult(1) { b.x }
	    assertResult(2) { b.y }
	    assertResult(3) { a.x }
	    assertResult(4) { a.y }
	}
    
    it should "allow sizling 2" in {
        val a = Vector2(1, 2)
        val b = Vector2(3, 4)
        val c = Vector4(a)
        
	    assertResult(1) { c.x }
	    assertResult(2) { c.y }
	    assertResult(0) { c.z }
	    assertResult(0) { c.w }
        
	    c.xy = a
	    c.zw = b

	    assertResult(1) { c.x }
	    assertResult(2) { c.y }
	    assertResult(3) { c.z }
	    assertResult(4) { c.w }
    }

	it should "perform multiplication" in {    
        // Some operations.
        
        val a = Vector2(1, 2)
        val b = Vector2(3, 4)
        val c:Vector2 = a + b
        val d:Vector2 = a * b
        
        assertResult(4) { c.x }
        assertResult(6) { c.y }
        assertResult(3) { d.x }
        assertResult(8) { d.y }
    }

	it should "perform cross product and dot product" in {   
        val a = Vector3(1, 2, 3)
        val b = Vector3(4, 5, 6)
        val c = a X b

        assertResult((-3, 6, -3)) { c.xyz }
        
        assertResult(32) { a.dot(b) }
    }
    
    "matrices" should "perform multiplication" in {
        // 3 x 3 matrices
        
        var a = Matrix3()
        var b = Matrix3()
        
        a.setIdentity
        b.setIdentity
        
        var c = a * b

        assertResult(1) {c(0,0)};  assertResult(0) {c(0,1)};  assertResult(0) {c(0,2)}
        assertResult(0) {c(1,0)};  assertResult(1) {c(1,1)};  assertResult(0) {c(1,2)}
        assertResult(0) {c(2,0)};  assertResult(0) {c(2,1)};  assertResult(1) {c(2,2)}

        a = Matrix3((1, 2, 3),
                    (4, 5, 6),
                    (7, 8, 9))

        b.row0 = (9, 8, 7)
        b.row1 = (6, 5, 4)
        b.row2 = (3, 2, 1)
        
        c = a * b
        
        assertResult(( 30,  24, 18)) {c.row(0)}
        assertResult(( 84,  69, 54)) {c.row(1)}
        assertResult((138, 114, 90)) {c.row(2)}
        
        // 4 x 4 matrices
        
        var g = Matrix4(( 1,  2,  3,  4),
                        ( 5,  6,  7,  8),
                        ( 9, 10, 11, 12),
                        (13, 14, 15, 16)) 
        var h = Matrix4((16, 15, 14, 13),
                		(12, 11, 10,  9),
                		( 8,  7,  6,  5),
                		( 4,  3,  2,  1))
        var i = g * h
       
        assertResult(( 80,  70,  60,  50)) {i.row0};
        assertResult((240, 214, 188, 162)) {i.row1};
        assertResult((400, 358, 316, 274)) {i.row2};
        assertResult((560, 502, 444, 386)) {i.row3};
        
        
        // Any size matrices
        
        var d = Matrix(List(1.0, 2.0, 3.0), List(4.0, 5.0, 6.0))
        var e = Matrix(List(1.0, 2.0), List(3.0, 4.0), List(5.0, 6.0))

        
        assertResult(3) {d.width}
        assertResult(2) {d.height}
        assertResult(2) {e.width}
        assertResult(3) {e.height}

        var f = d * e

        assertResult(2) {f.width}
        assertResult(2) {f.height}
    }
    
    it should "perform transforms" in {
        var a = Matrix4()
        
        a.setIdentity
        
        assertResult(1) {a(0,0)}; assertResult(0) {a(0,1)}; assertResult(0) {a(0,2)}; assertResult(0) {a(0,3)}
        assertResult(0) {a(1,0)}; assertResult(1) {a(1,1)}; assertResult(0) {a(1,2)}; assertResult(0) {a(1,3)}
        assertResult(0) {a(2,0)}; assertResult(0) {a(2,1)}; assertResult(1) {a(2,2)}; assertResult(0) {a(2,3)}
        assertResult(0) {a(3,0)}; assertResult(0) {a(3,1)}; assertResult(0) {a(3,2)}; assertResult(1) {a(3,3)}
        
        a.rotate(Pi/2, 0, 1, 0)
        
        a(0,0) should be ( 0.0 +- 0.001); a(0,1) should be (0.0 +- 0.001);  a(0,2) should be (1.0 +- 0.001);  a(0,3) should be (0.0 +- 0.001)
        a(1,0) should be ( 0.0 +- 0.001); a(1,1) should be (1.0 +- 0.001);  a(1,2) should be (0.0 +- 0.001);  a(1,3) should be (0.0 +- 0.001)
        a(2,0) should be (-1.0 +- 0.001); a(2,1) should be (0.0 +- 0.001);  a(2,2) should be (0.0 +- 0.001);  a(2,3) should be (0.0 +- 0.001)
        a(3,0) should be ( 0.0 +- 0.001); a(3,1) should be (0.0 +- 0.001);  a(3,2) should be (0.0 +- 0.001);  a(3,3) should be (1.0 +- 0.001)

        a.translate(1, 2, 3)

        a(0,0) should be ( 0.0 +- 0.001); a(0,1) should be (0.0 +- 0.001); a(0,2) should be (1.0 +- 0.001); a(0,3) should be ( 3.0 +- 0.01) 
        a(1,0) should be ( 0.0 +- 0.001); a(1,1) should be (1.0 +- 0.001); a(1,2) should be (0.0 +- 0.001); a(1,3) should be ( 2.0 +- 0.01) 
        a(2,0) should be (-1.0 +- 0.001); a(2,1) should be (0.0 +- 0.001); a(2,2) should be (0.0 +- 0.001); a(2,3) should be (-1.0 +- 0.01) 
        a(3,0) should be ( 0.0 +- 0.001); a(3,1) should be (0.0 +- 0.001); a(3,2) should be (0.0 +- 0.001); a(3,3) should be ( 1.0 +- 0.01) 
        
        a.scale(1, 2, 3)
        
        a(0,0) should be ( 0.0 +- 0.001); a(0,1) should be (0.0 +- 0.001); a(0,2) should be (3.0 +- 0.001); a(0,3) should be ( 3.0 +- 0.001) 
        a(1,0) should be ( 0.0 +- 0.001); a(1,1) should be (2.0 +- 0.001); a(1,2) should be (0.0 +- 0.001); a(1,3) should be ( 2.0 +- 0.001) 
        a(2,0) should be (-1.0 +- 0.001); a(2,1) should be (0.0 +- 0.001); a(2,2) should be (0.0 +- 0.001); a(2,3) should be (-1.0 +- 0.001) 
        a(3,0) should be ( 0.0 +- 0.001); a(3,1) should be (0.0 +- 0.001); a(3,2) should be (0.0 +- 0.001); a(3,3) should be ( 1.0 +- 0.001) 
    }
    
    it should "perform lookAt" in {
        var a = Matrix4()
        a.setIdentity
        a.lookAt(1, 1, 1,    0, 0, 0,    0, 1, 0,  true)
        
        a(0,0) should be ( 0.707 +- 0.001); a(0,1) should be  (0.000 +- 0.001); a(0,2) should be (-0.707 +- 0.001); a(0,3) should be ( 0.000 +- 0.001) 
        a(1,0) should be (-0.408 +- 0.001); a(1,1) should be  (0.816 +- 0.001); a(1,2) should be (-0.408 +- 0.001); a(1,3) should be ( 0.000 +- 0.001) 
        a(2,0) should be ( 0.577 +- 0.001); a(2,1) should be  (0.577 +- 0.001); a(2,2) should be ( 0.577 +- 0.001); a(2,3) should be (-1.732 +- 0.001) 
        a(3,0) should be ( 0.000 +- 0.001); a(3,1) should be  (0.000 +- 0.001); a(3,2) should be ( 0.000 +- 0.001); a(3,3) should be ( 1.000 +- 0.001) 
    }

	it should "allow multiplication by vectors" in {    
        val M1 = Matrix(List( 1.0,  2.0,  3.0,  4.0,  5.0),
                        List( 6.0,  7.0,  8.0,  9.0, 10.0),
                        List(11.0, 12.0, 13.0, 14.0, 15.0),
                        List(16.0, 17.0, 18.0, 19.0, 20.0),
                        List(21.0, 22.0, 23.0, 24.0, 25.0))
        val V1 = Vector(1.0, 2.0, 3.0, 4.0, 5.0)
        val R1 = M1 * V1
        
        R1(0) should be ( 55) 
        R1(1) should be (130) 
        R1(2) should be (205) 
        R1(3) should be (280) 
        R1(4) should be (355) 
        
        val M2 = Matrix4((1, 2, 3, 4), (5, 6, 7, 8), (9, 10, 11, 12), (13, 14, 15, 16))
        val V2 = Vector4(1, 2, 3, 4)
        val R2 = M2 * V2
        
        assertResult((30, 70, 110, 150)) { R2.asInstanceOf[Vector4].xyzw }
    }
    
    it should "allow taking a sub matrix" in {
        val M1 = Matrix4((1, 2, 3, 4), (5, 6, 7, 8), (9, 10, 11, 12), (13, 14, 15, 16))
        val M2 = Matrix3()
        
        M1.subMatrix(M2, 2, 2)
        
        assertResult(( 1,  2,  4)) { M2.row0 }
        assertResult(( 5,  6,  8)) { M2.row1 }
        assertResult((13, 14, 16)) { M2.row2 }
        
        M1.subMatrix(M2, 3, 3)
        
        assertResult((1,  2,  3)) { M2.row0 }
        assertResult((5,  6,  7)) { M2.row1 }
        assertResult((9, 10, 11)) { M2.row2 }
        
        M1.subMatrix(M2, 0, 0)
        
        assertResult(( 6,  7,  8)) { M2.row0 }
        assertResult((10, 11, 12)) { M2.row1 }
        assertResult((14, 15, 16)) { M2.row2 }
    }

	it should "perform determinant and inverse" in {        
        // A matrix multiplied by its inverse gives the identity. Uses this
        // to test the determinant and inverse methods.
        
        val M1 = Matrix3((1, 3, 2), (1, 1, 1), (0, 2, 3))
        val D1 = M1.det
        val I1 = M1 * M1.inverse
        
        assertResult(-4) { D1 }
        assertResult((1, 0, 0)) { I1.row0 }
        assertResult((0, 1, 0)) { I1.row1 }
        assertResult((0, 0, 1)) { I1.row2 }
        
        val M2 = Matrix4((1, 3, 2, 1), (1, 1, 1, 1), (0, 1, 1, 0), (0, 0, 0, 1))
        val D2 = M2.det
        val I2 = M2 * M2.inverse
        
        // println(M2)
        // println(D2)
        // println(M2.inverse)
        // println(I2)
        
        assertResult(-1) { D2 }
        assertResult((1, 0, 0, 0)) { I2.row0 }
        assertResult((0, 1, 0, 0)) { I2.row1 }
        assertResult((0, 0, 1, 0)) { I2.row2 }
        assertResult((0, 0, 0, 1)) { I2.row3 }
    }
}
