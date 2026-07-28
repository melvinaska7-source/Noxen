package ru.noxen.common.util.world;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Unique;
import ru.noxen.api.event.EventHandler;
import ru.noxen.common.QuickImports;
import ru.noxen.core.Main;
import ru.noxen.implement.events.block.BlockEntityProgressEvent;
import ru.noxen.implement.events.block.BlockUpdateEvent;
import ru.noxen.implement.events.render.WorldLoadEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlockFinder implements QuickImports {
    public List<BlockEntity> blockEntities = new ArrayList<>();
    public static BlockFinder INSTANCE = new BlockFinder();

    public BlockFinder() {
        Main.getInstance().getEventManager().register(this);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        blockEntities.clear();
    }

    @EventHandler
    public void onBlockEntityProgress(BlockEntityProgressEvent e) {
        BlockEntity blockEntity = e.blockEntity();
        switch (e.type()) {
            case BlockEntityProgressEvent.Type.ADD -> {
                if (!blockEntities.stream().map(BlockEntity::getPos).toList().contains(blockEntity.getPos())) {
                    blockEntities.add(blockEntity);
                }
            }
            case BlockEntityProgressEvent.Type.REMOVE -> blockEntities.remove(blockEntity);
        }
    }
}