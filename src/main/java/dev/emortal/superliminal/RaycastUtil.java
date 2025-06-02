package dev.emortal.superliminal;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;


public class RaycastUtil {

    public static @NotNull Pos raycastCube(Player shooter, Vec direction, PhysicsRigidBody rigidBody, double resolution) {
        Point eyePos = shooter.getPosition().add(0, shooter.getEyeHeight(), 0);
        Block.Getter getter = shooter.getInstance();

        double scaleMultiplier = 1.0 / 4.0;

        Pos current = Pos.fromPoint(eyePos);

        Quaternion quaternion = new Quaternion();
        rigidBody.getPhysicsRotation(quaternion);
        BoundingBox bb = new BoundingBox();
        rigidBody.getCollisionShape().boundingBox(CoordinateUtils.toVector3(current), quaternion, bb);
        Vector3f extentBullet = new Vector3f();
        bb.getExtent(extentBullet);
        Vec extent = CoordinateUtils.toVec(extentBullet).normalize();

        int steps = 0;
        Vec diff = direction.mul(resolution);

        while (steps < 500) {
            current = current.add(diff);

            steps++;

            double distance = eyePos.distance(current);

            // simultaneously moving and scaling a cube to perform the optical illusion
            Vec cornerOffset = extent.mul(distance * scaleMultiplier);

            // check each corner of the bounding box
            Block corner1 = getter.getBlock(current.add(cornerOffset), Block.Getter.Condition.TYPE);
            if (corner1.isSolid()) return current;
            Block corner2 = getter.getBlock(current.add(cornerOffset.mul(1, 1, -1)), Block.Getter.Condition.TYPE);
            if (corner2.isSolid()) return current;
            Block corner3 = getter.getBlock(current.add(cornerOffset.mul(1, -1, 1)), Block.Getter.Condition.TYPE);
            if (corner3.isSolid()) return current;
            Block corner4 = getter.getBlock(current.add(cornerOffset.mul(1, -1, -1)), Block.Getter.Condition.TYPE);
            if (corner4.isSolid()) return current;
            Block corner5 = getter.getBlock(current.add(cornerOffset.mul(-1, 1, 1)), Block.Getter.Condition.TYPE);
            if (corner5.isSolid()) return current;
            Block corner6 = getter.getBlock(current.add(cornerOffset.mul(-1, 1, -1)), Block.Getter.Condition.TYPE);
            if (corner6.isSolid()) return current;
            Block corner7 = getter.getBlock(current.add(cornerOffset.mul(-1, -1, 1)), Block.Getter.Condition.TYPE);
            if (corner7.isSolid()) return current;
            Block corner8 = getter.getBlock(current.add(cornerOffset.mul(-1, -1, -1)), Block.Getter.Condition.TYPE);
            if (corner8.isSolid()) return current;
        }

        return Pos.fromPoint(eyePos.add(direction.mul(2)));
    }

}