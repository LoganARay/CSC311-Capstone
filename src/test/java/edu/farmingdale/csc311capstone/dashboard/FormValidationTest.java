package edu.farmingdale.csc311capstone.dashboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FormValidationTest {
    @Test
    void loginRequiresBothFields() {
        assertEquals(2, FormValidation.login(" ", "").size());
        assertEquals(2, FormValidation.login(null, null).size());
    }

    @Test
    void validLoginIsOnlyLocalValidation() {
        assertTrue(FormValidation.login("guest", "test-only").isEmpty());
    }

    @Test
    void registrationRequiresConfirmation() {
        assertFalse(FormValidation.registration("player", "test-only", "").isEmpty());
    }

    @Test
    void confirmationMustMatchExactly() {
        assertFalse(FormValidation.registration("player", "test-only", "test-Only").isEmpty());
    }

    @Test
    void matchingRegistration() {
        assertTrue(FormValidation.registration("player", "test-only", "test-only").isEmpty());
    }

    @Test
    void doesNotInventBackendPasswordPolicy() {
        assertTrue(FormValidation.registration("player", "x", "x").isEmpty());
    }

    @Test
    void blankPasswordsNeverValidate() {
        assertFalse(FormValidation.registration("player", " ", " ").isEmpty());
    }
}
