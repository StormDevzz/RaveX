package ravex.di;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Injector {
    private static final List<Object> pending = new ArrayList<>();

    public static <T> T inject(T object) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    try {
                        Object value = ServiceLocator.resolve(field.getType());
                        field.set(object, value);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to inject " + field.getType().getSimpleName() + " into " + object.getClass().getSimpleName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return object;
    }

    public static void injectAll() {
        List<Object> toInject = new ArrayList<>(pending);
        pending.clear();
        for (Object obj : toInject) {
            inject(obj);
        }
    }

    public static void registerPending(Object object) {
        pending.add(object);
    }
}
