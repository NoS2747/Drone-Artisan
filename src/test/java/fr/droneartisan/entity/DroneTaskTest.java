package fr.droneartisan.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DroneTaskTest {
    @Test
    void taskCycleReturnsToFollowAfterHarvest() {
        DroneTask task = DroneTask.FOLLOW;
        for (DroneTask expected : DroneTask.values()) {
            assertEquals(expected, task);
            task = task.next();
        }
        assertEquals(DroneTask.FOLLOW, task);
    }

    @Test
    void eachTaskHasItsOwnTranslationKey() {
        for (DroneTask task : DroneTask.values()) {
            assertEquals("task.droneartisan." + task.name().toLowerCase(java.util.Locale.ROOT),
                    task.translation());
        }
    }

    @Test
    void controlPanelOffersTheRequestedOrders() {
        assertEquals(DroneTask.MINE, DroneTask.valueOf("MINE"));
        assertEquals(DroneTask.LOCATE_ORE, DroneTask.valueOf("LOCATE_ORE"));
        assertEquals(DroneTask.DEFEND, DroneTask.valueOf("DEFEND"));
        assertEquals(DroneTask.RETURN_BASE, DroneTask.valueOf("RETURN_BASE"));
    }
}
