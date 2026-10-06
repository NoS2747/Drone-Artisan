package fr.droneartisan.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DronePilotControlsTest {
    @Test
    void acceptsFiniteMovementAndKnownActions() {
        assertTrue(valid(0, 0.75F, -0.5F, 1));
        assertTrue(valid(3, 0.0F, 0.0F, 1));
        assertTrue(valid(5, 0.0F, 0.0F, 1));
        assertTrue(valid(6, 0.0F, 0.0F, 1));
        assertTrue(valid(3, 0.0F, 0.0F, 6));
    }

    @Test
    void rejectsInvalidMovementActionAndFace() {
        assertFalse(valid(0, Float.NaN, 0.0F, 1));
        assertFalse(valid(1, 1.5F, 0.0F, 1));
        assertFalse(valid(2, 0.0F, 0.0F, 6));
        assertFalse(valid(99, 0.0F, 0.0F, 1));
    }

    private static boolean valid(int action, float forward, float strafe, int face) {
        return DronePilotControls.isValid(forward, strafe, 0.0F, 45.0F, 0.0F, action, face);
    }
}
