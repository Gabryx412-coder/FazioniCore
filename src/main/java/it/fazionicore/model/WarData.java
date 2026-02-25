package it.fazionicore.model;

public class WarData {

    private final String faction1;
    private final String faction2;
    private int points1;
    private int points2;
    private boolean active;
    private final long startTime;
    private long endTime;

    public WarData(String faction1, String faction2) {
        this.faction1 = faction1;
        this.faction2 = faction2;
        this.points1 = 0;
        this.points2 = 0;
        this.active = true;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
    }

    public boolean involves(String factionName) {
        return faction1.equalsIgnoreCase(factionName) || faction2.equalsIgnoreCase(factionName);
    }

    public String getOpponent(String factionName) {
        return faction1.equalsIgnoreCase(factionName) ? faction2 : faction1;
    }

    public void addPoints(String factionName, int pts) {
        if (faction1.equalsIgnoreCase(factionName)) points1 += pts;
        else points2 += pts;
    }

    public int getPoints(String factionName) {
        return faction1.equalsIgnoreCase(factionName) ? points1 : points2;
    }

    public void end() {
        this.active = false;
        this.endTime = System.currentTimeMillis();
    }

    public String getFaction1() { return faction1; }
    public String getFaction2() { return faction2; }
    public int getPoints1() { return points1; }
    public void setPoints1(int points1) { this.points1 = points1; }
    public int getPoints2() { return points2; }
    public void setPoints2(int points2) { this.points2 = points2; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
}
