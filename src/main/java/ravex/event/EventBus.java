package ravex.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<RegisteredListener>> listeners = new ConcurrentHashMap<>();
    private final Map<Object, List<SubscribedMethod>> objectListeners = new ConcurrentHashMap<>();

    public <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener) {
        subscribe(eventType, listener, EventPriority.NORMAL);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener, EventPriority priority) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(new RegisteredListener((EventListener<Event>) listener, priority));
    }

    @SuppressWarnings("unchecked")
    public void subscribe(Object object) {
        Class<?> clazz = object.getClass();
        List<SubscribedMethod> methods = new ArrayList<>();
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) continue;
            if (!Event.class.isAssignableFrom(paramTypes[0])) continue;
            Class<? extends Event> eventType = (Class<? extends Event>) paramTypes[0];
            method.setAccessible(true);

            EventListener<Event> listener = createListener(object, method, eventType);
            methods.add(new SubscribedMethod(
                object, method, eventType, annotation.priority(), listener
            ));
        }
        methods.sort(Comparator.comparingInt(m -> m.priority.ordinal()));
        for (SubscribedMethod method : methods) {
            listeners.computeIfAbsent(method.eventType, k -> new CopyOnWriteArrayList<>())
                .add(new RegisteredListener(method.listener, method.priority));
        }
        objectListeners.put(object, methods);
    }

    @SuppressWarnings("unchecked")
    private EventListener<Event> createListener(
        Object object, java.lang.reflect.Method method, Class<? extends Event> eventType
    ) {
        Class<?> rawEventType = eventType;
        return event -> {
            if (!rawEventType.isInstance(event)) return;
            try {
                method.invoke(object, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    public void unsubscribe(Object object) {
        List<SubscribedMethod> methods = objectListeners.remove(object);
        if (methods == null) return;
        for (SubscribedMethod method : methods) {
            CopyOnWriteArrayList<RegisteredListener> list = listeners.get(method.eventType);
            if (list != null) {
                list.removeIf(rl -> rl.listener == method.listener);
            }
        }
    }

    public <E extends Event> E post(E event) {
        CopyOnWriteArrayList<RegisteredListener> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return event;

        boolean cancellable = event.isCancellable();
        for (RegisteredListener rl : list) {
            if (cancellable && event.isCancelled()) break;
            try {
                rl.listener.onEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return event;
    }

    public void clear() {
        listeners.clear();
        objectListeners.clear();
    }

    private record RegisteredListener(EventListener<Event> listener, EventPriority priority) {}
    private record SubscribedMethod(
        Object object,
        java.lang.reflect.Method method,
        Class<? extends Event> eventType,
        EventPriority priority,
        EventListener<Event> listener
    ) {}
}
