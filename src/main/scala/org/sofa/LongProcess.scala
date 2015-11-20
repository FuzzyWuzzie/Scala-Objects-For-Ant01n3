package org.sofa

trait LongProcess {
	def beginOperation(name:String)

	def advance(percent:Double)

	def endOperation(name:String)
}