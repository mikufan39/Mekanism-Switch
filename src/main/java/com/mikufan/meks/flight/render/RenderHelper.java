package com.mikufan.meks.flight.render;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.function.BiConsumer;

public class RenderHelper {
    public static BiConsumer<Integer, Integer> blankPixel(PoseStack matrices) {
        return (x, y) -> {
            int color = 0xffffffff;
            var matrix = matrices.last().pose();
            var bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            //TODO: Figure out if I can just delete the nexts here?
            bufferBuilder.addVertex(matrix, (float) x, (float) y + 1, 0.0F).setColor(color);
            bufferBuilder.addVertex(matrix, (float) x + 1, (float) y + 1, 0.0F).setColor(color);
            bufferBuilder.addVertex(matrix, (float) x + 1, (float) y, 0.0F).setColor(color);
            bufferBuilder.addVertex(matrix, (float) x, (float) y, 0.0F).setColor(color);
            BufferUploader.drawWithShader(bufferBuilder.build());
        };
    }
}