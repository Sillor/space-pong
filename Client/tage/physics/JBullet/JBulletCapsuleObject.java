package tage.physics.JBullet;

import com.bulletphysics.collision.shapes.CapsuleShape;

/** If using TAGE, physics objects should be created using the methods in the TAGE Scenegraph class. */

public class JBulletCapsuleObject extends JBulletPhysicsObject {
	private float radius;
	private float height;

    public JBulletCapsuleObject(int uid, float mass, double[] transform, float radius, float height) {

        super(uid, mass, transform, new CapsuleShape(radius, height));
        this.radius = radius;
        this.height = height;
    }
}
