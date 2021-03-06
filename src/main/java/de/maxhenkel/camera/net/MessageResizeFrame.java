package de.maxhenkel.camera.net;

import de.maxhenkel.camera.entities.ImageEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;

public class MessageResizeFrame implements Message<MessageResizeFrame> {

    private UUID uuid;
    private Direction direction;
    private boolean larger;

    public MessageResizeFrame() {

    }

    public MessageResizeFrame(UUID uuid, Direction direction, boolean larger) {
        this.uuid = uuid;
        this.direction = direction;
        this.larger = larger;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        if (context.getSender().world instanceof ServerWorld && context.getSender().abilities.allowEdit) {
            ServerWorld world = (ServerWorld) context.getSender().world;
            Entity entity = world.getEntityByUuid(uuid);
            if (entity instanceof ImageEntity) {
                ImageEntity image = (ImageEntity) entity;
                image.resize(direction, larger);
            }
        }
    }

    @Override
    public MessageResizeFrame fromBytes(PacketBuffer buf) {
        uuid = buf.readUniqueId();
        direction = Direction.values()[buf.readInt()];
        larger = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(PacketBuffer buf) {
        buf.writeUniqueId(uuid);
        buf.writeInt(direction.ordinal());
        buf.writeBoolean(larger);
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;
    }

}
