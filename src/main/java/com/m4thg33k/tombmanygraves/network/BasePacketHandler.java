package com.m4thg33k.tombmanygraves.network;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class BasePacketHandler implements IMessageHandler<BasePacket, IMessage> {

    @Override
    public IMessage onMessage(BasePacket message, MessageContext ctx) {
        if (message == null || ctx == null) {
            return null;
        }
        if (ctx.side == Side.SERVER) {
            NetHandlerPlayServer handler = ctx.getServerHandler();
            if (handler == null) {
                return null;
            }
            return message.handleServer(handler);
        } else {
            NetHandlerPlayClient handler = ctx.getClientHandler();
            if (handler == null) {
                return null;
            }
            return message.handleClient(handler);
        }
    }
}