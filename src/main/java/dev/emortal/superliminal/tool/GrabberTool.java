package dev.emortal.superliminal.tool;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import dev.emortal.superliminal.MinecraftPhysics;
import dev.emortal.superliminal.RaycastUtil;
import dev.emortal.superliminal.ShulkerUtil;
import dev.emortal.superliminal.objects.BlockRigidBody;
import dev.emortal.superliminal.objects.MinecraftPhysicsObject;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.emortal.superliminal.CoordinateUtils.toPos;
import static dev.emortal.superliminal.CoordinateUtils.toVector3;

public class GrabberTool extends Tool {

    private @Nullable PhysicsRigidBody heldObject = null;
    private @Nullable Task holdingTask = null;

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;

    public GrabberTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "grabber");
        this.player = player;
        this.physicsHandler = physicsHandler;
    }

    @Override
    void onSwitchHands() {

    }

    @Override
    public void onLeftClick() {

    }

    @Override
    public void onRightClick() {
        if (holdingTask != null) {
            MinecraftPhysicsObject mcObj = physicsHandler.getObjectByPhysicsObject(heldObject);
            if (mcObj == null || mcObj.getEntity() == null) return;

            mcObj.getEntity().setGlowing(false);

            ShulkerUtil.generateShulkers(physicsHandler, mcObj);

            physicsHandler.getPhysicsSpace().add(heldObject);

            holdingTask.cancel();
            holdingTask = null;
            heldObject = null;

            player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 1.8f), Sound.Emitter.self());
            return;
        }

        List<PhysicsRayTestResult> results = raycastEntity(physicsHandler.getPhysicsSpace(), player.getPosition().add(0, player.getEyeHeight(), 0), player.getPosition().direction(), 1000);
        if (results.isEmpty()) return;

        PhysicsCollisionObject obj = results.getFirst().getCollisionObject();
        if (!(obj instanceof PhysicsRigidBody rigidBody)) return;

        MinecraftPhysicsObject mcObj = physicsHandler.getObjectByPhysicsObject(rigidBody);
        if (mcObj == null || mcObj.getEntity() == null) return;

        mcObj.getEntity().setGlowing(true);

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_PLACE, Sound.Source.MASTER, 0.5f, 2f), Sound.Emitter.self());

        rigidBody.activate();
        heldObject = rigidBody;

        physicsHandler.getPhysicsSpace().remove(rigidBody);

        Vector3f objPos = new Vector3f();
        obj.getPhysicsLocation(objPos);


        holdingTask = player.scheduler().buildTask(() -> {
            Pos point = RaycastUtil.raycastCube(player, player.getPosition().direction(), rigidBody, 0.1)
                    .withYaw(0).withPitch(0);

            Point eyePos = player.getPosition().add(0, player.getEyeHeight(), 0);

            double distance = point.distance(eyePos);

            Vector3f vector = toVector3(point);
            rigidBody.setPhysicsLocation(vector);
            Transform transform = new Transform();
            rigidBody.getTransform(transform);

            mcObj.getEntity().teleport(toPos(transform.getTranslation()));
            ((BlockRigidBody) mcObj).setScale((float) distance / 4);
        }).repeat(TaskSchedule.tick(1)).schedule();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.BLAZE_ROD)
                .customName(Component.text("Grabber", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "grabber")
                .build();
    }

    public static List<PhysicsRayTestResult> raycastEntity(PhysicsSpace physicsSpace, @NotNull Point startPoint, @NotNull Point direction, double maxDistance) {
        Point endPoint = startPoint.add(Vec.fromPoint(direction).normalize().mul(maxDistance));

        return physicsSpace.rayTest(new Vector3f((float) startPoint.x(), (float) startPoint.y(), (float) startPoint.z()), new Vector3f((float) endPoint.x(), (float) endPoint.y(), (float) endPoint.z()));
    }
}