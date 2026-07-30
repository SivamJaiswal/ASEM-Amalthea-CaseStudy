package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.PeriodicTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Task;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Input;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Output;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests Structural rules (S1-S10) where changes originate on the AMALTHEA side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AmaltheaToAsemStructuralTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // S9 — LabelAccess inserted → retroactive Message ↔ Input/Output swap

    @Test
    @DisplayName("S9 – Label with only read accesses → Input")
    void s9_readOnlyLabel_inputCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "SensorHub");
        util.addRunnable(vsum, "SensorHub", "reader");
        util.addLabel(vsum, "SensorHub", "rawSignal", false);
        util.addLabelAccess(vsum, "reader", "rawSignal", true);

        Input input = util.getCorrespondingInAsem(vsum, "rawSignal", Input.class);
        assertNotNull(input, "read-only Label must become an Input");
        assertNull(util.getCorrespondingInAsem(vsum, "rawSignal", Message.class),
                "Message counterpart must be swapped out once access pattern is known");
    }

    @Test
    @DisplayName("S9 – Label with only write accesses → Output")
    void s9_writeOnlyLabel_outputCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "ActuatorHub");
        util.addRunnable(vsum, "ActuatorHub", "writer");
        util.addLabel(vsum, "ActuatorHub", "commandSignal", false);
        util.addLabelAccess(vsum, "writer", "commandSignal", false);

        Output output = util.getCorrespondingInAsem(vsum, "commandSignal", Output.class);
        assertNotNull(output, "write-only Label must become an Output");
        assertNull(util.getCorrespondingInAsem(vsum, "commandSignal", Message.class),
                "Message counterpart must be swapped out once access pattern is known");
    }

    // S10 — PeriodicStimulus inserted into Task.stimuli → sync PeriodicTask.period/delay

    @Test
    @DisplayName("S10 – PeriodicStimulus attached (ms unit) → period/delay copied directly")
    void s10_stimulusInMillis_periodDelayCopied(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "tickTask", "PeriodicTask");
        util.addPeriodicStimulus(vsum, "tickTask", 10, 5);

        PeriodicTask periodicTask = util.getCorrespondingInAsem(vsum, "tickTask", PeriodicTask.class);
        assertNotNull(periodicTask);
        assertEquals(10, periodicTask.getPeriod());
        assertEquals(5, periodicTask.getDelay());
    }

    @Test
    @DisplayName("S10 – PeriodicStimulus in seconds → period/delay converted to milliseconds")
    void s10_stimulusInSeconds_convertedToMillis(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "slowTask", "PeriodicTask");
        util.addPeriodicStimulus(vsum, "slowTask",
                2, org.eclipse.app4mc.amalthea.model.TimeUnit.S,
                500, org.eclipse.app4mc.amalthea.model.TimeUnit.MS);

        PeriodicTask periodicTask = util.getCorrespondingInAsem(vsum, "slowTask", PeriodicTask.class);
        assertNotNull(periodicTask);
        assertEquals(2000, periodicTask.getPeriod(), "2 seconds must convert to 2000 milliseconds");
        assertEquals(500, periodicTask.getDelay());
    }

    // Rule 2 note — Task.activityGraph RunnableCalls → Task.processes[] 

    @Test
    @DisplayName("RunnableCall in Task's activityGraph does not crash propagation")
    void runnableCall_doesNotCrashPropagation(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addRunnable(vsum, "Scheduler", "doWork");
        util.addTask(vsum, "Scheduler", "mainTask");

        assertDoesNotThrow(() -> util.addRunnableCall(vsum, "mainTask", "doWork"));
    }

    @Test
    @DisplayName("Flat RunnableCall strictly populates Task.processes")
    void runnableCall_flat_strictlyPopulatesProcesses(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addRunnable(vsum, "Scheduler", "doWork");
        util.addTask(vsum, "Scheduler", "mainTask");
        util.addRunnableCall(vsum, "mainTask", "doWork");

        Task task = util.getCorrespondingInAsem(vsum, "mainTask", Task.class);
        assertNotNull(task, "ASEM Task must exist");
        assertTrue(task.getProcesses().stream().anyMatch(m -> "doWork".equals(m.getName())),
                "Task.processes must contain the Method for the directly-called Runnable");
    }

    @Test
    @DisplayName("RunnableCall nested inside a ProbabilitySwitch still populates Task.processes")
    void runnableCall_nestedInProbabilitySwitch_populatesProcesses(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addRunnable(vsum, "Scheduler", "conditionalWork");
        util.addTask(vsum, "Scheduler", "branchingTask");
        util.addNestedRunnableCall(vsum, "branchingTask", "conditionalWork");

        Task task = util.getCorrespondingInAsem(vsum, "branchingTask", Task.class);
        assertNotNull(task, "ASEM Task must exist");
        assertTrue(task.getProcesses().stream().anyMatch(m -> "conditionalWork".equals(m.getName())),
                "Task.processes must contain the Method even when the RunnableCall is nested "
                        + "inside a ProbabilitySwitch entry, not directly on the activityGraph");
    }
}
