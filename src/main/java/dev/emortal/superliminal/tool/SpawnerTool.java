package dev.emortal.superliminal.tool;

import dev.emortal.superliminal.MinecraftPhysics;
import dev.emortal.superliminal.objects.BlockRigidBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static dev.emortal.superliminal.CoordinateUtils.toVector3;

public class SpawnerTool extends Tool {

    private final @NotNull Player player;
    private final @NotNull MinecraftPhysics physicsHandler;

    public SpawnerTool(@NotNull Player player, @NotNull MinecraftPhysics physicsHandler) {
        super(player, "spawner");
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
        var body = new BlockRigidBody(physicsHandler, toVector3(player.getPosition().add(0, player.getEyeHeight(), 0).add(player.getPosition().direction().mul(2.5))), new Vec(0.5), 1, true, Block.DIAMOND_BLOCK);
        body.setInstance();
        body.setAlwaysActive(true);
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.DIAMOND)
                .customName(Component.text("Spawner", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .set(Tool.TOOL_NAME_TAG, "spawner")
                .build();
    }

}