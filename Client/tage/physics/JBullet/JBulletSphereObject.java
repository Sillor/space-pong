package tage.physics.JBullet;

import com.bulletphysics.collision.shapes.SphereShape;

/** If using TAGE, physics objects should be created using the methods in the TAGE Scenegraph class. */

public class JBulletSphereObject extends JBulletPhysicsObject {
	
    private float radius;
    
    public JBulletSphereObject(int uid, float mass, double[] transform, float radius) {
   
    	super(uid, mass, transform, new SphereShape(radius));
        this.radius = radius;
    
    }

}
