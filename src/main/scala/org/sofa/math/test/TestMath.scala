package org.sofa.math.test

import org.sofa.math._
import org.junit._
import org.junit.Assert._
import scala.math._

class TestMath {
    @Test
	def testConstructors() {
        testConstructor2(new ArrayVector2(1, 2))
        testConstructor2(new NioBufferVector2(1, 2))
        testConstructor2(new ArrayPoint2(1, 2))
        testConstructor2(new NioBufferPoint2(1, 2))
        
        testConstructor3(new ArrayVector3(1, 2, 3))
        testConstructor3(new NioBufferVector3(1, 2, 3))
        testConstructor3(new ArrayPoint3(1, 2, 3))
        testConstructor3(new NioBufferPoint3(1, 2, 3))

        testConstructor4(new ArrayVector4(1, 2, 3, 4))
        testConstructor4(new NioBufferVector4(1, 2, 3, 4))
    }
    
    def testConstructor2(v:NumberSeq2) {
	    assertEquals(1, v.x, 0)
	    assertEquals(2, v.y, 0)
	    assertEquals(2, v.size, 0)
	    assertEquals((1, 2), v.xy)
	}
	    
    def testConstructor3(v:NumberSeq3) {
	    assertEquals(1, v.x, 0)
	    assertEquals(2, v.y, 0)
	    assertEquals(3, v.z, 0)
	    assertEquals(3, v.size, 0)
	    assertEquals((1, 2), v.xy)
	    assertEquals((2, 3), v.yz)
	    assertEquals((1, 2, 3), v.xyz)
	}
	    
    def testConstructor4(v:NumberSeq4) {
	    assertEquals(1, v.x, 0)
	    assertEquals(2, v.y, 0)
	    assertEquals(3, v.z, 0)
	    assertEquals(4, v.w, 0)
	    assertEquals(4, v.size, 0)
	    assertEquals((1, 2), v.xy)
	    assertEquals((2, 3), v.yz)
	    assertEquals((3, 4), v.zw)
	    assertEquals((1, 2, 3), v.xyz)
	    assertEquals((2, 3, 4), v.yzw)
	    assertEquals((1, 2, 3, 4), v.xyzw)
	}
    
    @Test
	def testVectorSizling() {
	    val a = ArrayVector2(1, 2)
	    val b = NioBufferVector2(3, 4)
	    val v4 = ArrayVector4(a.xy, b.xy)
	    
	    assertEquals(1, a.x, 0)
	    assertEquals(2, a.y, 0)
	    assertEquals(3, b.x, 0)
	    assertEquals(4, b.y, 0)
	    
	    assertEquals(1, v4.x, 0)
	    assertEquals(2, v4.y, 0)
	    assertEquals(3, v4.z, 0)
	    assertEquals(4, v4.w, 0)
	    
	    a.xy = v4.zw
	    b.xy = v4.xy
	    
	    assertEquals(1, b.x, 0)
	    assertEquals(2, b.y, 0)
	    assertEquals(3, a.x, 0)
	    assertEquals(4, a.y, 0)
	}
    
    @Test
    def testVectorSizling2() {
        val a = Vector2(1, 2)
        val b = Vector2(3, 4)
        val c = Vector4(a)
        
	    assertEquals(1, c.x, 0)
	    assertEquals(2, c.y, 0)
	    assertEquals(0, c.z, 0)
	    assertEquals(0, c.w, 0)
        
	    c.xy = a
	    c.zw = b

	    assertEquals(1, c.x, 0)
	    assertEquals(2, c.y, 0)
	    assertEquals(3, c.z, 0)
	    assertEquals(4, c.w, 0)
    }
    
    @Test
    def testVectorMult() {
        // Some operations.
        
        val a = Vector2(1, 2)
        val b = Vector2(3, 4)
        val c:Vector2 = a + b
        val d:Vector2 = a * b
        
        assertEquals(4, c.x, 0)
        assertEquals(6, c.y, 0)
        assertEquals(3, d.x, 0)
        assertEquals(8, d.y, 0)
    }
    
    @Test
    def testCrossAndDot() {
        val a = Vector3(1, 2, 3)
        val b = Vector3(4, 5, 6)
        val c = a X b

        assertEquals((-3, 6, -3), c.xyz)
        
        assertEquals(32, a.dot(b), 0)
    }
    
    @Test
    def testMatrixMult() {
        // 3 x 3 matrices
        
        var a = Matrix3()
        var b = Matrix3()
        
        a.setIdentity
        b.setIdentity
        
        var c = a * b

        assertEquals(1, c(0,0), 0);  assertEquals(0, c(0,1), 0);  assertEquals(0, c(0,2), 0)
        assertEquals(0, c(1,0), 0);  assertEquals(1, c(1,1), 0);  assertEquals(0, c(1,2), 0)
        assertEquals(0, c(2,0), 0);  assertEquals(0, c(2,1), 0);  assertEquals(1, c(2,2), 0)

        a = ArrayMatrix3((1, 2, 3),
                         (4, 5, 6),
                         (7, 8, 9))

        b.row0 = (9, 8, 7)
        b.row1 = (6, 5, 4)
        b.row2 = (3, 2, 1)
        
        c = a * b
        
        assertEquals(( 30,  24, 18), c.row(0))
        assertEquals(( 84,  69, 54), c.row(1))
        assertEquals((138, 114, 90), c.row(2))
        
        // 4 x 4 matrices
        
        var g = ArrayMatrix4(( 1,  2,  3,  4),
                             ( 5,  6,  7,  8),
                             ( 9, 10, 11, 12),
                             (13, 14, 15, 16)) 
        var h = ArrayMatrix4((16, 15, 14, 13),
                			 (12, 11, 10,  9),
                			 ( 8,  7,  6,  5),
                			 ( 4,  3,  2,  1))
        var i = g * h
       
        assertEquals(( 80,  70,  60,  50), i.row0);
        assertEquals((240, 214, 188, 162), i.row1);
        assertEquals((400, 358, 316, 274), i.row2);
        assertEquals((560, 502, 444, 386), i.row3);
        
        
        // Any size matrices
        
        var d = ArrayMatrix(List(1., 2., 3.), List(4., 5., 6.))
        var e = ArrayMatrix(List(1., 2.), List(3., 4.), List(5., 6.))

        
        assertEquals(3, d.width, 0)
        assertEquals(2, d.height, 0)
        assertEquals(2, e.width, 0)
        assertEquals(3, e.height, 0)

        var f = d * e

        assertEquals(2, f.width, 0)
        assertEquals(2, f.height, 0)
    }
    
    @Test
    def testMatrixTransforms() {
        var a = Matrix4()
        
        a.setIdentity
        
        assertEquals(1, a(0,0), 0.001); assertEquals(0, a(0,1), 0.001); assertEquals(0, a(0,2), 0.001); assertEquals(0, a(0,3), 0.001)
        assertEquals(0, a(1,0), 0.001); assertEquals(1, a(1,1), 0.001); assertEquals(0, a(1,2), 0.001); assertEquals(0, a(1,3), 0.001)
        assertEquals(0, a(2,0), 0.001); assertEquals(0, a(2,1), 0.001); assertEquals(1, a(2,2), 0.001); assertEquals(0, a(2,3), 0.001)
        assertEquals(0, a(3,0), 0.001); assertEquals(0, a(3,1), 0.001); assertEquals(0, a(3,2), 0.001); assertEquals(1, a(3,3), 0.001)
        
        a.rotate(Pi/2, 0, 1, 0)
        
        assertEquals( 0, a(0,0), 0.001); assertEquals(0, a(0,1), 0.001); assertEquals(1, a(0,2), 0.001); assertEquals(0, a(0,3), 0.001)
        assertEquals( 0, a(1,0), 0.001); assertEquals(1, a(1,1), 0.001); assertEquals(0, a(1,2), 0.001); assertEquals(0, a(1,3), 0.001)
        assertEquals(-1, a(2,0), 0.001); assertEquals(0, a(2,1), 0.001); assertEquals(0, a(2,2), 0.001); assertEquals(0, a(2,3), 0.001)
        assertEquals( 0, a(3,0), 0.001); assertEquals(0, a(3,1), 0.001); assertEquals(0, a(3,2), 0.001); assertEquals(1, a(3,3), 0.001)
        
        a.translate(1, 2, 3)

        assertEquals( 0, a(0,0), 0.001); assertEquals(0, a(0,1), 0.001); assertEquals(1, a(0,2), 0.001); assertEquals( 3, a(0,3), 0.001)
        assertEquals( 0, a(1,0), 0.001); assertEquals(1, a(1,1), 0.001); assertEquals(0, a(1,2), 0.001); assertEquals( 2, a(1,3), 0.001)
        assertEquals(-1, a(2,0), 0.001); assertEquals(0, a(2,1), 0.001); assertEquals(0, a(2,2), 0.001); assertEquals(-1, a(2,3), 0.001)
        assertEquals( 0, a(3,0), 0.001); assertEquals(0, a(3,1), 0.001); assertEquals(0, a(3,2), 0.001); assertEquals( 1, a(3,3), 0.001)
        
        a.scale(1, 2, 3)
        
        assertEquals( 0, a(0,0), 0.001); assertEquals(0, a(0,1), 0.001); assertEquals(3, a(0,2), 0.001); assertEquals( 3, a(0,3), 0.001)
        assertEquals( 0, a(1,0), 0.001); assertEquals(2, a(1,1), 0.001); assertEquals(0, a(1,2), 0.001); assertEquals( 2, a(1,3), 0.001)
        assertEquals(-1, a(2,0), 0.001); assertEquals(0, a(2,1), 0.001); assertEquals(0, a(2,2), 0.001); assertEquals(-1, a(2,3), 0.001)
        assertEquals( 0, a(3,0), 0.001); assertEquals(0, a(3,1), 0.001); assertEquals(0, a(3,2), 0.001); assertEquals( 1, a(3,3), 0.001)
    }
    
    @Test
    def testLookAt() {
        var a = Matrix4()
        a.setIdentity
        a.lookAt(1, 1, 1,    0, 0, 0,    0, 1, 0,  true)
        
        assertEquals( 0.707, a(0,0), 0.001); assertEquals(0.000, a(0,1), 0.001); assertEquals(-0.707, a(0,2), 0.001); assertEquals( 0.000, a(0,3), 0.001)
        assertEquals(-0.408, a(1,0), 0.001); assertEquals(0.816, a(1,1), 0.001); assertEquals(-0.408, a(1,2), 0.001); assertEquals( 0.000, a(1,3), 0.001)
        assertEquals( 0.577, a(2,0), 0.001); assertEquals(0.577, a(2,1), 0.001); assertEquals( 0.577, a(2,2), 0.001); assertEquals(-1.732, a(2,3), 0.001)
        assertEquals( 0.000, a(3,0), 0.001); assertEquals(0.000, a(3,1), 0.001); assertEquals( 0.000, a(3,2), 0.001); assertEquals( 1.000, a(3,3), 0.001)
    }
    
    @Test
    def testVectorMatrix() {
        val M1 = ArrayMatrix(List( 1.,  2.,  3.,  4.,  5.),
                             List( 6.,  7.,  8.,  9., 10.),
                             List(11., 12., 13., 14., 15.),
                             List(16., 17., 18., 19., 20.),
                             List(21., 22., 23., 24., 25.))
        val V1 = ArrayVector(1., 2., 3., 4., 5.)
        val R1 = M1 * V1
        
        assertEquals( 55, R1(0), 0)
        assertEquals(130, R1(1), 0)
        assertEquals(205, R1(2), 0)
        assertEquals(280, R1(3), 0)
        assertEquals(355, R1(4), 0)
        
        val M2 = ArrayMatrix4((1, 2, 3, 4), (5, 6, 7, 8), (9, 10, 11, 12), (13, 14, 15, 16))
        val V2 = ArrayVector4(1, 2, 3, 4)
        val R2 = M2 * V2
        
        assertEquals((30, 70, 110, 150), R2.asInstanceOf[ArrayVector4].xyzw)
    }
    
    @Test
    def testSubMatrix() {
        val M1 = Matrix4((1, 2, 3, 4), (5, 6, 7, 8), (9, 10, 11, 12), (13, 14, 15, 16))
        val M2 = Matrix3()
        
        M1.subMatrix(M2, 2, 2)
        
        assertEquals(( 1,  2,  4), M2.row0)
        assertEquals(( 5,  6,  8), M2.row1)
        assertEquals((13, 14, 16), M2.row2)
        
        M1.subMatrix(M2, 3, 3)
        
        assertEquals((1,  2,  3), M2.row0)
        assertEquals((5,  6,  7), M2.row1)
        assertEquals((9, 10, 11), M2.row2)
        
        M1.subMatrix(M2, 0, 0)
        
        assertEquals(( 6,  7,  8), M2.row0)
        assertEquals((10, 11, 12), M2.row1)
        assertEquals((14, 15, 16), M2.row2)
    }
        
    @Test
    def testDetInv() {
        // A matrix multiplied by its inverse gives the identity. Uses this
        // to test the determinant and inverse methods.
        
        val M1 = Matrix3((1, 3, 2), (1, 1, 1), (0, 2, 3))
        val D1 = M1.det
        val I1 = M1 * M1.inverse
        
        assertEquals(-4, D1, 0)
        assertEquals((1, 0, 0), I1.row0)
        assertEquals((0, 1, 0), I1.row1)
        assertEquals((0, 0, 1), I1.row2)
        
        val M2 = Matrix4((1, 3, 2, 1), (1, 1, 1, 1), (0, 1, 1, 0), (0, 0, 0, 1))
        val D2 = M2.det
        val I2 = M2 * M2.inverse
        
        println(M2)
        println(D2)
        println(M2.inverse)
        println(I2)
        
        assertEquals(-1, D2, 0)
        assertEquals((1, 0, 0, 0), I2.row0)
        assertEquals((0, 1, 0, 0), I2.row1)
        assertEquals((0, 0, 1, 0), I2.row2)
        assertEquals((0, 0, 0, 1), I2.row3)
    }
}
