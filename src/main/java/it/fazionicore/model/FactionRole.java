package it.fazionicore.model;

public enum FactionRole {
    RECRUIT(0, "Recluta"),
    MEMBER(1, "Membro"),
    OFFICER(2, "Ufficiale"),
    LEADER(3, "Leader");

    private final int power;
    private final String display;

    FactionRole(int power, String display) {
        this.power = power;
        this.display = display;
    }

    public int getPower() { return power; }
    public String getDisplay() { return display; }

    public boolean isAtLeast(FactionRole role) {
        return this.power >= role.power;
    }
}
