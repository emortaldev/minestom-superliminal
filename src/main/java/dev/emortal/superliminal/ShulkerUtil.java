package dev.emortal.superliminal;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.emortal.superliminal.objects.MinecraftPhysicsObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;

import java.util.*;

public class ShulkerUtil {

    private static final Map<MinecraftPhysicsObject, List<Entity>> SHULKER_ENTITIES = new HashMap<>();

    public static void generateShulkers(MinecraftPhysics minecraftPhysics, MinecraftPhysicsObject mcObject) {
        SHULKER_ENTITIES.getOrDefault(mcObject, new ArrayList<>()).forEach(Entity::remove);

        PhysicsCollisionObject object = mcObject.getCollisionObject();

        Vector3f location = object.getPhysicsLocation(new Vector3f());
        Quaternion quaternion = object.getPhysicsRotation(new Quaternion());
        BoundingBox bb = object.getCollisionShape().boundingBox(location, quaternion, new BoundingBox());
        Vector3f extent = bb.getExtent(new Vector3f());

        var testObj = new PhysicsRigidBody(new BoxCollisionShape(0.01f, 0.01f, 0.01f), 1);

        float resolution = 0.3f;
        PointIterator pointIterator = new PointIterator(bb, resolution);

        List<Entity> entities = new ArrayList<>();
        List<IntVec> scaledPoints = new ArrayList<>();

        while (pointIterator.hasNext()) {
            var point = pointIterator.next();
            var scaledPoint = new IntVec((int) Math.round(point.x() / resolution), (int) Math.round(point.y() / resolution), (int) Math.round(point.z() / resolution));

            testObj.setPhysicsLocation(new Vector3f((float) point.x() - extent.x, (float) point.y() - extent.y, (float) point.z() - extent.z));
            int num = minecraftPhysics.getPhysicsSpace().pairTest(object, testObj, null);
            if (num <= 0) continue;

            scaledPoints.add(scaledPoint);
        }

        for (IntVec point : scaledPoints) {
            float x = point.x() * resolution;
            float y = point.y() * resolution;
            float z = point.z() * resolution;

            // check to make shulkers hollow
            boolean anyNeighbourEmpty = anyNeighborEmpty(point, scaledPoints);
            if (!anyNeighbourEmpty) continue;

            Vec vector = new Vec(x - location.x - extent.x, y - location.y - extent.y, z - location.z - extent.z);

            spawnShulker(mcObject, minecraftPhysics.getInstance(), vector, entities);
        }


        SHULKER_ENTITIES.put(mcObject, entities);
    }

    private static void spawnShulker(MinecraftPhysicsObject mcObject, Instance instance, Vec offset, List<Entity> shulkerEntities) {
        Vector3f startLocation = new Vector3f();
        mcObject.getCollisionObject().getPhysicsLocation(startLocation);
        Pos spawnPos = offset.asPosition().add(startLocation.x, startLocation.y, startLocation.z);


        var holder = new Entity(EntityType.TEXT_DISPLAY);
        holder.setNoGravity(true);

        var shulker = new LivingEntity(EntityType.SHULKER);
        shulker.setNoGravity(true);
        shulker.setInvisible(true);

        shulkerEntities.add(holder);
        shulkerEntities.add(shulker);

        holder.setInstance(instance, spawnPos).thenRun(() -> {
            shulker.setInstance(instance, spawnPos).thenRun(() -> {
                holder.addPassenger(shulker);

                shulker.getAttribute(Attribute.SCALE).setBaseValue(0.1);
            });
        });
    }

    public static boolean anyNeighborEmpty(IntVec pos, List<IntVec> positions) {
        for (Direction value : Direction.values()) {
            if (!positions.contains(pos.add(value.normalX(), value.normalY(), value.normalZ()))) return true;
        }
        return false;
    }

    public record IntVec(int x, int y, int z) {
        public IntVec add(int x, int y, int z) {
            return new IntVec(this.x + x, this.y + y, this.z + z);
        }
    }

    public static class PointIterator implements Iterator<net.minestom.server.collision.BoundingBox.MutablePoint> {
        private float sx, sy, sz;
        float x, y, z;
        private float minX, minY, minZ, maxX, maxY, maxZ;
        private float resolution = 1f;
        private final net.minestom.server.collision.BoundingBox.MutablePoint point = new net.minestom.server.collision.BoundingBox.MutablePoint();

        public PointIterator(BoundingBox boundingBox, float resolution) {
            reset(boundingBox, resolution);
        }

        public void reset(BoundingBox boundingBox, float resolution) {
            this.resolution = resolution;

            Vector3f min = boundingBox.getMin(new Vector3f());
            Vector3f max = boundingBox.getMax(new Vector3f());
            Vector3f extent = boundingBox.getExtent(new Vector3f());

            minX = min.x;
            minY = min.y;
            minZ = min.z;
            maxX = max.x;
            maxY = max.y;
            maxZ = max.z;

            x = minX;
            y = minY;
            z = minZ;

            sx = extent.x;
            sy = extent.y;
            sz = extent.z;
        }

        @Override
        public boolean hasNext() {
            return x <= maxX && y <= maxY && z <= maxZ;
        }

        @Override
        public net.minestom.server.collision.BoundingBox.MutablePoint next() {
            point.set(x + sx, y + sy, z + sz);

            x += resolution;
            if (x > maxX) {
                x = minX;
                y += resolution;
                if (y > maxY) {
                    y = minY;
                    z += resolution;
                }
            }

            return point;
        }
    }

}
