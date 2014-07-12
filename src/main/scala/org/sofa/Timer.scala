package org.sofa

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.compat.Platform
import java.io.PrintStream
import java.lang.System


/** Timer companion object. */
object Timer {
	def apply():Timer = new Timer()
	def apply(out:PrintStream):Timer = Timer(out)

	/** Global shared timer. */
	val timer = new Timer()
}


/** Timer allowing to measure the duration of a code block. */
class Timer(val out:PrintStream = Console.out) {
	/** The set of measures, identified by names. */
	protected[this] val measures = new HashMap[String,Measures]()

	/** Measure a code block execution time. The name gives the code block name, allowing to take
	  * several measures of the same block and compute average times. */
	def measure(name:String)(code: => Unit) {
		val measure = measures.get(name).getOrElse({val m = new Measures(name); measures += ((name,m)); m})
		measure.T1 = System.nanoTime
		code
		measure.addMeasure(System.nanoTime - measure.T1)
		//measure.printLast(out)
	}

	def measureStart(name:String) {
		val measure = measures.get(name).getOrElse({val m = new Measures(name); measures += ((name,m)); m})
		measure.T1 = System.nanoTime
	}

	def measureEnd(name:String) {
		val T2 = System.nanoTime
		val measure = measures.get(name).getOrElse(throw new RuntimeException("reset() called between measures ?"))
		measure.addMeasure(T2 - measure.T1)
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

	/** Reset all measures. */
	def reset() { measures.clear }
}


/** A set of measures for the same part of code.
  *
  * The name arguement allows to identify to measured thing. */
class Measures(val name:String) {
	/** Used during the measure to store the start time.
	  * A start time is stored in each measure, although not
	  * useful for the measure itself, it allows the timer to
	  * nest measures. */
	var T1 = 0L

	/** The number of measures. */
	protected[this] var count = 0

	/** The sum of all measures. */
	protected[this] var sum = 0L

	/** The last measure. */
	protected[this] var last = 0L

	/** The maximum measure. */
	protected[this] var max = 0L

	/** The minimum measure. */
	protected[this] var min = Long.MaxValue

	/** Add a measure. */
	def addMeasure(value:Long) {
		count += 1
		sum   += value
		last   = value

		if(value > max) max = value
		if(value < min) min = value
	}

	/** The average of all measures until now. */
	def average():Double = { sum.toDouble / count.toDouble }

	/** Print the average measure to the given output stream. */
	def printAvg(out:PrintStream):Double = {
		val avg = average
		out.println("    %32s: ~%8.2f msecs | %d samples || last %8.2f | max %8.2f | min %8.2f | sum %8.2f |".format(name, avg/100000.0, count, last/100000.0, max/100000.0, min/100000.0, sum/100000.0))
		avg
	}
}