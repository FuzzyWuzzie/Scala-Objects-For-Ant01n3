package  org.sofa.nio.test

import org.sofa.nio._
import org.sofa.Timer


object TestNioBufers {
	def main(args:Array[String]):Unit = { 
		val test = new TestNioBufers

		test.test1
		test.test2
		test.test3
	}
}


class TestNioBufers {

	val timer = Timer()

	final val Size = 100000

	final val NTests = 100

	def test1() {
		// set test

		val intbuf = IntBuffer(Size, true)
		val intarr = new Array[Int](Size)

		var j = 0
		var i = 0
		var t = 0

		while(t < NTests) {
			i = 0
			timer.measure("array  set") {
				while(i < Size) {
					intarr(i) = i
					i += 1
				}
			}
			t += 1
		}

		t = 0

		while(t < NTests) {
			i = 0
			timer.measure("buffer set") {
				while(i < Size) {
					intbuf(i) = i
					i += 1
				}
			}
			t += 1
		}

		// get test

		t = 0

		while(t < NTests) {
			i = 0
			timer.measure("array  get") {
				while(i < Size) {
					j = intarr(i)
					i += 1
				}
			}
			t += 1
		}

		t = 0

		while(t < NTests) {
			i = 0
			timer.measure("buffer get") {
				while(i < Size) {
					j = intbuf(i)
					i += 1
				}
			}
			t += 1
		}

		timer.printAvgs("test1")
	}	

	def test2() {
	}

	def test3() {
	}
}