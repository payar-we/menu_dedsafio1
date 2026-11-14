package com.hexasafio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Imagen de interfaz que se re-escala al tamano EXACTO en pixeles reales de la pantalla
 * (promediando area), asi el texto y el logo se ven nitidos con cualquier resolucion o GUI scale.
 */
final class UiTex {
    private static final Map<String, UiTex> CACHE = new HashMap<>();

    static UiTex of(String path) {
        return CACHE.computeIfAbsent(path, UiTex::new);
    }

    private final Identifier source;
    private final Identifier dyn;
    private int sw, sh;
    private int[] argb;               // imagen original (ARGB sin premultiplicar)
    private int cw = -1, ch = -1;     // tamano actual de la textura generada
    private NativeImageBackedTexture tex;
    private boolean failed;

    private UiTex(String path) {
        this.source = Identifier.of("hexasafio", path);
        this.dyn = Identifier.of("hexasafio", "dynamic/ui/" + path.replace('/', '_').replace('.', '_'));
    }

    boolean load(MinecraftClient mc) {
        if (argb != null) return true;
        if (failed) return false;
        try (InputStream in = mc.getResourceManager().getResource(source).orElseThrow().getInputStream()) {
            BufferedImage bi = ImageIO.read(in);
            sw = bi.getWidth();
            sh = bi.getHeight();
            argb = bi.getRGB(0, 0, sw, sh, null, 0, sw);
            return true;
        } catch (Exception e) {
            System.err.println("[Hexasafio] No se pudo cargar " + source + ": " + e);
            failed = true;
            return false;
        }
    }

    /** Devuelve la textura lista para dibujarse 1:1 a tw x th pixeles reales. */
    Identifier get(MinecraftClient mc, int tw, int th) {
        if (!load(mc)) return null;
        tw = Math.max(1, tw);
        th = Math.max(1, th);
        if (tw != cw || th != ch) {
            int[] px = (tw == sw && th == sh) ? argb : resample(argb, sw, sh, tw, th);
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, tw, th, false);
            for (int y = 0; y < th; y++) {
                for (int x = 0; x < tw; x++) {
                    int p = px[y * tw + x];
                    img.setColor(x, y, (p & 0xFF00FF00) | ((p & 0xFF) << 16) | ((p >> 16) & 0xFF));
                }
            }
            if (tex != null) {
                mc.getTextureManager().destroyTexture(dyn);
            }
            tex = new NativeImageBackedTexture(img);
            mc.getTextureManager().registerTexture(dyn, tex);
            tex.setFilter(true, false);
            cw = tw;
            ch = th;
        }
        return dyn;
    }

    // ---------------------------------------------------------------- re-escalado

    private static int[] resample(int[] src, int sw, int sh, int tw, int th) {
        float[] p = new float[sw * sh * 4];
        for (int i = 0; i < sw * sh; i++) {
            int c = src[i];
            float a = (c >>> 24) / 255f;
            p[i * 4] = ((c >> 16) & 255) * a;
            p[i * 4 + 1] = ((c >> 8) & 255) * a;
            p[i * 4 + 2] = (c & 255) * a;
            p[i * 4 + 3] = a * 255f;
        }
        float[] tmp = axis(p, sw, sh, tw, true);
        float[] out = axis(tmp, tw, sh, th, false);
        int[] res = new int[tw * th];
        for (int i = 0; i < tw * th; i++) {
            float al = out[i * 4 + 3];
            int a = Math.min(255, Math.max(0, Math.round(al)));
            int r = 0, g = 0, b = 0;
            if (al > 0.5f) {
                float inv = 255f / al;
                r = clamp(out[i * 4] * inv);
                g = clamp(out[i * 4 + 1] * inv);
                b = clamp(out[i * 4 + 2] * inv);
            }
            res[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return res;
    }

    private static int clamp(float v) {
        return Math.min(255, Math.max(0, Math.round(v)));
    }

    /** Re-escala a lo largo de un eje: promedio de area al reducir, bilineal al ampliar. */
    private static float[] axis(float[] in, int w, int h, int nt, boolean horiz) {
        int ns = horiz ? w : h;
        int ow = horiz ? nt : w;
        int oh = horiz ? h : nt;
        float[] out = new float[ow * oh * 4];
        float scale = (float) ns / nt;
        for (int d = 0; d < nt; d++) {
            int[] idx;
            float[] wt;
            if (scale > 1f) {
                float a = d * scale, b = (d + 1) * scale;
                int s0 = (int) Math.floor(a);
                int s1 = Math.min(ns, (int) Math.ceil(b));
                int cnt = Math.max(1, s1 - s0);
                idx = new int[cnt];
                wt = new float[cnt];
                float sum = 0;
                for (int i = 0; i < cnt; i++) {
                    idx[i] = Math.min(ns - 1, s0 + i);
                    float lo = Math.max(a, s0 + i), hi = Math.min(b, s0 + i + 1);
                    wt[i] = Math.max(0f, hi - lo);
                    sum += wt[i];
                }
                if (sum <= 0f) { wt[0] = 1f; sum = 1f; }
                for (int i = 0; i < cnt; i++) wt[i] /= sum;
            } else {
                float pos = (d + 0.5f) * scale - 0.5f;
                int i0 = (int) Math.floor(pos);
                float fr = pos - i0;
                idx = new int[] { Math.max(0, Math.min(ns - 1, i0)), Math.max(0, Math.min(ns - 1, i0 + 1)) };
                wt = new float[] { 1f - fr, fr };
            }
            int lines = horiz ? h : w;
            for (int l = 0; l < lines; l++) {
                float r = 0, g = 0, b = 0, a = 0;
                for (int k = 0; k < idx.length; k++) {
                    int o = (horiz ? (l * w + idx[k]) : (idx[k] * w + l)) * 4;
                    r += in[o] * wt[k];
                    g += in[o + 1] * wt[k];
                    b += in[o + 2] * wt[k];
                    a += in[o + 3] * wt[k];
                }
                int oo = (horiz ? (l * nt + d) : (d * w + l)) * 4;
                out[oo] = r;
                out[oo + 1] = g;
                out[oo + 2] = b;
                out[oo + 3] = a;
            }
        }
        return out;
    }
}
