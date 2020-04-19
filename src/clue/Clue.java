package clue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Clue {

    private Set<String> members;

    private Set<String> banned;

    private Set<String> moderators;

    public Clue() {
        this.members = ConcurrentHashMap.newKeySet();
        this.banned = ConcurrentHashMap.newKeySet();
        this.moderators = ConcurrentHashMap.newKeySet();
    }

    public void join(String username) {
        this.members.add(username);
    }

    public void leave(String username) {
        this.members.remove(username);
    }

    public boolean isMember(String username) {
        return this.members.contains(username);
    }

    public void ban(String username) {
        this.banned.add(username);
    }

    public void unban(String username) {
        this.banned.remove(username);
    }

    public boolean isBanned(String username) {
        return this.banned.contains(username);
    }

    public void mod(String username) {
        this.moderators.add(username);
    }

    public void unmod(String username) {
        this.moderators.remove(username);
    }

    public boolean isMod(String username) {
        return this.moderators.contains(username);
    }

    public Set<String> getMembers() {
        return new HashSet<String>(this.members);
    }

    public Set<String> getModerators() {
        return new HashSet<String>(this.moderators);
    }

    public Set<String> getBanned() {
        return new HashSet<String>(this.banned);
    }
}
