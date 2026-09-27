package ravex.utility.render;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class GifUtility {
    public static final class Clip {
        public final int width;
        public final int height;
        public final int[][] frames;
        public final int[] delaysMs;

        Clip(int width, int height, int[][] frames, int[] delaysMs) {
            this.width = width;
            this.height = height;
            this.frames = frames;
            this.delaysMs = delaysMs;
        }
    }

    private GifUtility() {}

    public static Clip load(InputStream stream) throws Exception {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(stream)) {
            reader.setInput(iis, false);
            int count = reader.getNumImages(true);
            if (count <= 0) throw new IllegalStateException("empty gif");

            int w = reader.getWidth(0);
            int h = reader.getHeight(0);
            BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, w, h);
            List<int[]> frames = new ArrayList<>(count);
            List<Integer> delays = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                BufferedImage frame = reader.read(i);
                int[] meta = readMeta(reader.getImageMetadata(i));
                int left = meta[0];
                int top = meta[1];
                int delayMs = meta[2];
                int disposal = meta[3];

                BufferedImage backup = null;
                if (disposal == 3) {
                    backup = copy(canvas);
                }

                g.drawImage(frame, left, top, null);
                frames.add(canvas.getRGB(0, 0, w, h, null, 0, w));
                delays.add(delayMs);

                if (disposal == 2) {
                    g.clearRect(left, top, frame.getWidth(), frame.getHeight());
                    g.setColor(java.awt.Color.BLACK);
                    g.fillRect(left, top, frame.getWidth(), frame.getHeight());
                } else if (disposal == 3 && backup != null) {
                    g.drawImage(backup, 0, 0, null);
                }
            }
            g.dispose();
            return new Clip(w, h, frames.toArray(new int[0][]), toIntArray(delays));
        } finally {
            reader.dispose();
        }
    }

    private static int[] readMeta(IIOMetadata metadata) {
        int left = 0;
        int top = 0;
        int delayMs = 100;
        int disposal = 0;
        try {
            Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                String name = node.getNodeName();
                if ("ImageDescriptor".equals(name)) {
                    left = attrInt(node, "imageLeftPosition", 0);
                    top = attrInt(node, "imageTopPosition", 0);
                } else if ("GraphicControlExtension".equals(name)) {
                    delayMs = attrInt(node, "delayTime", 10) * 10;
                    disposal = disposalCode(attrStr(node, "disposalMethod", "none"));
                }
            }
        } catch (Exception ignored) {}
        if (delayMs < 20) delayMs = 100;
        if (delayMs > 3000) delayMs = 200;
        return new int[]{left, top, delayMs, disposal};
    }

    private static int attrInt(Node node, String name, int fallback) {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) return fallback;
        try {
            return Integer.parseInt(attr.getNodeValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String attrStr(Node node, String name, String fallback) {
        Node attr = node.getAttributes().getNamedItem(name);
        return attr == null ? fallback : attr.getNodeValue();
    }

    private static int disposalCode(String method) {
        if ("restoreToBackgroundColor".equals(method)) return 2;
        if ("restoreToPrevious".equals(method)) return 3;
        return 0;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return out;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
