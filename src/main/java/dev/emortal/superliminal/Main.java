package dev.emortal.superliminal;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import dev.emortal.superliminal.tool.GrabberTool;
import dev.emortal.superliminal.tool.SpawnerTool;
import dev.emortal.superliminal.worldmesh.ChunkMesher;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) throws Exception {
        LibraryInfo info = new LibraryInfo(
                new DirectoryPath("linux/x86-64/com/github/stephengold"),
                "bulletjme", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(info);
        NativeDynamicLibrary[] libraries = new NativeDynamicLibrary[]{
//                new NativeDynamicLibrary("native/linux/arm64", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("native/linux/arm32", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary("native/linux/x86_64", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("native/osx/arm64", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("native/osx/x86_64", PlatformPredicate.MACOS_X86_64),
//                new NativeDynamicLibrary("native/windows/x86_64", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        loader.loadLibrary(LoadingCriterion.INCREMENTAL_LOADING);

        System.setProperty("minestom.tps", "60");

        var server = MinecraftServer.init();
        MojangAuth.init();

        var fullbrightDimension = DimensionType.builder().ambientLight(1f).build();
        var fullbright = MinecraftServer.getDimensionTypeRegistry().register(Key.key("fullbright"), fullbrightDimension);

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer(fullbright);
        instance.setGenerator(g -> {
            g.modifier().fillHeight(-1, 0, Block.GRASS_BLOCK);
        });

        var minecraftPhysics = new MinecraftPhysics(instance);

        var global = MinecraftServer.getGlobalEventHandler();

        global.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
        });

        global.addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().getInventory().setItemStack(5, new GrabberTool(e.getPlayer(), minecraftPhysics).getItem());
            e.getPlayer().getInventory().setItemStack(4, new SpawnerTool(e.getPlayer(), minecraftPhysics).getItem());
            e.getPlayer().setAllowFlying(true);
            e.getPlayer().setGameMode(GameMode.CREATIVE);
        });

        global.addListener(ItemDropEvent.class, e -> {
            e.setCancelled(true);
        });


        Map<Long, PhysicsCollisionObject> meshMap = new ConcurrentHashMap<>();
//        global.addListener(InstanceChunkLoadEvent.class, e -> {
//            PhysicsCollisionObject meshObj = ChunkMesher.createChunk(e.getChunk());
//            minecraftPhysics.getPhysicsSpace().add(meshObj);
//            meshMap.put(chunkIndex(e.getChunkX(), e.getChunkZ()), meshObj);
//        });

        global.addListener(PlayerBlockPlaceEvent.class, e -> {
            if (!e.isCancelled()) {
                // regenerate chunk mesh
                e.getInstance().scheduler().buildTask(() -> {
                    Chunk chunk = e.getInstance().getChunkAt(e.getBlockPosition());
                    if (chunk == null) return;
                    long chunkIndex = chunkIndex(chunk.getChunkX(), chunk.getChunkZ());
                    PhysicsCollisionObject meshObj = meshMap.get(chunkIndex);
                    minecraftPhysics.getPhysicsSpace().remove(meshObj);

                    PhysicsCollisionObject newMeshObj = ChunkMesher.createChunk(chunk);
                    minecraftPhysics.getPhysicsSpace().add(newMeshObj);
                    meshMap.put(chunkIndex, newMeshObj);
//                    e.getPlayer().sendMessage("Generated chunk mesh " + chunkIndex);
                }).delay(TaskSchedule.tick(1)).schedule();

            }
        });
        global.addListener(PlayerBlockBreakEvent.class, e -> {
            if (!e.isCancelled()) {
                // regenerate chunk mesh
                e.getInstance().scheduler().buildTask(() -> {
                    Chunk chunk = e.getInstance().getChunkAt(e.getBlockPosition());
                    if (chunk == null) return;
                    long chunkIndex = chunkIndex(chunk.getChunkX(), chunk.getChunkZ());
                    PhysicsCollisionObject meshObj = meshMap.get(chunkIndex);
                    minecraftPhysics.getPhysicsSpace().remove(meshObj);

                    PhysicsCollisionObject newMeshObj = ChunkMesher.createChunk(chunk);
                    minecraftPhysics.getPhysicsSpace().add(newMeshObj);
                    meshMap.put(chunkIndex, newMeshObj);
//                    e.getPlayer().sendMessage("Generated chunk mesh " + chunkIndex);
                }).delay(TaskSchedule.tick(1)).schedule();
            }
        });


        instance.scheduler().buildTask(new Runnable() {
            long lastRan = System.nanoTime();

            @Override
            public void run() {
                long diff = System.nanoTime() - lastRan;
                float deltaTime = diff / 1_000_000_000f;

                lastRan = System.nanoTime();
                minecraftPhysics.update(deltaTime);
            }
        }).repeat(TaskSchedule.tick(1)).schedule();


        server.start("0.0.0.0", 25565);
    }

    private static long chunkIndex(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }


}