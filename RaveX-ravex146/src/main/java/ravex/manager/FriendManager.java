package ravex.manager;

import java.util.HashSet;
import java.util.Set;

public class FriendManager {
    public static final FriendManager INSTANCE = new FriendManager();
    private final Set<String> friends = new HashSet<>();

    private FriendManager() {}

    public boolean isFriend(String name) {
        if (name == null) return false;
        return friends.contains(name.toLowerCase());
    }

    public void addFriend(String name) {
        if (name == null || name.isEmpty()) return;
        friends.add(name.toLowerCase());
    }

    public void removeFriend(String name) {
        if (name == null || name.isEmpty()) return;
        friends.remove(name.toLowerCase());
    }

    public Set<String> getFriends() {
        return friends;
    }
}
