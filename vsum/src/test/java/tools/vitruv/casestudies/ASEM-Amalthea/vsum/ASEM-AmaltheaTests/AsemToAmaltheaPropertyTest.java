package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Array;
import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.PeriodicStimulus;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.TimeUnit;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Constant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Method;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests Property rules (P1-P19) where changes originate on the ASEM side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AsemToAmaltheaPropertyTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // P2 — Module.name → Component.name

    @Test
    @DisplayName("P2 – Module renamed → Component name updated")
    void p2_moduleRenamed_componentNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "InitialName");
        util.renameInAsem(vsum, "InitialName", Module.class, "UpdatedName");

        assertNull(util.getCorrespondingInAmalthea(vsum, "InitialName", Component.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "UpdatedName", Component.class));
    }

    // P4 — Method.name → Runnable.name

    @Test
    @DisplayName("P4 – Method renamed → Runnable name updated")
    void p4_methodRenamed_runnableNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "MCU");
        util.addVoidMethod(vsum, "MCU", "oldOp");
        util.renameInAsem(vsum, "oldOp", Method.class, "newOp");

        assertNull(util.getCorrespondingInAmalthea(vsum, "oldOp", Runnable.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "newOp", Runnable.class));
    }

    // P6 / P7 — name propagation

    @Test
    @DisplayName("P6 – Message renamed → Label name updated")
    void p6_messageRenamed_labelNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "CAN_Node");
        util.addMessage(vsum, "CAN_Node", "oldFrame");
        util.renameInAsem(vsum, "oldFrame", Message.class, "newFrame");

        assertNull(util.getCorrespondingInAmalthea(vsum, "oldFrame", Label.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "newFrame", Label.class));
    }

    @Test
    @DisplayName("P7 – Constant renamed → Label name updated")
    void p7_constantRenamed_labelNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "LIN_Node");
        util.addConstant(vsum, "LIN_Node", "OLD_PARAM");
        util.renameInAsem(vsum, "OLD_PARAM", Constant.class, "NEW_PARAM");

        assertNull(util.getCorrespondingInAmalthea(vsum, "OLD_PARAM", Label.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "NEW_PARAM", Label.class));
    }

    // Bidirectional round-trip — P1+P2 / P3+P4 together across both directions

    @Test
    @DisplayName("Bidirectional – Component/Module names stay in sync after alternating renames")
    void bidirectional_componentModule_alternatingRenames(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Start");
        assertNotNull(util.getCorrespondingInAsem(vsum, "Start", Module.class));

        // rename from AMALTHEA side
        util.renameInAmalthea(vsum, "Start", Component.class, "Middle");
        assertNotNull(util.getCorrespondingInAsem(vsum, "Middle", Module.class),
                "Module must follow Component rename");

        // rename from ASEM side
        util.renameInAsem(vsum, "Middle", Module.class, "End");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "End", Component.class),
                "Component must follow Module rename");
    }

    @Test
    @DisplayName("Bidirectional – Runnable/Method names stay in sync after alternating renames")
    void bidirectional_runnableMethod_alternatingRenames(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "BiDir");
        util.addRunnable(vsum, "BiDir", "initial");
        assertNotNull(util.getCorrespondingInAsem(vsum, "initial", Method.class));

        util.renameInAmalthea(vsum, "initial", Runnable.class, "fromAmalthea");
        assertNotNull(util.getCorrespondingInAsem(vsum, "fromAmalthea", Method.class));

        util.renameInAsem(vsum, "fromAmalthea", Method.class, "fromAsem");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "fromAsem", Runnable.class));
    }

    // P8 / P9 — ASEM TypedElement.constant flipped → Label.constant follows, object swapped

    @Test
    @DisplayName("P8 – Message.constant flipped true → replaced by Constant, Label.constant follows")
    void p8_messageConstantFlagFlippedTrue_replacedByConstant(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "SensorModule");
        util.addMessage(vsum, "SensorModule", "reading");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "reading", Label.class));

        util.setConstantInAsem(vsum, "reading", Message.class, true);

        assertNull(util.getCorrespondingInAsem(vsum, "reading", Message.class),
                "Message must be replaced");
        assertNotNull(util.getCorrespondingInAsem(vsum, "reading", Constant.class),
                "Constant must be created");
        Label label = util.getCorrespondingInAmalthea(vsum, "reading", Label.class);
        assertNotNull(label);
        assertTrue(label.isConstant(), "Label.constant must follow the ASEM-side flip");
    }

    @Test
    @DisplayName("P9 – Constant.constant flipped false → replaced by Message, Label.constant follows")
    void p9_constantConstantFlagFlippedFalse_replacedByMessage(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ConfigModule");
        util.addConstant(vsum, "ConfigModule", "threshold");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "threshold", Label.class));

        util.setConstantInAsem(vsum, "threshold", Constant.class, false);

        assertNull(util.getCorrespondingInAsem(vsum, "threshold", Constant.class),
                "Constant must be replaced");
        assertNotNull(util.getCorrespondingInAsem(vsum, "threshold", Message.class),
                "Message must be created");
        Label label = util.getCorrespondingInAmalthea(vsum, "threshold", Label.class);
        assertNotNull(label);
        assertFalse(label.isConstant(), "Label.constant must follow the ASEM-side flip");
    }

    @Test
    @DisplayName("P8/P9 round-trip – AMALTHEA flip then ASEM flip back returns to original state")
    void p8p9_roundTrip_amaltheaThenAsemFlip_returnsToOriginal(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "RoundTrip");
        util.addLabel(vsum, "RoundTrip", "value", false);
        assertNotNull(util.getCorrespondingInAsem(vsum, "value", Message.class));

        // P8 — flip from AMALTHEA side
        util.setConstant(vsum, "value", true);
        assertNotNull(util.getCorrespondingInAsem(vsum, "value", Constant.class));

        // P9 — flip from ASEM side
        util.setConstantInAsem(vsum, "value", Constant.class, false);

        assertNull(util.getCorrespondingInAsem(vsum, "value", Constant.class),
                "Constant must be gone after flipping back");
        Message messageAgain = util.getCorrespondingInAsem(vsum, "value", Message.class);
        assertNotNull(messageAgain, "Message must exist again after round-trip");
        Label label = util.getCorrespondingInAmalthea(vsum, "value", Label.class);
        assertFalse(label.isConstant());
    }

    // P18, P19 — PeriodicTask.period/delay changed in ASEM → PeriodicStimulus updated

    @Test
    @DisplayName("P18, P19 – period/delay changed in ASEM → PeriodicStimulus updated in AMALTHEA")
    void p18p19_periodDelayChanged_stimulusUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemPeriodicTask(vsum, tempDir, "reversePeriodicTask");
        util.addPeriodicStimulus(vsum, "reversePeriodicTask", 10, 5);

        util.setPeriodicTaskPeriod(vsum, "reversePeriodicTask", 250);
        util.setPeriodicTaskDelay(vsum, "reversePeriodicTask", 100);

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reversePeriodicTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task);
        PeriodicStimulus stimulus = (PeriodicStimulus) task.getStimuli().stream()
                .filter(s -> s instanceof PeriodicStimulus).findFirst().orElse(null);
        assertNotNull(stimulus, "PeriodicStimulus must still be attached");
        assertEquals(250, stimulus.getRecurrence().getValue().intValue());
        assertEquals(TimeUnit.MS, stimulus.getRecurrence().getUnit());
        assertEquals(100, stimulus.getOffset().getValue().intValue());
        assertEquals(TimeUnit.MS, stimulus.getOffset().getUnit());
    }

    // P12 — ComposedType.numberElements → Array.numberElements

    @Test
    @DisplayName("P12 – ComposedType.numberElements copied to Array at creation")
    void p12_asemComposedTypeCreated_numberElementsCopied(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemComposedType(vsum, tempDir, "reverseComposed", 15);

        Array array = util.getCorrespondingInAmalthea(vsum, null, Array.class);
        assertNotNull(array, "Array must be created for the ASEM ComposedType");
        assertEquals(15, array.getNumberElements());
    }

    @Test
    @DisplayName("P12 – ComposedType.numberElements changed → Array.numberElements updated")
    void p12_asemComposedTypeNumberElementsChanged_arrayUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemComposedType(vsum, tempDir, "reverseComposed", 15);
        util.setComposedTypeNumberElements(vsum, "reverseComposed", 40);

        Array array = util.getCorrespondingInAmalthea(vsum, null, Array.class);
        assertNotNull(array);
        assertEquals(40, array.getNumberElements());
    }

    // P15 — ASEM Task subtype .name → AMALTHEA Task.name

    @Test
    @DisplayName("P15 – ASEM Task subtype renamed → AMALTHEA Task name updated")
    void p15_asemTaskSubtypeRenamed_taskNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSoftwareTask(vsum, tempDir, "oldSubtypeName");
        util.renameInAsem(vsum, "oldSubtypeName",
                edu.kit.ipd.sdq.metamodels.asem.classifiers.Task.class, "newSubtypeName");

        assertNull(util.getCorrespondingInAmalthea(
                vsum, "oldSubtypeName", org.eclipse.app4mc.amalthea.model.Task.class));
        assertNotNull(util.getCorrespondingInAmalthea(
                vsum, "newSubtypeName", org.eclipse.app4mc.amalthea.model.Task.class));
    }

    // P16 — ASEM InterruptTask.name → AMALTHEA ISR.name

    @Test
    @DisplayName("P16 – ASEM InterruptTask renamed → AMALTHEA ISR name updated")
    void p16_asemInterruptTaskRenamed_isrNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemInterruptTask(vsum, tempDir, "oldIsrSubtypeName");
        util.renameInAsem(vsum, "oldIsrSubtypeName",
                edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask.class, "newIsrSubtypeName");

        assertNull(util.getCorrespondingInAmalthea(
                vsum, "oldIsrSubtypeName", org.eclipse.app4mc.amalthea.model.ISR.class));
        assertNotNull(util.getCorrespondingInAmalthea(
                vsum, "newIsrSubtypeName", org.eclipse.app4mc.amalthea.model.ISR.class));
    }

    // P17 — Input/Output/SystemConstant .name → Label.name

    @Test
    @DisplayName("P17 – Input renamed → Label name updated")
    void p17_inputRenamed_labelNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "SensorModule");
        util.addInput(vsum, "SensorModule", "oldInputName");
        util.renameInAsem(vsum, "oldInputName",
                edu.kit.ipd.sdq.metamodels.asem.dataexchange.Input.class, "newInputName");

        assertNull(util.getCorrespondingInAmalthea(vsum, "oldInputName", Label.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "newInputName", Label.class));
    }

    @Test
    @DisplayName("P17 – Output renamed → Label name updated")
    void p17_outputRenamed_labelNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ActuatorModule");
        util.addOutput(vsum, "ActuatorModule", "oldOutputName");
        util.renameInAsem(vsum, "oldOutputName",
                edu.kit.ipd.sdq.metamodels.asem.dataexchange.Output.class, "newOutputName");

        assertNull(util.getCorrespondingInAmalthea(vsum, "oldOutputName", Label.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "newOutputName", Label.class));
    }

    @Test
    @DisplayName("P17 – SystemConstant renamed → Label name updated")
    void p17_systemConstantRenamed_labelNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "CalibModule");
        util.addSystemConstant(vsum, "CalibModule", "oldConstName");
        util.renameInAsem(vsum, "oldConstName",
                edu.kit.ipd.sdq.metamodels.asem.dataexchange.SystemConstant.class, "newConstName");

        assertNull(util.getCorrespondingInAmalthea(vsum, "oldConstName", Label.class));
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "newConstName", Label.class));
    }
}
