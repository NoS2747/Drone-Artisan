package fr.droneartisan.entity;

public enum DroneTask {
    FOLLOW,
    STAY,
    COLLECT,
    HARVEST,
    RETURN_BASE,
    MINE,
    LOCATE_ORE,
    DEFEND;

    public DroneTask next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translation() {
        return "task.droneartisan." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
