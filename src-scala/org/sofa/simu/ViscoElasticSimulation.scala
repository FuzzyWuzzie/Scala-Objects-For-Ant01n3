package org.sofa.simu

import scala.collection.mutable.ArrayBuffer
import org.sofa.math.Vector3
import org.sofa.math.Point3
import org.sofa.math.SpatialHash
import org.sofa.math.SpatialPoint
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

// XXX Amélioration possible:
// Le test de collision des murs est fait pour toutes les particules à tout moment, mais il n'est besoin de le faire que pour les
// cubes du spaceHash qui sont le long de ces murs !

object Particle {
	val g       = 9.81				// Assuming we use meters as units, g = 9.81 m/s^2
	val h       = 0.9				// 50 cm
	val spacialHashBucketSize = 1	// 1 m

	// Appear in doubleDensityRelaxation():
	val k       = 1.0				// Pressure stiffness ???
	val kNear   = k * 10			// Near-pressure stiffness ??? Seems to be 10 times k
	val rhoZero = h/2				// Rest density (4.1), the larger then denser, less than 1 == gaz ??? seems to be half of h

	// Appear in applyViscosity():
	val sigma   = 3.0				// Viscosity linear dependency on velocity (between 0 and +inf), augment for highly viscous (lava, sperm).
	val beta    = 5.0				// Viscosity quadratic dependency on velocity (between 0 and +inf), only this one for less viscous fluids (water).

	// Appear in adjustSprings() and applySpringDisplacements():
	val plasticity = false
	val L       = h					// Spring rest length ??? Should be smaller than h.
	val kspring = 0.3				// Spring stiffness (5.1) ???
	val gamma   = 0.1				// Spring yield ratio, typically between 0 and 0.2
	val alpha   = 0.1				// Plasticity constant ???
		
	// Appear in resolveCollions():
	val umicron = 0.1				// Friction parameter between 0 and 1 (0 = slip, 1 = no slip). ???
	val wallsX  = 5.0
	val wallsZ  = 0.4
	val ground  = 0.1
	
	// Stickiness:
	val stickiness = false
	val kStick  = 1.0
	val dStick  = h/3				// Smaller than h
}
/*
object Particle {
	val g       = 9.81				// Assuming we use meters as units, g = 9.81 m/s^2
	val h       = 0.5				// 50 cm
	val spacialHashBucketSize = 1	// 1 m

	// Appear in doubleDensityRelaxation():
	val k       = 1					// Pressure stiffness ???
	val kNear   = k * 10			// Near-pressure stiffness ??? Seems to be 10 times k
	val rhoZero = h/2				// Rest density (4.1), the larger then denser, less than 1 == gaz ??? seems to be half of h

	// Appear in applyViscosity():
	val sigma   = 0					// Viscosity linear dependency on velocity (between 0 and +inf), augment for highly viscous (lava, sperm).
	val beta    = 0.1				// Viscosity quadratic dependency on velocity (between 0 and +inf), only this one for less viscous fluids (water).

	// Appear in adjustSprings() and applySpringDisplacements():
	val plasticity = false
	val L       = h					// Spring rest length ??? Should be smaller than h.
	val kspring = 0.3				// Spring stiffness (5.1) ???
	val gamma   = 0.1				// Spring yield ratio, typically between 0 and 0.2
	val alpha   = 0.1				// Plasticity constant ???
		
	// Appear in resolveCollions():
	val umicron = 0.0				// Friction parameter between 0 and 1 (0 = slip, 1 = no slip). ???
	val wallsX  = 5.0
	val wallsZ  = 0.4
	val ground  = 0.1
	
	// Stickiness:
	val stickiness = false
	val kStick  = 1.0
	val dStick  = h/3				// Smaller than h
}
*/
//class Neigbor(val p:Particle, val r:Vector3, val distance:Double) {}

class Spring(val i:Particle, val j:Particle, var L:Double) {
	// At construction, the spring registers in its two particles.
	j.springs += ((i.index, this))
	i.springs += ((j.index, this))
	/** Called when the spring disappears, it un-registers from its two particles. */
	def removed() {
		i.springs.remove(j.index)
		j.springs.remove(i.index)
	}
}

class Particle(val index:Int) extends SpatialPoint {
	/** Actual position. */
	val x = Point3()
	
	/** Previous position. */
	val xprev = Point3()
	
	/** Velocity. */
	val v = Vector3()
	
	/** Set of visible neighbor (distance < h). */
	var neighbors:ArrayBuffer[Particle] = null

	/** Map of springs toward other particles. The particles
	  * are mapped to their index in the simulation. */
	var springs = new HashMap[Int,Spring]()
	
	/** Position. */
	def from:Point3 = x
	
	/** As this is a non volumic object, this is equal to position. */
	def to:Point3 = x
	
	def applyGravity(dt:Double) {
		v.y -= dt * Particle.g
	}
	
	def move(dt:Double) {
		xprev.copy(x)
		x.x += v.x * dt
		x.y += v.y * dt
		x.z += v.z * dt
	}
	
	def computeVelocity(dt:Double) {
		v.x = (x.x - xprev.x) / dt
		v.y = (x.y - xprev.y) / dt
		v.z = (x.z - xprev.z) / dt
	}
}

class ViscoElasticSimulation extends ArrayBuffer[Particle] {

	protected var particlesIndexMax = 0
	
	val spaceHash = new SpatialHash[Particle](Particle.spacialHashBucketSize) 
	
	val springs = new HashSet[Spring]()
	
	var running = true
	
	/** Evaluate the iso-surface at the given point `x`. */
	def evalIsoSurface(x:Point3):Double = {
		var value = 0.0
		val neighbors = spaceHash.neighborsInBox(x, Particle.h)
		
		neighbors.foreach { i =>
			val R = Vector3(x, i.x)
			val r = R.norm
			val q = r/Particle.h
			if(q < 1) {
				val v = (1 - q)
				value += (v*v)
			}
		}
		
		val v = math.sqrt(value)
//println("%s with %d neighbors = %f".format(x, neighbors.size, v))
		v
	}
	
	def evalIsoNormal(x:Point3):Vector3 = {
		val n = Vector3(0, 0, 0)
		val neighbors = spaceHash.neighborsInBox(x, Particle.h)
		
		neighbors.foreach { i =>
			val R = Vector3(i.x, x)
			val l = R.normalize
			val q = l/Particle.h
			if(q<1) {
				R /= l*l
				n += R
			}
		}
		
		n.normalize
		n
	}
	
	def addParticle(x:Double, y:Double, z:Double, vx:Double, vy:Double, vz:Double):Particle = {
		val p = new Particle(particlesIndexMax) 
		
		particlesIndexMax += 1
		
		p.x.set(x, y, z)
		p.v.set(vx, vy, vz)
		spaceHash.add(p)
		
		this += p
		
		p		
	}
	
	def addParticle(x:Double, y:Double, z:Double):Particle = addParticle(x,y,z, 0, 0, 0)
	
	def simulation() {
		var dt = 0.1	// In seconds
		while(running) {
			simulationStep(dt)
		}
	}
	
	def simulationStep(dt:Double) {
		// Compute neighbors
		var n = 0.0
		foreach { p => p.neighbors = computeNeighbors(p); n += p.neighbors.size }
//println("%f neighbors in average".format(n/size))
		// Gravity
		foreach { _.applyGravity(dt) }
		// Viscosity
		applyViscosity(dt)
		// Move
		foreach { _.move(dt) }
		// Add and remove springs, change rest lengths
		if(Particle.plasticity) {
			adjustSprings(dt)
			// Modify positions according to springs
			// double density relaxation, and collisions
			applySpringDisplacements(dt)
		}
		doubleDensityRelaxation(dt)
		resolveCollions(dt)
		foreach { _.computeVelocity(dt) }
		// Update the space hash
		foreach { spaceHash.move(_) }
	}
	
	def computeNeighbors(i:Particle):ArrayBuffer[Particle] = {
		val potential = spaceHash.neighborsInBox(i, Particle.h*2)
		val neighbors = new ArrayBuffer[Particle]()
		
		potential.foreach { j =>
			if(j ne i) {
				val r = Vector3(i.x, j.x)
				val l = r.norm
				
				if(l < Particle.h) {
					neighbors += j
				}
			}
		}
		
		neighbors
	}
	
	def applyViscosity(dt:Double) {
		foreach { i =>
			i.neighbors.foreach { j =>
				val r = Vector3(i.x, j.x)
				val d = r.norm
				val q = d / Particle.h
				if(q < 1) {
					val R = r.divBy(d) // normalized (but we already computed the norm)
					val vv = i.v - j.v
					val u = vv.dot(R)
					if(u > 0) {
						val I = R.multBy(dt * (1-q) * (Particle.sigma*u + Particle.beta*(u*u)))
						I.divBy(2)
						i.v.subBy(I)
						j.v.addBy(I)
					}
				}
			}
		}
	}
	
	def adjustSprings(dt:Double) {
		// Add and adjust springs.
		foreach { i =>
			i.neighbors.foreach { j =>
				val L = Particle.L
				val r = Vector3(i.x, j.x)
				val rij = r.norm
				val q = rij / Particle.h
				if(q < 1) {
					var spring = i.springs.get(j.index).getOrElse {
						val s = new Spring(i, j, Particle.h)
						springs += s
						s
					}
					val d = Particle.gamma * spring.L
					if(rij > L+d) {
						spring.L += dt * Particle.alpha * (rij-L-d)
					} else if(rij < L-d) {
						spring.L -= dt * Particle.alpha * (L-d-rij) 
					}
				}
			}
		}
		// Remove springs
		springs.retain { spring =>
			if(spring.L > Particle.h) { spring.removed; false } else { true }
		}
//println("springs: %d".format(springs.size))
	}
	
	def applySpringDisplacements(dt:Double) {
		val dt2 = dt * dt
		springs.foreach { spring =>
			var r = Vector3(spring.i.x, spring.j.x)
			val rij = r.normalize
			val D = r.multBy(dt2 * Particle.kspring * (1-spring.L/Particle.h)*(spring.L-rij))
			D /= 2
			spring.i.x -= D
			spring.j.x += D
		}
	}
	
	def doubleDensityRelaxation(dt:Double) {
		// Algorithm 2
		val dt2 = dt*dt
		foreach { i =>
			var rho = 0.0
			var rhoNear = 0.0
			// Compute density and near density
			i.neighbors.foreach { j =>
				val r = Vector3(i.x, j.x)
				val q = r.norm / Particle.h
				if(q < 1) {
					val q1 = (1-q)
					val q2 = q1*q1
					rho += q2
					rhoNear += q2*q1
				}
			}
			// Compute pressure and near-pressure
			var P = Particle.k * (rho - Particle.rhoZero)
			var Pnear = Particle.kNear * rhoNear
			var dx = Vector3(0,0,0)
			i.neighbors.foreach { j =>
				val r = Vector3(i.x, j.x)
				val d = r.norm
				val q = d / Particle.h
				if(q < 1) {
					val q1 = (1-q)
					// Apply displacements
					val R = r.divBy(d) //j.r.normalized
					val D = R.multBy(dt2 * ((P * q1) + (Pnear * (q1*q1))))
					D.divBy(2)
					j.x += D
					dx -= D
				}
			}
			i.x += dx
		}
	}
	
	def resolveCollions(dt:Double) {
		val ground = Vector3( 0, 1, 0)
		val wallX1 = Vector3(-1, 0, 0)
		val wallX2 = Vector3( 1, 0, 0)
		val wallZ1 = Vector3( 0, 0,-1)
		val wallZ2 = Vector3( 0, 0, 1)
		
		// We only test collisions with the ground and walls actually.
		foreach { i =>
			if(i.x.y <  Particle.ground) { handleCollisionWithAxisPlane(dt, i, ground, Particle.ground) }
			if(i.x.x >  Particle.wallsX) { handleCollisionWithAxisPlane(dt, i, wallX1, Particle.wallsX) } else
			if(i.x.x < -Particle.wallsX) { handleCollisionWithAxisPlane(dt, i, wallX2, Particle.wallsX) }
			if(i.x.z >  Particle.wallsZ) { handleCollisionWithAxisPlane(dt, i, wallZ1, Particle.wallsZ) } else  
			if(i.x.z < -Particle.wallsZ) { handleCollisionWithAxisPlane(dt, i, wallZ2, Particle.wallsZ) }
		}
		
		if(Particle.stickiness) {
			val wallXplus = Particle.wallsX - Particle.h/2
			
			foreach { i =>
				if(i.x.x > wallXplus) { handleStickiness(dt, wallX1, i, wallXplus, Particle.wallsX - i.x.x) }
			}
		}
	}

	protected def handleCollisionWithAxisPlane(dt:Double, i:Particle, n:Vector3, p:Double) {
		//      ^
		//      |
		//  ^-->| n     ^
		// v \  |       |
		//    \ |   =>  | vnormal
		//     \|       |
		//  <---+       +
		// vtangent
		//  
		val v = Vector3(i.x, i.xprev)
		// Objects have no velocity, therefore we do not subtract it.
		// v.subBy(object.v)
		val vnormal = n * (v.dot(n))			// vector projection (v.dot(n) = scalar projection), this is the normal vector scaled by the length of the projection of v on it.
		if(Particle.umicron > 0) {
			val vtangent = v.subBy(vnormal)
			vtangent.multBy(Particle.umicron)
			i.x.addBy(vnormal.addBy(vtangent))	// vnormal + vtangent = I
		} else {
			i.x.addBy(vnormal)
		}
	}
	
	protected def handleStickiness(dt:Double, n:Vector3, i:Particle, p:Double, di:Double) {
		// XXX BUG does not seems to work actually... 
		val Istick = n.multBy(-dt * Particle.kStick * di * (1 - (di/Particle.dStick)) )
		i.x.addBy(Istick)
	}
}