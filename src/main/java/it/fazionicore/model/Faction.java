package it.fazionicore.model;

import java.time.Instant;
import java.util.*;

public class Faction {

    private final String name;
    private String tag;
    private String description;
    private UUID leader;
    private final Map<UUID, FactionRole> members = new HashMap<>();
    private final Set<String> pendingInvites = new HashSet<>();
    private double bank;
    private boolean friendlyFire;
    private boolean open;
    private int warPoints;
    private int level;
    private String shieldStart;
    private String shieldEnd;
    private boolean shieldEnabled;
    private final long createdAt;
    private int totalClaimsEver;

    public Faction(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag;
        this.description = "";
        this.leader = leader;
        this.bank = 0.0;
        this.friendlyFire = false;
        this.open = false;
        this.warPoints = 0;
        this.level = 1;
        this.shieldStart = "22:00";
        this.shieldEnd = "10:00";
        this.shieldEnabled = false;
        this.createdAt = Instant.now().getEpochSecond();
        this.members.put(leader, FactionRole.LEADER);
    }

    public void recalculateLevel(Map<Integer, Integer> thresholds, double memberMult) {
        int score = members.size() * (int) memberMult + totalClaimsEver;
        int newLevel = 1;
        for (Map.Entry<Integer, Integer> entry : new TreeMap<>(thresholds).entrySet()) {
            if (score >= entry.getValue()) newLevel = entry.getKey();
        }
        this.level = newLevel;
    }

    public FactionRole getRole(UUID uuid) {
        return members.getOrDefault(uuid, null);
    }

    public boolean hasMember(UUID uuid) { return members.containsKey(uuid); }

    public void addMember(UUID uuid, FactionRole role) { members.put(uuid, role); }

    public void removeMember(UUID uuid) { members.remove(uuid); }

    public void setRole(UUID uuid, FactionRole role) { members.put(uuid, role); }

    public void addInvite(UUID uuid) { pendingInvites.add(uuid.toString()); }

    public void removeInvite(UUID uuid) { pendingInvites.remove(uuid.toString()); }

    public boolean hasInvite(UUID uuid) { return pendingInvites.contains(uuid.toString()); }

    public boolean depositBank(double amount) {
        if (amount <= 0) return false;
        bank += amount;
        return true;
    }

    public boolean withdrawBank(double amount) {
        if (amount <= 0 || bank < amount) return false;
        bank -= amount;
        return true;
    }

    public String getName() { return name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) {
        this.leader = leader;
        members.put(leader, FactionRole.LEADER);
    }
    public Map<UUID, FactionRole> getMembers() { return Collections.unmodifiableMap(members); }
    public Set<String> getPendingInvites() { return pendingInvites; }
    public double getBank() { return bank; }
    public void setBank(double bank) { this.bank = bank; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public int getWarPoints() { return warPoints; }
    public void addWarPoints(int points) { this.warPoints += points; }
    public void setWarPoints(int warPoints) { this.warPoints = warPoints; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getShieldStart() { return shieldStart; }
    public void setShieldStart(String shieldStart) { this.shieldStart = shieldStart; }
    public String getShieldEnd() { return shieldEnd; }
    public void setShieldEnd(String shieldEnd) { this.shieldEnd = shieldEnd; }
    public boolean isShieldEnabled() { return shieldEnabled; }
    public void setShieldEnabled(boolean shieldEnabled) { this.shieldEnabled = shieldEnabled; }
    public long getCreatedAt() { return createdAt; }
    public int getTotalClaimsEver() { return totalClaimsEver; }
    public void incrementClaimsEver() { this.totalClaimsEver++; }
    public void setTotalClaimsEver(int totalClaimsEver) { this.totalClaimsEver = totalClaimsEver; }
}
