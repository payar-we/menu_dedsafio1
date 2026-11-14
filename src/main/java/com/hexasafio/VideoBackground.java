package com.hexasafio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Reproduce el video (guardado como fotogramas JPG dentro del mod) en bucle.
 * Los fotogramas se decodifican en un hilo aparte para no congelar el juego.
 */
public final class VideoBackground implements AutoCloseable {

    public static final int FRAME_COUNT = 37;   // cuadros en assets/hexasafio/frames
    public static final int FPS = 12;           // velocidad del video original

    private static final Identifier TEXTURE_ID = Identifier.of("hexasafio", "dynamic/video");

    private final Object lock = new Object();
    private final NativeImage[] frames = new NativeImage[FRAME_COUNT];
    private volatile int loaded = 0;
    private volatile boolean closed = false;

    private NativeImageBackedTexture texture;
    private int shownFrame = -1;
    private long startMs = -1;

    public void start(MinecraftClient client) {
        Thread t = new Thread(() -> {
            Identifier pack = Identifier.of("hexasafio", "video/frames.bin");
            try (InputStream raw = client.getResourceManager().getResource(pack).orElseThrow().getInputStream();
                 DataInputStream in = new DataInputStream(new BufferedInputStream(raw, 1 << 16))) {
                int count = Math.min(in.readInt(), FRAME_COUNT);
                for (int i = 0; i < count && !closed; i++) {
                    byte[] jpg = new byte[in.readInt()];
                    in.readFully(jpg);
                    BufferedImage bi = ImageIO.read(new ByteArrayInputStream(jpg));
                    NativeImage img = toNative(bi);
                    synchronized (lock) {
                        if (closed) {
                            img.close();
                            return;
                        }
                        frames[i] = img;
                        loaded = i + 1;
                    }
                }
            } catch (Exception e) {
                System.err.println("[Hexasafio] No se pudo cargar el video: " + e);
            }
        }, "Hexasafio-Video-Loader");
        t.setDaemon(true);
        t.start();
    }

    private static NativeImage toNative(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        int[] argb = bi.getRGB(0, 0, w, h, null, 0, w);
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = argb[y * w + x];
                int abgr = 0xFF000000 | ((p & 0xFF) << 16) | (p & 0xFF00) | ((p >> 16) & 0xFF);
                img.setColor(x, y, abgr);
            }
        }
        return img;
    }

    /** Dibuja el video cubriendo toda la pantalla (como "background-size: cover"). */
    public void draw(DrawContext ctx, MinecraftClient client, int screenW, int screenH, long nowMs) {
        int ready = loaded;
        if (ready <= 0) {
            ctx.fill(0, 0, screenW, screenH, 0xFF05030F);
            return;
        }
        if (startMs < 0) startMs = nowMs;

        int wanted = (int) (((nowMs - startMs) * FPS / 1000L) % FRAME_COUNT);
        int idx = Math.min(wanted, ready - 1);

        int tw, th;
        synchronized (lock) {
            if (closed) return;
            NativeImage src = frames[idx];
            if (src == null) return;
            tw = src.getWidth();
            th = src.getHeight();
            if (texture == null) {
                texture = new NativeImageBackedTexture(new NativeImage(NativeImage.Format.RGBA, src.getWidth(), src.getHeight(), false));
                client.getTextureManager().registerTexture(TEXTURE_ID, texture);
                texture.setFilter(true, false);
            }
            if (idx != shownFrame) {
                NativeImage dst = texture.getImage();
                if (dst != null) {
                    dst.copyFrom(src);
                    texture.upload();
                }
                shownFrame = idx;
            }
        }

        double texAspect = (double) tw / th;
        double scrAspect = (double) screenW / screenH;
        int rw, rh, u = 0, v = 0;
        if (scrAspect > texAspect) {      // pantalla mas ancha: recortar arriba/abajo
            rw = tw;
            rh = (int) Math.round(tw / scrAspect);
            v = (th - rh) / 2;
        } else {                          // pantalla mas alta: recortar a los lados
            rh = th;
            rw = (int) Math.round(th * scrAspect);
            u = (tw - rw) / 2;
        }
        ctx.drawTexture(TEXTURE_ID, 0, 0, screenW, screenH, u, v, rw, rh, tw, th);
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            for (int i = 0; i < frames.length; i++) {
                if (frames[i] != null) {
                    frames[i].close();
                    frames[i] = null;
                }
            }
            if (texture != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE_ID);
                texture = null;
            }
        }
    }
}
