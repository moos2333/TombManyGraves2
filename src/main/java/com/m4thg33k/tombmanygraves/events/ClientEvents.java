package com.m4thg33k.tombmanygraves.events;

import com.m4thg33k.tombmanygraves.Names;
import com.m4thg33k.tombmanygraves.client.fx.ParticleRenderDispatcher;
import com.m4thg33k.tombmanygraves.client.render.models.AdaptiveGraveModel;
import com.m4thg33k.tombmanygraves.util.Utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEvents {

    private static final ResourceLocation GRAVE_MODEL = Utility.getResource("block/grave");
    private static final ModelResourceLocation GRAVE_MODEL_LOCATION =
            Utility.getModelResource(Names.GRAVE_BLOCK, "normal");
    private static final ResourceLocation RED_SPRITE = new ResourceLocation(Names.MODID, "blocks/red");

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        try {
            IModel model = ModelLoaderRegistry.getModel(GRAVE_MODEL);
            IBakedModel standard = event.getModelRegistry().getObject(GRAVE_MODEL_LOCATION);
            IBakedModel finalModel = new AdaptiveGraveModel(standard, model);
            event.getModelRegistry().putObject(GRAVE_MODEL_LOCATION, finalModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void stitchTextures(TextureStitchEvent.Pre pre) {
        pre.getMap().registerSprite(RED_SPRITE);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Profiler profiler = mc.profiler;
        profiler.startSection("tmg_particles");
        ParticleRenderDispatcher.dispatch();
        profiler.endSection();
    }
}