package com.m4thg33k.tombmanygraves.network;

import java.util.List;
import java.util.UUID;

import com.m4thg33k.tombmanygraves.TombManyGraves;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketProbeFiles extends BaseThreadsafePacket{

    private BlockPos pos;
    private int dimension;
    private UUID uuid;

    public PacketProbeFiles()
    {

    }

    public PacketProbeFiles(int dimension, UUID playerUUID, BlockPos pos)
    {
        this.dimension = dimension;
        this.pos = pos;
        this.uuid = playerUUID;
    }

    @Override
    public void handleClientSafe(NetHandlerPlayClient netHandler) {
        throw new UnsupportedOperationException("Server-side only!");
    }

    @Override
    public void handleServerSafe(NetHandlerPlayServer netHandler) {
        if (uuid == null || pos == null) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);
        if (player == null) {
            return;
        }
        List<String> files = TombManyGraves.proxy.probeForFiles(pos);
        TMGNetwork.sendTo(new PacketFileNames(pos, files), player);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writePos(pos, buf);
        buf.writeInt(dimension);
        ByteBufUtils.writeUTF8String(buf, uuid == null ? "" : uuid.toString());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = readPos(buf);
        dimension = buf.readInt();
        String uuidStr = ByteBufUtils.readUTF8String(buf);
        uuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
    }
}