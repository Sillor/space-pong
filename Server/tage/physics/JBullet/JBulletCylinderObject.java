package tage.physics.JBullet;

import com.bulletphysics.collision.shapes.CylinderShape;
import javax.vecmath.Vector3f;

/** If using TAGE, physics objects should be created using the methods in the TAGE Scenegraph class. */

public class JBulletCylinderObject extends JBulletPhysicsObject {
	
    private float[] halfExtents;

    public JBulletCylinderObject(int uid, float mass, double[] transform, float[] halfExtents) {

        super(uid, mass, transform, new CylinderShape(new Vector3f(halfExtents)));
        this.halfExtents = halfExtents;
    }

}
