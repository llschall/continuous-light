/*
 * Java Test Class
 */
package org.llschall.continuous.light;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AppJavaTest {
    @Test
    void appJavaHasAGreeting() {
        App app = new App();
        Assertions.assertFalse(app.getGreeting().isBlank());
    }
}

