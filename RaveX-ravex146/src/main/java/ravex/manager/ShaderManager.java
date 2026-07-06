package ravex.manager;

public class ShaderManager {
    public static final ShaderManager INSTANCE = new ShaderManager();

    public boolean renderingPlayer = false;
    public boolean renderingHand = false;

    private final org.joml.Matrix4f projectionMatrix = new org.joml.Matrix4f();

    public void setProjectionMatrix(org.joml.Matrix4f matrix) {
        this.projectionMatrix.set(matrix);
    }

    public org.joml.Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
