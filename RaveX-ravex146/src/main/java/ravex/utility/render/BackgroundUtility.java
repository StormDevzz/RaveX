package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayDeque;
import java.util.Queue;

public class BackgroundUtility {


    public static void removeBackground(NativeImage image, int keyRed, int keyGreen, int keyBlue, int threshold) {
        if (image == null) return;
        int width = image.getWidth();
        int height = image.getHeight();

        boolean[][] visited = new boolean[width][height];
        Queue<int[]> queue = new ArrayDeque<>();


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


            int pixel = image.getPixel(cx, cy);
            int transparentPixel = pixel & 0x00FFFFFF;
            image.setPixel(cx, cy, transparentPixel);

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {


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

        int r = pixel & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = (pixel >> 16) & 0xFF;

        int dist = (int) Math.sqrt((r - keyRed) * (r - keyRed) + (g - keyGreen) * (g - keyGreen) + (b - keyBlue) * (b - keyBlue));
        return dist <= threshold;
    }
}
