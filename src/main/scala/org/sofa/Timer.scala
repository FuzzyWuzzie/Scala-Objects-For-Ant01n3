package org.sofa

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.compat.Platform
import java.io.PrintStream
import java.lang.System

/** Timer allowing to measure the duration of a code block. */
class Timer(val out:PrintStream) {
	/** The set of measures, identified by names. */
	val measures = new HashMap[String,Measures]()

	/** Measure a code block execution time. The name gives the code block name, allowing to take
	  * several measures of the same block and compute average times. */
	def measure(name:String)(code: => Unit) {
		val measure = measures.get(name).getOrElse({val m = new Measures(name); measures += ((name,m)); m})
		val T1 = System.nanoTime
		code
		measure.addMeasure(System.nanoTime - T1)
		//measure.printLast(out)
	}

	/** Print all the average times of all measures. The header is a string to print before all the measures. */
	def printAvgs(header:String) {
		out.println(header)
		var sum = 0.0
		measures.foreach { measure =>
			sum += measure._2.printAvg(out)
		}
		out.println("  total: %.2f msecs".format(sum/100000.0))
	}

	def reset() {
		measures.clear
		
	}
}

/** A set of measures for the same part of code.
  *
  * The name arguement allows to identify to measured thing. */
class Measures(val name:String) {
	/** The number of measures. */
	var count = 0

	/** The sum of all measures. */
	var sum = 0L

	/** The last measure. */
	var last = 0L

	/** Add a measure. */
	def addMeasure(value:Long) {
		count += 1
		sum   += value
		last   = value
	}

	/** The average of all measures until now. */
	def average():Double = { sum.toDouble / count.toDouble }

	/** Print the average measure to the given output stream. */
	def printAvg(out:PrintStream):Double = {
		val avg = average
		out.println("    %s: ~ %.2f msecs (%d measures) (last %.2f)".format(name, avg/100000.0, count, last/100000.0))
		avg
	}
}