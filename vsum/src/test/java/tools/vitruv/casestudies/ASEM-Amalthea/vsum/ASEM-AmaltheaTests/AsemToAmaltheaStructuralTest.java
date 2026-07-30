package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.PeriodicStimulus;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests Structural rules (S1-S10) where changes originate on the ASEM side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AsemToAmaltheaStructuralTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // S10 — PeriodicTask.period/delay set before any AMALTHEA stimulus exists

    @Test
    @DisplayName("S10 – period/delay set before any AMALTHEA stimulus exists → stimulus auto-created")
    void s10_periodSetBeforeStimulusExists_autoCreatesStimulus(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemPeriodicTask(vsum, tempDir, "earlyPeriodicTask");

        assertDoesNotThrow(() -> {
            util.setPeriodicTaskPeriod(vsum, "earlyPeriodicTask", 250);
            util.setPeriodicTaskDelay(vsum, "earlyPeriodicTask", 100);
        });

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "earlyPeriodicTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task);
        PeriodicStimulus stimulus = (PeriodicStimulus) task.getStimuli().stream()
                .filter(s -> s instanceof PeriodicStimulus).findFirst().orElse(null);
        assertNotNull(stimulus, "PeriodicStimulus should have been auto-created");
        assertEquals(250, stimulus.getRecurrence().getValue().intValue());
        assertEquals(100, stimulus.getOffset().getValue().intValue());
    }

    @Test
    @DisplayName("S10 – delay set alone before any stimulus exists → stimulus auto-created with default recurrence")
    void s10_delaySetAloneBeforeStimulusExists_autoCreatesStimulus(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemPeriodicTask(vsum, tempDir, "delayOnlyTask");
        util.setPeriodicTaskDelay(vsum, "delayOnlyTask", 100);

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "delayOnlyTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task);
        PeriodicStimulus stimulus = (PeriodicStimulus) task.getStimuli().stream()
                .filter(s -> s instanceof PeriodicStimulus).findFirst().orElse(null);
        assertNotNull(stimulus, "PeriodicStimulus should have been auto-created");
        assertEquals(0, stimulus.getRecurrence().getValue().intValue());
        assertEquals(100, stimulus.getOffset().getValue().intValue());
    }
}
