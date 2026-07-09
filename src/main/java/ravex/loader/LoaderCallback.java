package ravex.loader;

public interface LoaderCallback {
    void updateStatus(String text, int progress);
    void setSystemScore(int score);
    void setSystemInfo(String info);
    void setExtraInfo(String info);
    void setError(String msg);
    void dispose();
}
