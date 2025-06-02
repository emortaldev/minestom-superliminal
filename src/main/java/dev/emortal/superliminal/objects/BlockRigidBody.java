package dev.emortal.superliminal.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.emortal.superliminal.MinecraftPhysics;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BlockRigidBody extends MinecraftPhysicsObject {

    private final Block block;
    private final boolean visible;

    public BlockRigidBody(@NotNull MinecraftPhysics mcPhysics, Vector3f position, Vec size, float mass, boolean visible, Block block) {
        super(mcPhysics, new PhysicsRigidBody(new BoxCollisionShape((float) size.x(), (float) size.y(), (float) size.z()), mass), size);

        this.block = block;
        this.visible = visible;

        var rigidBody = (PhysicsRigidBody) getCollisionObject();
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsLocation(position);
    }

    public void setScale(float scale) {
        getCollisionObject().setCollisionShape(new BoxCollisionShape(scale * 0.5f, scale * 0.5f, scale * 0.5f));
        getEntity().editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(scale * 2);
            meta.setHeight(scale * 2);
            meta.setScale(new Vec(scale));
        });
    }

    @Override
    public Entity createEntity() {
        if (!visible) return null;

        // Uses an ITEM_DISPLAY instead of a BLOCK_DISPLAY as it is centered around the middle instead of the corner
        // although causes issues with certain items, it works for most
        Entity entity = new Entity(EntityType.ITEM_DISPLAY);
        entity.setNoGravity(true);

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setItemStack(ItemStack.of(block.registry().material()));
            meta.setScale(getSize().mul(2));
        });

        return entity;
    }

}
