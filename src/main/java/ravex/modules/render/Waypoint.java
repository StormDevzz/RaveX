package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Waypoint", category = "Render")
public class Waypoint extends ravex.modules.Module {
public record WaypointData(String name, double x, double y, double z, String dimension) {
    }

    public final List<WaypointData> waypoints = new ArrayList<>();
    public final ColorParameter color = new ColorParameter("Color", 0xFF33AAFF);
    public final NumberParameter markerSize = new NumberParameter("Size", 2.0, 0.5, 5.0, 0.5);
    public final NumberParameter range = new NumberParameter("Range", 256.0, 16.0, 512.0, 16.0);
    public final BooleanParameter showName = new BooleanParameter("ShowName", true);
    public final BooleanParameter showDistance = new BooleanParameter("ShowDistance", true);
    public final BooleanParameter showBeam = new BooleanParameter("Beam", true);

    public static List<WaypointData> getWaypoints() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).waypoints;
    }

    public static int getColor() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).color.getValue();
    }

    public static double getMarkerSize() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).markerSize.getValue();
    }

    public static double getRange() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).range.getValue();
    }

    public static boolean isShowName() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).showName.getValue();
    }

    public static boolean isShowDistance() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).showDistance.getValue();
    }

    public static boolean isShowBeam() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class).showBeam.getValue();
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Waypoint").getEnabled();
    }

    public static Waypoint itz() {
        return ravex.manager.ModuleManager.delegate(Waypoint.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}