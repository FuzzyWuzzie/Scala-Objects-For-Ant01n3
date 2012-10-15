package org.sofa.simu

import scala.collection.mutable.ArrayBuffer
import org.sofa.math.Vector3
import org.sofa.math.Point3
import org.sofa.math.SpatialHash
import org.sofa.math.SpatialPoint
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.sofa.math.SpatialObject
import org.sofa.math.SpatialCube
import scala.util.Random

// XXX Améliorations possibles:

// Le test de collision des murs est fait pour toutes les particules à tout moment, mais il n'est besoin de le faire que pour les
// cubes du spaceHash qui sont le long de ces murs ! Il reste à inserer les murs dans le space hash, ainsi que
// d'autres objets !

// Ajouter un mode 2D

// Permettre de retirer des particules.

// Allow to easily configure the parameters

object Particle {
	var g       = 9.81				// Assuming we use meters as units, g = 9.81 m/s^2
	var h       = 0.9				// 90 cm
	var spacialHashBucketSize = 1	// 1 m

	// Appear in doubleDensityRelaxation():
	var k       = 1.0				// Pressure stiffness ???
	var kNear   = k * 10			// Near-pressure stiffness ??? Seems to be 10 times k
	var rhoZero = h/2				// Rest density (4.1), the larger the denser, less than 1 == gaz ??? seems to be half of h

	// Appear in applyViscosity():
	var sigma   = 2.0				// Viscosity linear dependency on velocity (between 0 and +inf), augment for highly viscous (lava, sperm).
	var beta    = 6.0				// Viscosity quadratic dependency on velocity (between 0 and +inf), only this one for less viscous fluids (water).

	// Appear in adjustSprings() and applySpringDisplacements():
	var plasticity = false
	var L       = h					// Spring rest length ??? Should be smaller than h.
	var kspring = 0.3				// Spring stiffness (5.1) ???
	var gamma   = 0.1				// Spring yield ratio, typically between 0 and 0.2
	var alpha   = 0.1				// Plasticity constant ???
		
	// Appear in resolveCollions():
	var umicron = 0.1				// Friction parameter between 0 and 1 (0 = slip, 1 = no slip). ???
	var wallsX  = 5.0
	var wallsZ  = 2.0
	var ground  = 0.1
	
	// Stickiness:
	var stickiness = false
	var kStick  = 1.0
	var dStick  = h/3				// Smaller than h
}
/*
object Particle {
	var g       = 9.81				// Assuming we use meters as units, g = 9.81 m/s^2
	var h       = 0.5				// 50 cm
	var spacialHashBucketSize = 1	// 1 m

	// Appear in doubleDensityRelaxation():
	var k       = 1					// Pressure stiffness ???
	var kNear   = k * 10			// Near-pressure stiffness ??? Seems to be 10 times k
	var rhoZero = h/2				// Rest density (4.1), the larger then denser, less than 1 == gaz ??? seems to be half of h

	// Appear in applyViscosity():
	var sigma   = 0					// Viscosity linear dependency on velocity (between 0 and +inf), augment for highly viscous (lava, sperm).
	var beta    = 0.1				// Viscosity quadratic dependency on velocity (between 0 and +inf), only this one for less viscous fluids (water).

	// Appear in adjustSprings() and applySpringDisplacements():
	var plasticity = false
	var L       = h					// Spring rest length ??? Should be smaller than h.
	var kspring = 0.3				// Spring stiffness (5.1) ???
	var gamma   = 0.1				// Spring yield ratio, typically between 0 and 0.2
	var alpha   = 0.1				// Plasticity constant ???
		
	// Appear in resolveCollions():
	var umicron = 0.0				// Friction parameter between 0 and 1 (0 = slip, 1 = no slip). ???
	var wallsX  = 5.0
	var wallsZ  = 0.4
	var ground  = 0.1
	
	// Stickiness:
	var stickiness = false
	var kStick  = 1.0
	var dStick  = h/3				// Smaller than h
}
*/

/** A single spring between two particles. */
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

trait VESObject extends SpatialObject {
	
}

trait Obstacle extends VESObject with SpatialCube {
	/** Return a double that represents the distance from the particle to the obstacle closest surface polygon. */
	def isNear(p:Particle):Double
}

class Wall(val axis:Int, val position:Double) extends Obstacle {
	def from:Point3 = null
	
	def to:Point3 = null
	
	def isNear(p:Particle):Double = {
		100000
	}
}

/** A single SPH particle in the visco-elastic simulation.
  * 
  * A particle is a spatial point, that is a location in space
  * that can be hashed and inserted in the spatial hash. */
class Particle(val index:Int) extends VESObject with SpatialPoint {
	/** Actual position. */
	val x = Point3()
	
	/** Previous position. */
	val xprev = Point3()
	
	/** Velocity. */
	val v = Vector3()
	
	/** Set of visible neighbor (distance < h). */
	var neighbors:ArrayBuffer[Particle] = null

	/** Set of visible obstacles. */
	var obstacles:ArrayBuffer[Obstacle] = null
	
	/** Map of springs toward other particles. The particles
	  * are mapped to their index in the simulation. */
	var springs = new HashMap[Int,Spring]()
	
	/** Position. */
	def from:Point3 = x
	
	/** As this is a non volumic object, this is equal to position. */
	def to:Point3 = x

	/** Apply gravity to the velocity. */
	def applyGravity(dt:Double) {
		v.y -= dt * Particle.g
	}
	
	/** Move the particle using its velocity.
	  * The old position is stored in the xprev field. */
	def move(dt:Double) {
		xprev.copy(x)
		x.x += v.x * dt
		x.y += v.y * dt
		x.z += v.z * dt
	}
	
	/** Compute the new velocity from the current position and the previous one. */
	def computeVelocity(dt:Double) {
		v.x = (x.x - xprev.x) / dt
		v.y = (x.y - xprev.y) / dt
		v.z = (x.z - xprev.z) / dt
	}
}

/** A viscoelastic fluid simulator. 
  * 
  * This is entirely based on the very good article "Particle-based viscoelastic
  * fluid simulation" by Simon Clavet, Philippe Beaudoin and Pierre Poulin. As well as
  * on the nonetheless very good PhD thesis "Animation de fluides visco élastiques à
  * base de particules" by Simon Clavet.
  */
class ViscoElasticSimulation extends ArrayBuffer[Particle] {
	/** Used to allocate indices for particles. */
	protected var particlesIndexMax = 0
	
	/** Allow to quickly retrieve the particles in space without browsing all
	  * particles (avoids the O(n^2) complexity). */
	val spaceHash = new SpatialHash[VESObject](Particle.spacialHashBucketSize) 
	
	/** All the springs actually active between two particles. */
	val springs = new HashSet[Spring]()
	
	/** The simulation stops as soon as this is false. */
	var running = true
	
	/** A particle at random in the simulation. */
	def randomParticle(random:Random):Particle = this(random.nextInt(size))
	
	/** Evaluate the iso-surface at the given point `x`. */
	def evalIsoSurface(x:Point3):Double = {
		var value = 0.0
		val neighbors = spaceHash.neighborsInBox(x, Particle.h)
		
		neighbors.foreach { i =>
			val R = Vector3(x, i.from)
			val r = R.norm
			val q = r/Particle.h
			if(q < 1) {
				val v = (1 - q)
				value += (v*v)
			}
		}
		
		math.sqrt(value)
	}

	/** Evaluate the normal to the iso-surface at point `x`. */
	def evalIsoNormal(x:Point3):Vector3 = {
		val n = Vector3(0, 0, 0)
		val neighbors = spaceHash.neighborsInBox(x, Particle.h)
		
		neighbors.foreach { i =>
			val R = Vector3(i.from, x)
			val l = R.normalize
			val q = l/Particle.h
			if(q < 1) {
				R /= l*l
				n += R
			}
		}
		
		n.normalize
		n
	}
	
	/** Add a particle at a given location (`x`,`y`,`z`) with given velocity (`vx`,`vy`,`vz`). */
	def addParticle(x:Double, y:Double, z:Double, vx:Double, vy:Double, vz:Double):Particle = {
		val p = new Particle(particlesIndexMax) 
		
		particlesIndexMax += 1
		
		p.x.set(x, y, z)
		p.v.set(vx, vy, vz)
		spaceHash.add(p)
		
		this += p
		
		p		
	}
	
	/** Remove a particle from the simulation. */
	def removeParticle(p:Particle) {
		this -= p
		spaceHash.remove(p)
		
		// find springs that may reference this particle
		if(Particle.plasticity) {			
			val s = p.springs.map { item => item._2 }
			
			s.foreach { spring =>
				spring.removed
				springs.remove(spring)
			}
		}
	}
	
	/** Add a particle at a given location (`x`,`y`,`z`) with zero velocity. */
	def addParticle(x:Double, y:Double, z:Double):Particle = addParticle(x,y,z, 0, 0, 0)
	
	/** Apply one simulaton step during time range `dt`. */
	def simulationStep(dt:Double) {
		
		// Compute neighbors
		foreach { p => val (n,o) = computeNeighbors(p); p.neighbors = n; p.obstacles = o }

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
	
	/** Set of neighbors of a given particle `i`. 
	  * The set of neighbors depends on the viewing distance `Particle.h`. */
	def computeNeighbors(i:Particle):(ArrayBuffer[Particle], ArrayBuffer[Obstacle]) = {
		val potential = spaceHash.neighborsInBox(i, Particle.h*2)
		val neighbors = new ArrayBuffer[Particle]()
		val obstactles = new ArrayBuffer[Obstacle]()
		
		potential.foreach { j =>
			if(j.isInstanceOf[Particle]) {
				if(j ne i) {
					val r = Vector3(i.x, j.from)
					val l = r.norm
				
					if(l < Particle.h) {
						neighbors += j.asInstanceOf[Particle]
					}
				}
			} else {
				val o = j.asInstanceOf[Obstacle]
				val l = o.isNear(i)
				
				if(l < Particle.h) {
					obstactles += o
				}
			}
		}
		
		(neighbors, obstactles)
	}
	
	/** The viscosity step during `dt` time. */
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
	
	/** Add new springs, adjust existing ones, and remove old ones during `dt` time. */
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
	}
	
	/** Apply the springs displacements to the particles during `dt` time. */
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
	
	/** The main feature of the algorithm (see paper). */
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
	
	/** Apply displacements from collisions with objects. */
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