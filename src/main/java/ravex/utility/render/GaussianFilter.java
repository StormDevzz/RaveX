package ravex.utility.render;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class GaussianFilter {

    public static BufferedImage blur(BufferedImage source, int radius) {
        if (radius <= 0) return source;

        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sigma = Math.max(0.5f, radius / 2f);
        float sum = 0;

        for (int i = 0; i < size; i++) {
            int x = i - radius;
            kernel[i] = (float) Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        for (int i = 0; i < size; i++) kernel[i] /= sum;

        Kernel horizKernel = new Kernel(size, 1, kernel);
        Kernel vertKernel = new Kernel(1, size, kernel);

        ConvolveOp horizOp = new ConvolveOp(horizKernel, ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertOp = new ConvolveOp(vertKernel, ConvolveOp.EDGE_NO_OP, null);

        BufferedImage temp = horizOp.filter(source, null);
        return vertOp.filter(temp, null);
    }
}
