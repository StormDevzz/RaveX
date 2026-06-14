package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayDeque;
import java.util.Queue;

public class BackgroundUtility {

    /**
     * Removes the background from a NativeImage by making pixels close to the key color transparent.
     * Uses a flood fill BFS starting from the 4 corners, and blocks traversal along the bottom edge
     * of the image (except near corners) to prevent entering the character's cut-off body.
     * @param image The Minecraft NativeImage to modify.
     * @param keyRed Red component (0-255)
     * @param keyGreen Green component (0-255)
     * @param keyBlue Blue component (0-255)
     * @param threshold Max distance in color space to match background (e.g. 35)
     */
    public static void removeBackground(NativeImage image, int keyRed, int keyGreen, int keyBlue, int threshold) {
        if (image == null) return;
        int width = image.getWidth();
        int height = image.getHeight();

        boolean[][] visited = new boolean[width][height];
        Queue<int[]> queue = new ArrayDeque<>();

        // Add the 4 corners of the image to the queue to start flood fill
        int[][] corners = {
            {0, 0},
            {width - 1, 0},
            {0, height - 1},
            {width - 1, height - 1}
        };

        for (int[] corner : corners) {
            int cx = corner[0];
            int cy = corner[1];
            if (isMatchingColor(image.getPixel(cx, cy), keyRed, keyGreen, keyBlue, threshold)) {
                queue.add(new int[]{cx, cy});
                visited[cx][cy] = true;
            }
        }

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int cx = curr[0];
            int cy = curr[1];

            // Make current pixel transparent
            int pixel = image.getPixel(cx, cy);
            int transparentPixel = pixel & 0x00FFFFFF;
            image.setPixel(cx, cy, transparentPixel);

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    // Block the bottom edge from being traversed (except near the corners)
                    // to prevent leaking into cut-off character bodies.
                    if (ny == height - 1 && nx >= 15 && nx < width - 15) {
                        continue;
                    }
                    if (!visited[nx][ny]) {
                        if (isMatchingColor(image.getPixel(nx, ny), keyRed, keyGreen, keyBlue, threshold)) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }
    }

    private static boolean isMatchingColor(int pixel, int keyRed, int keyGreen, int keyBlue, int threshold) {
        // NativeImage stores pixels in ABGR format: 0xAABBGGRR
        int r = pixel & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = (pixel >> 16) & 0xFF;

        int dist = (int) Math.sqrt((r - keyRed) * (r - keyRed) + (g - keyGreen) * (g - keyGreen) + (b - keyBlue) * (b - keyBlue));
        return dist <= threshold;
    }
}
