package org.sofa.simu

import org.sofa.Timer
import org.sofa.math.{Point3, Point2, Vector3, NumberSeq3, SpatialHash, SpatialPoint, SpatialObject, SpatialCube, Triangle, ConstTriangle}
import scala.collection.mutable.{Set, HashMap, HashSet, ArrayBuffer, ArrayStack}
import scala.util.Random

/** This allows to configure the ViscoElasticSimulation. It contains all the parameters
  * needed to change the fluid behavior. This is not the best way to setup the VES, since
  * any instance of the VES will be changed by this. But shoudl evolve in the future. */
object Particle {
	var g       = 9.81				// Assuming we use meters as units, g = 9.81 m/s^2
	var h       = 0.8				// 90 cm
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
	var kspring = 100.0				// Spring stiffness (5.1) ???
	var gamma   = 0.2				// Spring yield ratio, typically between 0 and 0.2 and < 1
	var alpha   = 0.1				// Plasticity constant ???
	var maxSpringLength = 1.2 		// Max spring real length as a factor of h. The spring is removed if its real length (rij) is more than h times this factor.
	var springRemoveFactor = 0.9	// Normally, springs are removed as soon as their rest length (L) equals h. This factor modifies h, to select when to remove a spring.

	// Appear in resolveCollions():
	var umicron = 0.5				// Friction parameter between 0 and 1 (0 = slip, 1 = no slip). ???
	var wallsX  = 5.0
	var wallsZ  = 2.0
	var wallY   = 10.0
	var ground  = 0.1
	
	// Stickiness:
	var stickiness = false
	var kStick  = 1.0
	var dStick  = h/3				// Smaller than h

	// 2D Mode ?
	var is2D = false

	var maxVelocity = 50.0

	/** Print the values of all the parameters. */
	def printConfig() {
		println("Particle settings:")
		println("    2D ................ %b".format(is2D))
		println("    g ................. %.4f".format(g))
		println("    h ................. %.4f".format(h))
		println("  Dentity / Pressure:")
		println("    k / kNear ......... %.4f / %.4f".format(k, kNear))
		println("    rhoZero ........... %.4f".format(rhoZero))
		println("  Viscosity:")
		println("    sigma ............. %.4f".format(sigma))
		println("    beta .............. %.4f".format(beta))
		println("  Plasticity (%b):".format(plasticity))
		println("    kspring ........... %.4f".format(kspring))
		println("    gamma ............. %.4f".format(gamma))
		println("    alpha ............. %.4f".format(alpha))
		println("    spring remove ..... %.4f".format(springRemoveFactor))
		println("    max spring length . %.4f".format(maxSpringLength))
		println("  Collisions:")
		println("    umicron ........... %.4f".format(umicron))
		println("  Stickiness (%b):".format(stickiness))
		println("    kStick ............ %.4f".format(kStick))
		println("    dStick ............ %.4f".format(dStick))
	}
}

/** A single spring between two particles. */
class Spring(val i:Particle, val j:Particle, var L:Double) {
	/** Current step of the spring (the last time it was modified). */
	var step = -1
	
	// At construction, the spring registers in its two particles.
	j.springs += ((i.index, this))
	i.springs += ((j.index, this))
	
	/** Called when the spring disappears, it un-registers from its two particles. */
	def removed() {
		i.springs.remove(j.index)
		j.springs.remove(i.index)
	}
}

/** A VESObject is an element that represents any object that occurs in the
  * visco-elastic fluid simulation, be it a fluid particle, or an obstacle
  * object. */
trait VESObject extends SpatialObject {}

/** An abstract obstacle in the VES simulaton. */
trait Obstacle extends VESObject with SpatialCube {
	/** Return a double that represents the distance from the particle
	  * to the obstacle closest surface polygon as well as the nearest point on
	  * the obstacle, and finally the normal of the face of the obstacle the point
	  * lies in. If the distance is negative, the particle is inside the
	  * obstacle. */
	def isNear(p:Particle):(Double,Point3,Vector3)

	def collision(p:Particle):Collision
}

/** A rectangular wall obstacle made of two triangles. */
class QuadWall(p:Point3, v0:Vector3, v1:Vector3) extends Obstacle {
	/** The first (lower) triangle of the wall. */
	val tri0 = ConstTriangle(p,Point3(p.x+v0.x,p.y+v0.y,p.z+v0.z),Point3(p.x+v1.x,p.y+v1.y,p.z+v1.z))

	/** The second (higher) triangle of the wall. */
	val tri1 = ConstTriangle(Point3(p.x+v0.x+v1.x,p.y+v0.y+v1.y,p.z+v0.z+v1.z),tri0.p2,tri0.p1)
	
	def from:Point3 = tri0.p0
	
	def to:Point3 = tri1.p0
	
	def isNear(p:Particle):(Double,Point3,Vector3) = {
		val (d0,p0) = tri0.distanceFrom(p.x)
		val (d1,p1) = tri1.distanceFrom(p.x)
		
		if(d0 < d1) (d0,p0,tri0.normal) else (d1,p1,tri1.normal)
	}

	def collision(p:Particle):Collision = {
		var (d0,p0) = tri0.distanceFrom(p.x)
		var (d1,p1) = tri1.distanceFrom(p.x)
		var tri:ConstTriangle = tri0

		if(d1 < d0) {
			d0  = d1
			p0  = p1
			tri = tri1
		}

		if(d0 < Particle.h) {
			if((p.collision eq null) || ((p.collision ne null) && d0 < p.collision.distance)) {
				new Collision(d0,p0,tri.normal,this)
			} else {
				p.collision
			}
		} else {
			p.collision
		}
	}
}

/** A single SPH particle in the visco-elastic simulation, representing the fluid.
  * 
  * The particle is not a real physical particle, but represents the state of the
  * fluid at its position in a given smoothing area.
  *
  * A particle is a spatial point, that is a location in space
  * that can be hashed and inserted in the spatial hash. */
class Particle(val index:Int) extends VESObject with SpatialPoint {
	/** Actual position. */
	val x = Point3()
	
	/** Previous position, at the previous step. */
	val xprev = Point3()
	
	/** Velocity. */
	val v = Vector3()

	/** Particle density, as computed in ViscoElasticSimulation.doubleDensityRelaxation(). */
	var rho = 0.0
	
	/** Set of visible neighbor (distance < h). This field
	  * is updated by the simulation at the begining of each step. */
	var neighbors:ArrayBuffer[Particle] = null

	/** The nearest obstacle. This field is updated by the simulation
	  * at the begining of each step. */
	var collision:Collision = null
	
	/** Map of springs toward other particles. The particles
	  * are mapped to their index in the simulation (the particles
	  * index is invariant). When a particle is removed, the springs
	  * it references are removed from the global spring list in the
	  * simulation, and from other particles it is tied to by the
	  * Spring.removed() method. */
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

		if(v.x > Particle.maxVelocity) v.x = 0.1
		if(v.y > Particle.maxVelocity) v.y = 0.1

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

	/** Set of neighbors field from the given set of potential neighbors.
	  * The set of neighbors depends on the viewing distance `Particle.h`. If
	  * amongst the neighbors there are some non-particles objects, they are
	  * treated as obstacles and the closest one is stored in the obstacle field. */
	def computeNeighbors(potential:Set[VESObject]) {
		val v = Vector3()

		collision = null

		if(neighbors eq null) {
			neighbors = new ArrayBuffer[Particle]()
		} else {
			neighbors.clear
		}

		potential.foreach { j =>
			if(j.isInstanceOf[Particle]) {
				if(j ne this) {
					v.set(x, j.from)
					
					val l = v.norm
				
					if(l < Particle.h) {
						neighbors += j.asInstanceOf[Particle]
					}
				}
			} else {
				val o = j.asInstanceOf[Obstacle]
				
				collision = o.collision(this)
			}
		}		
	}

	/** Set of neighbors field from the given set of potential neighbors.
	  * The set of neighbors depends on the viewing distance `Particle.h`. If
	  * amongst the neighbors there are some non-particles objects, they are
	  * treated as obstacles and the closest one is stored in the obstacle field. */
	def computeNeighbors(points:ArrayBuffer[Particle], volumes:HashSet[Obstacle]) {
		// val v = Vector3()

		// collision = null

		// if(neighbors eq null) {
		// 	neighbors = new ArrayBuffer[Particle]()
		// } else {
		// 	neighbors.clear
		// }

		// A "match" would be so much cleaner... But again, after some micro bench,
		// on an so important method, that is applied to all particles, this is
		// much slower than "if instanceOf".
		// potential.foreach { j =>
		// 	if(j.isInstanceOf[Particle]) {
		// 		if(j ne this) {
		// 			v.set(x, j.from)
					
		// 			val l = v.norm
				
		// 			if(l < Particle.h) {
		// 				neighbors += j.asInstanceOf[Particle]
		// 			}
		// 		}
		// 	} else {
		// 		val o = j.asInstanceOf[Obstacle]
				
		// 		collision = o.collision(this)
		// 	}
		// }
		
		// if(neighbors eq null) {
		// 	neighbors = new ArrayBuffer[Particle]()
		// } else {
		// 	neighbors.clear
		// }

		// points.foreach { j =>
		// 	if(j ne this) {
		// 		v.set(x, j.from)
					
		// 		val l = v.norm
				
		// 		if(l < Particle.h) {
		// 			neighbors += j.asInstanceOf[Particle]
		// 		}
		// 	}			
		// }
		neighbors = points
	
		collision = null

		volumes.foreach { o =>
			collision = o.collision(this)
		}
	}
}

/** Represents a collision of a particle with an obstacle.
  * This contains the distance at wich the particle is from the obstacle,
  * (under the minimum distance to consider it is an impact), the
  * closest point of impact on the surface, the normal vector to the
  * surface, and the obstacle reference. */
case class Collision(val distance:Double, val hit:Point3, val normal:Vector3, val obstacle:Obstacle) {}

/** A viscoelastic fluid simulator. 
  * 
  * This is entirely based on the very good article "Particle-based viscoelastic
  * fluid simulation" by Simon Clavet, Philippe Beaudoin and Pierre Poulin. As well as
  * on its nonetheless very good PhD thesis "Animation de fluides visco élastiques à
  * base de particules" by Simon Clavet.
  */
class ViscoElasticSimulation extends ArrayBuffer[Particle] {
	/** Used to allocate indices for particles. */
	protected var particlesIndexMax = 0
	
	/** Allow to quickly retrieve the particles in space without browsing all
	  * particles (avoids the O(n^2) complexity). */
	val spaceHash = new SpatialHash[VESObject,Particle,Obstacle](Particle.spacialHashBucketSize) 
	
	/** All the springs actually active between two particles. */
	val springs = new HashSet[Spring]()
	
	/** The simulation stops as soon as this is false. */
	var running = true

	/** The current step. */
	var step = 0
	
	/** All the obstacles. */
	val obstacles = new ArrayBuffer[Obstacle]()
	
	protected val timer = new Timer(Console.out)
	
	protected var evalHFactor = 1.0

	/** A particle at random in the simulation. */
	def randomParticle(random:Random):Particle = this(random.nextInt(size))
	
	protected def neighborsAround(x:Point3, points:ArrayBuffer[Particle], volumes:HashSet[Obstacle]) {
		points.clear
		if(volumes ne null) volumes.clear
		if(Particle.is2D)
			 spaceHash.neighborsInBoxXY(x, Particle.h*evalHFactor, points, volumes)
		else spaceHash.neighborsInBox(x, Particle.h*evalHFactor, points, volumes)
	}

	protected def neighborsAround(x:Point2, points:ArrayBuffer[Particle], volumes:HashSet[Obstacle]) {
		points.clear
		if(volumes ne null) volumes.clear
		spaceHash.neighborsInBoxXY(x, Particle.h*evalHFactor, points, volumes)
	}

	protected def neighborsAround(p:Particle, points:ArrayBuffer[Particle], volumes:HashSet[Obstacle]) {
		points.clear
		if(volumes ne null) volumes.clear
		if(Particle.is2D)
			 spaceHash.neighborsInBoxXY(p, Particle.h*evalHFactor, points, volumes)
		else spaceHash.neighborsInBox(p, Particle.h*evalHFactor, points, volumes)		
	}

	protected val pointBuffer = new ArrayBuffer[Particle]()

	protected val volumeBuffer = new HashSet[Obstacle]()

//var evalCount = 0

	/** Evaluate the iso-surface in 3D at the given point `x`. */
	def evalIsoSurface(x:Point3):Double = {
		var value = 0.0
		neighborsAround(x, pointBuffer, null)
		var I = 0
		val N = pointBuffer.size
	
		while(I < N) {
			val i = pointBuffer(I)
			val r = x.distance(i.from)
			val q = r/Particle.h
			if(q < 1) {
				val v = (1 - q)
				value += (v*v)
			}
			I += 1
		}
		
//evalCount += 1
		math.sqrt(value)
	}

	/** Evaluate the iso-surface in 2D at the given point `x`. */
	def evalIsoSurface(x:Point2):Double = {
		val h2 = Particle.h/2
		var value = 0.0
		var xx = x.x
		var yy = x.y
		spaceHash.getThingsRadius(x, Particle.h, xx-h2, yy-h2, xx+h2, yy+h2, 
			{	(l:Double, p:Particle) => {
					val v = 1 - (l/Particle.h)
					value += (v*v)
				}
			}
		)

// 		var value = 0.0
// 		neighborsAround(x, pointBuffer, null)
// 		var I = 0
// 		val N = pointBuffer.size
	
// 		while(I < N) {
// 			val i = pointBuffer(I)
// 			val r = x.distance(i.from)
// 			val q = r/Particle.h
// 			if(q < 1) {
// 				val v = (1 - q)
// 				value += (v*v)
// 			}
// 			I += 1
// 		}
		
// //evalCount += 1
 		math.sqrt(value)		
	}

	protected val RR = Vector3()

	/** Evaluate the normal to the iso-surface in 3D at point `x`. */
	def evalIsoNormal(x:Point3):Vector3 = {
		val n = Vector3(0, 0, 0)
		neighborsAround(x, pointBuffer, null)
		
		pointBuffer.foreach { i =>
			RR.set(i.from, x)
			val l = RR.normalize
			val q = l/Particle.h
			if(q < 1) {
				RR /= l*l
				n += RR
			}
		}
		
		n.normalize
		n
	}

	/** TODO */
	def evalIsoDensity(x:Point3):Double = { 
		var rho = 0.0
		var n = 0
		neighborsAround(x, pointBuffer, null)
		
		pointBuffer.foreach { i =>
//			RRR.set(i.from, x)
//			val l = RRR.norm
			val l = x.distance(i.from)
			val q = l/Particle.h
			if(q < 1) {
				rho += (i.asInstanceOf[Particle].rho / (l*l))  
			}
			n += 1
		}
		
		rho / n
	}

	def evalIsoDensity(x:Point2):Double = evalIsoDensity(Point3(x.x, x.y, 0))

	/** Add a particle at a given location `p` with given velocity `v`. */
	def addParticle(p:NumberSeq3, v:NumberSeq3):Particle = addParticle(p.x,p.y,p.z, v.x,v.y,v.z)

	/** Add a particle at a given location `p` with given velocity (`vx`,`vy`,`vz`). */
	def addParticle(p:NumberSeq3, vx:Double, vy:Double, vz:Double):Particle = addParticle(p.x,p.y,p.z, vx,vy,vz)

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
	
	/** Add an obstacle. The obstacle is inserted in the obstacles list
	  * and tested for collision with particles in the computeNeighbors()
	  * step. */
	def addObstacle(o:Obstacle) {
		obstacles += o
		spaceHash.add(o)
	}

	/** Apply one simulaton step during time range `dt`. */
	def simulationStepWithTimer(dt:Double) {
		
		// Compute neighbors
		timer.measure("neighbors") {
			computeNeighbors
		}

		// Gravity
		applyGravity(dt)							// Changes V
		
		// Viscosity
		timer.measure("viscosity") {
			applyViscosity(dt)						// Changes V
		}
		
		// Move
		move(dt)									// Changes X
		
		// Add and remove springs, change rest lengths
		if(Particle.plasticity) {
			timer.measure("adjustSprings") {
				adjustSprings(dt)					// Does not changes X nor V
			}
			timer.measure("applySprings") {
				// Modify positions according to springs
				// double density relaxation, and collisions
				applySpringDisplacements(dt)		// Changes X
			}
		}
		
		timer.measure("dblDstRelax") {
			doubleDensityRelaxation(dt)				// Changes X
		}
		timer.measure("collisions") {
			resolveCollions(dt)						// Changes X
		}
		timer.measure("velocity") {
			computeVelocity(dt)
		}
		
		// Update the space hash
		timer.measure("spaceHash") {
			updateSpaceHash
		}

		// One step finished.
		step += 1

		if(step % 10 == 0) {
			timer.printAvgs("-- Times --------")
		}
		if(step % 1000 == 0) {
			println("### RESET TIMER ##########################")
			timer.reset
		}

	}
	/** Apply one simulaton step during time range `dt`. */
	def simulationStep(dt:Double) {		
		// Compute neighbors
		computeNeighbors

		// Gravity
		applyGravity(dt)							// Changes V
		
		// Viscosity
		applyViscosity(dt)						// Changes V
		
		// Move
		move(dt)									// Changes X
		
		// Add and remove springs, change rest lengths
		if(Particle.plasticity) {
			adjustSprings(dt)					// Does not changes X nor V
			// Modify positions according to springs
			// double density relaxation, and collisions
			applySpringDisplacements(dt)		// Changes X
		}
		
		doubleDensityRelaxation(dt)				// Changes X
		resolveCollions(dt)						// Changes X
		computeVelocity(dt)
		
		// Update the space hash
		updateSpaceHash

		// One step finished.
		step += 1
	}

	def computeNeighbors() {
		// foreach { p => p.computeNeighbors(spaceHash.neighborsInBoxXY(p, Particle.h*evalHFactor)) }

		foreach { p =>
			volumeBuffer.clear
		//	pointBuffer.clear
		
			if(p.neighbors eq null)
			 	p.neighbors = new ArrayBuffer[Particle]()
			else
		     	p.neighbors.clear
			
			spaceHash.neighborsInBoxRadiusXY(p, Particle.h*evalHFactor, Particle.h, p.neighbors, volumeBuffer)
			p.computeNeighbors(p.neighbors, volumeBuffer)

		}
	}

	def applyGravity(dt:Double) {
		foreach { _.applyGravity(dt) }
	}

	def move(dt:Double) {
		foreach { _.move(dt) }
	}

	def computeVelocity(dt:Double) {
		foreach { _.computeVelocity(dt) }
	}

	def updateSpaceHash() {
		foreach { spaceHash.move(_) }		
	}
	
	// /** Set of neighbors of a given particle `i`. 
	//   * The set of neighbors depends on the viewing distance `Particle.h`. If
	//   * amongst the neighbors there are some non-particles objects, they are
	//   * treated as obstacles and the closest one is returned. */
	// def computeNeighbors(i:Particle) {
	// 	val potential = neighborsAround(i)
	// 	val v = Vector3()

	// 	if(i.neighbors eq null) {
	// 		i.neighbors = new ArrayBuffer[Particle]()
	// 	} else {
	// 		i.neighbors.clear
	// 	}

	// 	i.collision = null

	// 	potential.foreach { j =>
	// 		if(j.isInstanceOf[Particle]) {
	// 			if(j ne i) {
	// 				i.neighbors += j.asInstanceOf[Particle]
	// 			}
	// 		} else {
	// 			val o = j.asInstanceOf[Obstacle]
				
	// 			i.collision = o.collision(i)
	// 		}
	// 	}
	// }
		
	/** The viscosity step during `dt` time. */
	def applyViscosity(dt:Double) {
		// We use "while" instead of beautiful foreach since it is much more 
		// efficient (no function call).

		val r  = Vector3()
		val vv = Vector3()
		var I  = 0
		val N  = size
		
		while(I < N) {
			val i = this(I)
			var J = 0
			val M = i.neighbors.size

			while(J < M) {
				// Not sure yet, but in theory, i.x and j.x did not changed since computeNeighbors
				// and Therefore j is still at the same distance. Hence, we do not need to recompute
				// d and q, we could reuse them.
				val j = i.neighbors(J)
				
				r.set(i.x, j.x)
				
				val d = r.norm
				val q = d / Particle.h
				
				if(q < 1) {
					val R = r.divBy(d) // normalized (but we already computed the norm)
					//val vv = i.v - j.v   // This hides the creation of a vector that multiply times by more than 2 (we are in O(n^2)) !
					vv.copy(i.v); vv.subBy(j.v)
					val u = vv.dot(R)
					
					if(u > 0) {
						val I = R.multBy(dt * (1-q) * (Particle.sigma*u + Particle.beta*(u*u)))
						I.divBy(2)
						i.v.subBy(I)
						j.v.addBy(I)
					}
				}				

				J += 1
			}

			I += 1
		}
	}
	
	/** Add new springs, adjust existing ones, and remove old ones during `dt` time. */
	def adjustSprings(dt:Double) {
		// Add and adjust springs.
		val r = Vector3()

		foreach { i =>
			i.neighbors.foreach { j =>
				var spring = i.springs.get(j.index).getOrElse(null)

				if((spring eq null) || spring.step < step) {
					r.set(i.x, j.x)
					
					val rij = r.norm
					val q   = rij / Particle.h

					if(q < 1) {
						if(spring eq null) {
							// Contrary to the original algorithm we do
							// not initialize the rest length L to h but to
							// the actual length separating the particles.
							spring = new Spring(i, j, rij);//Particle.h)
							springs += spring
						}
					}
					
					// At the contrary of the original algoritm we do this
					// event if q >= 1, since I do not see how we can augment L
					// above h if we only do this when rij is under h !		
					if(spring ne null) {
						val L = spring.L
						val d = Particle.gamma * L
						
						if(rij > L+d) {	// Strech
							spring.L += dt * Particle.alpha * (rij-L-d)
						} else if(rij < L-d) {	// Compress
							spring.L -= dt * Particle.alpha * (L-d-rij) 
						}

						// // Contrary to the original algorithm we remove the spring under the h
						// // distance to avoid building too much springs and fastening the simulation.
						// // This incurs a lost of precision, however.
						// // if(spring.L > Particle.h) ...
						// if(spring.L > Particle.h*Particle.springRemoveFactor) {
						// 	spring.removed
						// 	springs.remove(spring)
						// } else {
							spring.step = step
						// }
					}
				}
			}
		}

		// Remove springs

		// springs.retain { spring =>
		// 	// Contrary to the original algorithm we remove the spring under the h
		// 	// distance to avoid building too much springs and fastening the simulation.
		// 	// This incurs a lost of precision, however.
		// 	// if(spring.L > Particle.h) ...
		// 	if(spring.L > Particle.h*Particle.springRemoveFactor) { spring.removed; false } else { true }
		// }
	}
	
	/** Apply the springs displacements to the particles during `dt` time. */
	def applySpringDisplacements(dt:Double) {
		val dt2 = dt * dt
		val r = Vector3()

		// Process and remove springs at once.

		springs.retain { spring =>
			r.set(spring.i.x, spring.j.x)

			val rij = r.normalize

			if(rij < Particle.h*Particle.maxSpringLength && spring.L < Particle.h*Particle.springRemoveFactor) {
				val D   = r.multBy(dt2 * Particle.kspring * (1-spring.L/Particle.h)*(spring.L-rij))
				D      /= 2
			
				spring.i.x -= D
				spring.j.x += D

				true
			} else {
				spring.removed
				false
			}
		}
		
		// springs.foreach { spring =>
		// 	r.set(spring.i.x, spring.j.x)

		// 	val rij = r.normalize

		// 	if(rij < Particle.h*1.2) {
		// 		val D   = r.multBy(dt2 * Particle.kspring * (1-spring.L/Particle.h)*(spring.L-rij))
		// 		D      /= 2
			
		// 		spring.i.x -= D
		// 		spring.j.x += D
		// 	}
		// }
	}
	
	/** The main feature of the algorithm (see paper). */
	def doubleDensityRelaxation(dt:Double) {
		// Algorithm 2, forget "foreach", beautiful but so slow.
		var dx  = Vector3()
		val r   = Vector3()
		val dt2 = dt*dt
		var I   = 0
		val N   = size

		while(I < N) {
			val i       = this(I)
			var rho     = 0.0
			var rhoNear = 0.0
			var J       = 0
			val M       = i.neighbors.size

			// Compute density and near density
			while(J < M) {
				val j = i.neighbors(J)
				r.set(i.x, j.x)
				val q = r.norm / Particle.h
				
				if(q < 1) {
					val q1 = (1-q)
					val q2 = q1*q1
					rho     += q2
					rhoNear += q2*q1
				}

				J += 1
			}

			// Compute pressure and near-pressure
			i.rho     = rho
			var P     = Particle.k * (rho - Particle.rhoZero)
			var Pnear = Particle.kNear * rhoNear

			dx.set(0, 0, 0)
			
			J = 0

			while(J < M) {
				val j = i.neighbors(J)
				r.set(i.x, j.x)
				val d = r.norm
				val q = d / Particle.h
				
				if(q < 1) {
					val q1 = (1-q)
					// Apply displacements
					val R = r.divBy(d) //j.r.normalized
					val D = R.multBy(dt2 * ((P * q1) + (Pnear * (q1*q1))))
					
					D.divBy(2)
					
					j.x += D
					dx  -= D
				}

				J += 1
			}
			
			i.x += dx

			I += 1
		}
	}

	val ground = Vector3( 0, 1, 0)
	val wallY  = Vector3( 0,-1, 0)
	val wallX1 = Vector3(-1, 0, 0)
	val wallX2 = Vector3( 1, 0, 0)
	val wallZ1 = Vector3( 0, 0,-1)
	val wallZ2 = Vector3( 0, 0, 1)
	
	/** Apply displacements from collisions with objects. */
	def resolveCollions(dt:Double) {
		// We only test collisions with the ground and walls actually.
		foreach { i =>
			if(i.x.y <  Particle.ground) { handleCollisionWithAxisPlane(dt, i, ground, Particle.ground) } else
			if(i.x.y >  Particle.wallY)  { handleCollisionWithAxisPlane(dt, i, wallY,  Particle.wallY) }
			
			if(i.x.x >  Particle.wallsX) { handleCollisionWithAxisPlane(dt, i, wallX1, Particle.wallsX) } else
			if(i.x.x < -Particle.wallsX) { handleCollisionWithAxisPlane(dt, i, wallX2, Particle.wallsX) }
			
			if(i.x.z >  Particle.wallsZ) { handleCollisionWithAxisPlane(dt, i, wallZ1, Particle.wallsZ) } else  
			if(i.x.z < -Particle.wallsZ) { handleCollisionWithAxisPlane(dt, i, wallZ2, Particle.wallsZ) }
			
			if(i.collision ne null) {
				handleCollisionWithObstacle(dt, i)
			}
		}
	}
	
	protected def handleCollisionWithObstacle(dt:Double, i:Particle) {
		val c = i.collision
		
		if(c.distance < Particle.h/2) {
			val v = Vector3(i.x, i.xprev)
			
			// Objects have no velocity, therefore we do not subtract it.
			// v.subBy(object.v)
			
			val vnormal = c.normal * (v.dot(c.normal))			// vector projection (v.dot(n) = scalar projection), this is the normal vector scaled by the length of the projection of v on it.
		
			if(Particle.umicron > 0) {
				val vtangent = v.subBy(vnormal)
				vtangent.multBy(Particle.umicron)
				i.x.addBy(vnormal.addBy(vtangent))	// vnormal + vtangent = I
			} else {
				i.x.addBy(vnormal)
			}		

			if(Particle.stickiness) {
				handleStickiness(dt, i, c)// c.normal, i, wallXplus, c.distance)
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
	
	protected def handleStickiness(dt:Double, i:Particle, c:Collision) {// n:Vector3, i:Particle, di:Double) {
		// XXX BUG does not seems to work actually...
		val Istick = c.normal * (-dt * Particle.kStick * c.distance * (1 - (c.distance/Particle.dStick)) )
Console.err.print("stickiness %.4f (x=%.4f ->".format(Istick(0), i.x.x))
		i.x.addBy(Istick)
Console.err.println(" %.4f)".format(i.x.x))
	}
}