package ravex.di;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ServiceLocator {
    private static final Map<Class<?>, Object> services = new HashMap<>();
    private static final Map<Class<?>, Supplier<?>> factories = new HashMap<>();

    private ServiceLocator() {}

    public static <T> void register(Class<T> type, T instance) {
        services.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T> T resolve(Class<T> type) {
        T instance = (T) services.get(type);
        if (instance != null) return instance;
        Supplier<?> factory = factories.get(type);
        if (factory != null) {
            instance = (T) factory.get();
            services.put(type, instance);
            return instance;
        }
        throw new IllegalStateException("No service registered for " + type.getName());
    }

    public static <T> T resolveOrNull(Class<T> type) {
        try {
            return resolve(type);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static <T> boolean isRegistered(Class<T> type) {
        return services.containsKey(type) || factories.containsKey(type);
    }

    public static void clear() {
        services.clear();
        factories.clear();
    }
}
