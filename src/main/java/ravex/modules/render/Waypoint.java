package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import ravex.modules.Modules;

@Module(name = "Waypoint", category = "Render")
public class Waypoint {
public record WaypointData(String name, double x, double y, double z, String dimension) {
    }

    public final List<WaypointData> waypoints = new ArrayList<>();
    @Parameter(name = "Color", color = true)
    public int color = 0xFF33AAFF;
    @Parameter(name = "Size", min = 0.5, max = 5.0, step = 0.5)
    public double markerSize = 2.0;
    @Parameter(name = "Range", min = 16.0, max = 512.0, step = 16.0)
    public double range = 256.0;
    @Parameter(name = "ShowName")
    public boolean showName = true;
    @Parameter(name = "ShowDistance")
    public boolean showDistance = true;
    @Parameter(name = "Beam")
    public boolean showBeam = true;

    public static List<WaypointData> getWaypoints() {
        return Modules.get(Waypoint.class).waypoints;
    }

    public static int getColor() {
        return Modules.get(Waypoint.class).color;
    }

    public static double getMarkerSize() {
        return Modules.get(Waypoint.class).markerSize;
    }

    public static double getRange() {
        return Modules.get(Waypoint.class).range;
    }

    public static boolean isShowName() {
        return Modules.get(Waypoint.class).showName;
    }

    public static boolean isShowDistance() {
        return Modules.get(Waypoint.class).showDistance;
    }

    public static boolean isShowBeam() {
        return Modules.get(Waypoint.class).showBeam;
    }

    public void addWaypoint(String name, double x, double y, double z, String dimension) {
        waypoints.add(new WaypointData(name, x, y, z, dimension));
    }

    public boolean removeWaypoint(String name) {
        return waypoints.removeIf(w -> w.name().equalsIgnoreCase(name));
    }

    public void clearWaypoints() {
        waypoints.clear();
    }
    public void saveExtra(JsonObject obj) {
        JsonArray arr = new JsonArray();
        for (WaypointData wp : waypoints) {
            JsonObject wpObj = new JsonObject();
            wpObj.addProperty("name", wp.name());
            wpObj.addProperty("x", wp.x());
            wpObj.addProperty("y", wp.y());
            wpObj.addProperty("z", wp.z());
            wpObj.addProperty("dimension", wp.dimension());
            arr.add(wpObj);
        }
        obj.add("waypoints", arr);
    }
    public void loadExtra(JsonObject obj) {
        waypoints.clear();
        if (obj.has("waypoints")) {
            JsonArray arr = obj.getAsJsonArray("waypoints");
            for (var el : arr) {
                JsonObject wpObj = el.getAsJsonObject();
                String name = wpObj.get("name").getAsString();
                double x = wpObj.get("x").getAsDouble();
                double y = wpObj.get("y").getAsDouble();
                double z = wpObj.get("z").getAsDouble();
                String dimension = wpObj.get("dimension").getAsString();
                waypoints.add(new WaypointData(name, x, y, z, dimension));
            }
        }
    }





}