package moe.quoex.seedray1218.client.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.Vec3;

public final class RenderUtils {

    private RenderUtils() {
    }

    /**
     * Draws a 1x1x1 block outline at the given world position.
     * Coordinates are translated relative to the camera, which the world render
     * event pipeline expects.
     */
    public static void drawBlockOutline(PoseStack matrices, VertexConsumer buffer, Vec3 camera,
                                        double x, double y, double z,
                                        float red, float green, float blue, float alpha) {
        ShapeRenderer.renderLineBox(matrices, buffer,
                x - camera.x, y - camera.y, z - camera.z,
                x + 1.0D - camera.x, y + 1.0D - camera.y, z + 1.0D - camera.z,
                red, green, blue, alpha);
    }
}
