package org.sofa.gfx.renderer

import org.scalatest.FlatSpec


class TestAvatarName extends FlatSpec {
	"An AvatarName" should "build from an array of strings" in {
		val n = AvatarName(Array[String]("a", "b", "c"))

		assertResult (3, "length of name") { n.length }
		assertResult (3, "size of name") { n.size }
		assertResult (3, "depth") { n.depth }
		assertResult ("a", "element 0") { n(0) }
		assertResult ("b", "element 1") { n(1) }
		assertResult ("c", "element 2") { n(2) }
	}

	it should "convert to and from a string in a predictable way" in {
		val n = AvatarName("a.b.c")
		
		assertResult (3, "length of name") { n.length }
		assertResult ("a", "element 0") { n(0) }
		assertResult ("b", "element 1") { n(1) }
		assertResult ("c", "element 2") { n(2) }
		assertResult ("a.b.c", "convert to string") { n.toString }
	}

	it should "build from a var args of strings" in {
		val n = AvatarName("a", "b", "c")

		assertResult (3, "length of name") { n.length }
		assertResult ("a", "element 0") { n(0) }
		assertResult ("b", "element 1") { n(1) }
		assertResult ("c", "element 2") { n(2) }
	}

	it should "allow equality comparison" in {
		val n1 = AvatarName("a.b.c")
		val n2 = AvatarName("a.b.c")
		val n3 = AvatarName("a.b.c.d")

		assertResult(true, "equality") { n1 == n2 }
		assertResult(true, "inequality 1") { n2 != n3 }
		assertResult(true, "inequality 2") { n1 != n3 }
		assertResult(true, "equality of hash codes for same pathes") { n1.hashCode == n2.hashCode }
		assertResult(true, "inequality of hash codes for distinct pathes") { n1.hashCode != n3.hashCode }
	}

	it should "allow to retrieve prefix and suffix" in {
		val n = AvatarName("a.b.c")

		assertResult ("c", "suffix") { n.suffix }
		assertResult (AvatarName("a.b"), "prefix") { n.prefix }
	}

	it should "allow to compare prefixes" in {
		val n1 = AvatarName("a.b.c")
		val n2 = AvatarName("a.b.c.d")

		assertResult(true, "prefix a") { n1.equalPrefix(1, n2) }
		assertResult(true, "prefix a.b") { n1.equalPrefix(2, n2) }
		assertResult(true, "prefix a.b.c") { n1.equalPrefix(3, n2) }
		assertResult(false, "no prefix") { n2.equalPrefix(4, n1) }
	}
}