package ravex.addon.util;

public class AddonVersion implements Comparable<AddonVersion> {
    private final int major;
    private final int minor;
    private final int patch;

    public AddonVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static AddonVersion parse(String versionStr) {
        try {
            String[] parts = versionStr.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new AddonVersion(major, minor, patch);
        } catch (Exception e) {
            return new AddonVersion(1, 0, 0);
        }
    }

    @Override
    public int compareTo(AddonVersion o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
