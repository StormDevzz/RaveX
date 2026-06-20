package ravex.modules.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class Waypoint extends Module {
    public static final Waypoint INSTANCE = new Waypoint();

    public record WaypointData(String name, double x, double y, double z, String dimension) {}

    public final List<WaypointData> waypoints = new ArrayList<>();

    public final ColorParameter color = new ColorParameter("Color", 0xFF33AAFF);
    public final NumberParameter markerSize = new NumberParameter("Size", 2.0, 0.5, 5.0, 0.5);
    public final NumberParameter range = new NumberParameter("Range", 256.0, 16.0, 512.0, 16.0);
    public final BooleanParameter showName = new BooleanParameter("Show Name", true);
    public final BooleanParameter showDistance = new BooleanParameter("Show Distance", true);
    public final BooleanParameter showBeam = new BooleanParameter("Beam", true);

    private Waypoint() {
        super("Waypoint", Category.RENDER);
        addParameter(color);
        addParameter(markerSize);
        addParameter(range);
        addParameter(showName);
        addParameter(showDistance);
        addParameter(showBeam);
    }

    public static List<WaypointData> getWaypoints() {
        return INSTANCE.waypoints;
    }

    public static int getColor() {
        return INSTANCE.color.getValue();
    }

    public static double getMarkerSize() {
        return INSTANCE.markerSize.getValue();
    }

    public static double getRange() {
        return INSTANCE.range.getValue();
    }

    public static boolean isShowName() {
        return INSTANCE.showName.getValue();
    }

    public static boolean isShowDistance() {
        return INSTANCE.showDistance.getValue();
    }

    public static boolean isShowBeam() {
        return INSTANCE.showBeam.getValue();
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

    @Override
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

    @Override
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
