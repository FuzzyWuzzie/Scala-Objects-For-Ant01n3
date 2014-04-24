package org.sofa.math

import scala.collection.mutable.HashMap

/** Thrown when there is no transition from a state by a given object. */
case class NoSuchTransitionException(msg:String) extends Exception(msg)

/** An automaton state. */
trait State {
	/** Given a transition object, reach a given state. Throw a
	  * NoSuchTransitionException if the transition is undefined. */
	def transition(by:AnyRef):State	
}

/** A basic state class that you can edit. */
class EditableState(initialTransitions:Pair[AnyRef,State]*) extends State {
	/** Set of defined transitions. */
	protected val transitions = new HashMap[AnyRef,State]()

	init

	protected def init() { initialTransitions.foreach { item => setupTransition(item._1, item._2) } }

	/** Setup a new transition from this state to another state by a given object. */
	def setupTransition(by:AnyRef, to:State) { transitions += (by -> to) }

	def transition(by:AnyRef):State = { transitions.get(by).getOrElse(throw NoSuchTransitionException(by.toString)) }
}

// trait EditableStateAction(val action:()=>Unit, initialTransitions:Pair[AnyRef,State]*) extends EditableState(initialTransitions) {
// 	def transition(by:AnyRef):State = {
// 		transitions.get(by).getOrElse(throw NoSuchTransitionException(by.toString)) match {
// 			case act:EditableStateAction => { act.action; act }
// 			case non:State => { non }
// 		}
// 	}
// }

/** An automaton is a current state. You can change its state using the
  * transition(String)` method. */
class Automaton(var state:State) {
	/** Transition this automaton from the current state to another (potentially the same) by a given object
	  * as transition. Throw a NoSuchTransitionException if the transition is undefined. */
	def transition(by:AnyRef) { state = state.transition(by) }
}