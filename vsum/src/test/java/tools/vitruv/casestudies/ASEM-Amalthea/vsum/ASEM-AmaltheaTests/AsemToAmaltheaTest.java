package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Array;
import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;
import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.ISR;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.PeriodicStimulus;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.TimeUnit;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.ComposedType;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.InitTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.SoftwareTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.PeriodicTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.TimeTableTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Task;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Constant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Method;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.BooleanType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.ContinuousType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.SignedDiscreteType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.UnsignedDiscreteType;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * AsemToAmaltheaTest
 *
 * Tests all E and P rules where changes originate on the ASEM side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AsemToAmaltheaTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // E3 / E4 — Module ↔ Component

    @Test
    @DisplayName("E3 – Module created → Component with same name in AMALTHEA view")
    void e3_moduleCreated_componentCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "FuelPump");

        Component comp = util.getCorrespondingInAmalthea(vsum, "FuelPump", Component.class);
        assertNotNull(comp, "Component must be created for the new Module");
        assertEquals("FuelPump", comp.getName());
    }

    @Test
    @DisplayName("E4 – Module deleted → Component removed from AMALTHEA view")
    void e4_moduleDeleted_componentRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "OilPressure");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "OilPressure", Component.class));

        util.deleteFromAsem(vsum, "OilPressure", Module.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "OilPressure", Component.class),
                "Component must be removed");
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

    // E7 / E8 — Method ↔ Runnable

    @Test
    @DisplayName("E7 – Void no-param Method → Runnable in Component.runnables")
    void e7_voidMethodCreated_runnableCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "Throttle");
        util.addVoidMethod(vsum, "Throttle", "openValve");

        Runnable runnable = util.getCorrespondingInAmalthea(vsum, "openValve", Runnable.class);
        Component comp    = util.getCorrespondingInAmalthea(vsum, "Throttle", Component.class);

        assertNotNull(runnable, "Runnable must be created for void Method");
        assertEquals("openValve", runnable.getName());
        assertTrue(comp.getRunnables().stream()
                .anyMatch(r -> runnable.getName().equals(r.getName())));
    }

    @Test
    @DisplayName("E7 – Method with returnType is NOT propagated (Rule 4 OCL guard)")
    void e7_methodWithReturnType_notPropagated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ComputeUnit");
        util.addVoidMethodWithReturnType(vsum, "ComputeUnit", "compute");

        assertNull(util.getCorrespondingInAmalthea(vsum, "compute", Runnable.class),
                "Method with returnType must NOT produce a Runnable");
    }

    @Test
    @DisplayName("E8 – Method deleted → Runnable removed")
    void e8_methodDeleted_runnableRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "Clutch");
        util.addVoidMethod(vsum, "Clutch", "disengage");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "disengage", Runnable.class));

        util.deleteFromAsem(vsum, "disengage", Method.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "disengage", Runnable.class),
                "Runnable must be removed");
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

    // E12 — Message → Label (constant=false)

    @Test
    @DisplayName("E12 – Message created → non-constant Label in Component.labels")
    void e12_messageCreated_nonConstantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "PressureSensor");
        util.addMessage(vsum, "PressureSensor", "oilPressure");

        Label label    = util.getCorrespondingInAmalthea(vsum, "oilPressure", Label.class);
        Component comp = util.getCorrespondingInAmalthea(vsum, "PressureSensor", Component.class);

        assertNotNull(label, "Label must be created for Message");
        assertEquals("oilPressure", label.getName());
        assertFalse(label.isConstant(), "Label.constant must be false (Rule 6)");
        assertTrue(comp.getLabels().stream()
                .anyMatch(l -> label.getName().equals(l.getName())));
    }

    // E13 — Constant → Label (constant=true)

    @Test
    @DisplayName("E13 – Constant created → constant Label in Component.labels")
    void e13_constantCreated_constantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ThrottleConfig");
        util.addConstant(vsum, "ThrottleConfig", "MAX_THROTTLE");

        Label label    = util.getCorrespondingInAmalthea(vsum, "MAX_THROTTLE", Label.class);
        Component comp = util.getCorrespondingInAmalthea(vsum, "ThrottleConfig", Component.class);

        assertNotNull(label, "Label must be created for Constant");
        assertEquals("MAX_THROTTLE", label.getName());
        assertTrue(label.isConstant(), "Label.constant must be true (Rule 5)");
        assertTrue(comp.getLabels().stream()
                .anyMatch(l -> label.getName().equals(l.getName())));
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

    // Bidirectional round-trip

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

    // R2 / R3 (reverse) — ASEM Task/InterruptTask → AMALTHEA Task/ISR

    @Test
    @DisplayName("R2 (reverse) – ASEM Task created → AMALTHEA Task exists")
    void r2_asemTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemTask(vsum, tempDir, "reverseTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM Task");
        assertEquals("reverseTask", task.getName());
    }

    @Test
    @DisplayName("R3 (reverse) – ASEM InterruptTask created → AMALTHEA ISR exists")
    void r3_asemInterruptTaskCreated_isrCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemInterruptTask(vsum, tempDir, "reverseISR");

        ISR isr = util.getCorrespondingInAmalthea(vsum, "reverseISR", ISR.class);
        assertNotNull(isr, "AMALTHEA ISR must be created for the ASEM InterruptTask");
        assertEquals("reverseISR", isr.getName());
    }

    @Test
    @DisplayName("R2 (reverse) – ASEM InitTask created → AMALTHEA Task exists")
    void r2_asemInitTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemInitTask(vsum, tempDir, "reverseInitTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseInitTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM InitTask");
        assertEquals("reverseInitTask", task.getName());
    }

    @Test
    @DisplayName("R2 (reverse) – ASEM SoftwareTask created → AMALTHEA Task exists")
    void r2_asemSoftwareTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSoftwareTask(vsum, tempDir, "reverseSoftwareTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseSoftwareTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM SoftwareTask");
        assertEquals("reverseSoftwareTask", task.getName());
    }

    @Test
    @DisplayName("R2 (reverse) – ASEM PeriodicTask created → AMALTHEA Task exists")
    void r2_asemPeriodicTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemPeriodicTask(vsum, tempDir, "reversePeriodicTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reversePeriodicTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM PeriodicTask");
        assertEquals("reversePeriodicTask", task.getName());
    }

    @Test
    @DisplayName("PeriodicTask fields (reverse) – period/delay changed in ASEM → PeriodicStimulus updated in AMALTHEA")
    void periodicTaskFields_periodDelayChanged_stimulusUpdated(@TempDir Path tempDir) throws Exception {
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

    @Test
    @DisplayName("PeriodicTask fields (reverse) – period/delay set before any AMALTHEA stimulus exists → no stimulus auto-created")
    void periodicTaskFields_periodSetBeforeStimulusExists_noAutoCreate(@TempDir Path tempDir) throws Exception {
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
        boolean hasStimulus = task.getStimuli().stream().anyMatch(s -> s instanceof PeriodicStimulus);
        assertFalse(hasStimulus, "no PeriodicStimulus should have been auto-created");
    }

    @Test
    @DisplayName("R2 (reverse) – ASEM TimeTableTask created → AMALTHEA Task exists")
    void r2_asemTimeTableTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemTimeTableTask(vsum, tempDir, "reverseTimeTableTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseTimeTableTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM TimeTableTask");
        assertEquals("reverseTimeTableTask", task.getName());
    }

    // R5 / R6 (reverse) — Input/Output/SystemConstant → Label

    @Test
    @DisplayName("R6 (reverse) – Input created → non-constant Label")
    void r6_inputCreated_nonConstantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "SensorModule");
        util.addInput(vsum, "SensorModule", "rawInput");

        Label label = util.getCorrespondingInAmalthea(vsum, "rawInput", Label.class);
        assertNotNull(label, "Label must be created for Input");
        assertFalse(label.isConstant(), "Label.constant must be false");
    }

    @Test
    @DisplayName("R6 (reverse) – Output created → non-constant Label")
    void r6_outputCreated_nonConstantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ActuatorModule");
        util.addOutput(vsum, "ActuatorModule", "rawOutput");

        Label label = util.getCorrespondingInAmalthea(vsum, "rawOutput", Label.class);
        assertNotNull(label, "Label must be created for Output");
        assertFalse(label.isConstant(), "Label.constant must be false");
    }

    @Test
    @DisplayName("R5 (reverse) – SystemConstant created → constant Label")
    void r5_systemConstantCreated_constantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "CalibrationModule");
        util.addSystemConstant(vsum, "CalibrationModule", "SYS_MAX");

        Label label = util.getCorrespondingInAmalthea(vsum, "SYS_MAX", Label.class);
        assertNotNull(label, "Label must be created for SystemConstant");
        assertTrue(label.isConstant(), "Label.constant must be true");
    }

    // R7-R10 (reverse) — PrimitiveType/ComposedType → BaseTypeDefinition/Array

    @Test
    @DisplayName("R7 (reverse) – ASEM BooleanType created → BaseTypeDefinition size=1 bit")
    void r7_asemBooleanTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemBooleanType(vsum, tempDir, "reverseBool");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseBool", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM BooleanType");
        assertEquals(1, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("R8 (reverse) – ASEM UnsignedDiscreteType created → BaseTypeDefinition size=32 bit")
    void r8_asemUnsignedDiscreteTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemUnsignedDiscreteType(vsum, tempDir, "reverseUint");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseUint", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM UnsignedDiscreteType");
        assertEquals(32, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("R8 (reverse, interactive) – user picks 8 → BaseTypeDefinition size=8 bit")
    void r8_asemUnsignedDiscreteTypeCreated_userPicks8_size8(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemUnsignedDiscreteType(vsum, tempDir, "reverseUint8", "8");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseUint8", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM UnsignedDiscreteType");
        assertEquals(8, btd.getSize().getValue().intValue(),
                "size must reflect the user's dialog answer, not the old hardcoded 32");
    }

    @Test
    @DisplayName("R8 (reverse) – ASEM SignedDiscreteType created → BaseTypeDefinition size=32 bit")
    void r8_asemSignedDiscreteTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSignedDiscreteType(vsum, tempDir, "reverseSint");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseSint", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM SignedDiscreteType");
        assertEquals(32, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("R8 (reverse, interactive) – user picks 16 → BaseTypeDefinition size=16 bit")
    void r8_asemSignedDiscreteTypeCreated_userPicks16_size16(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSignedDiscreteType(vsum, tempDir, "reverseSint16", "16");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseSint16", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM SignedDiscreteType");
        assertEquals(16, btd.getSize().getValue().intValue(),
                "size must reflect the user's dialog answer, not the old hardcoded 32");
    }

    @Test
    @DisplayName("R9 (reverse) – ASEM ContinuousType created → BaseTypeDefinition size=64 bit")
    void r9_asemContinuousTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemContinuousType(vsum, tempDir, "reverseFloat");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseFloat", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM ContinuousType");
        assertEquals(64, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("R9 (reverse, interactive) – user picks 32 → BaseTypeDefinition size=32 bit")
    void r9_asemContinuousTypeCreated_userPicks32_size32(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemContinuousType(vsum, tempDir, "reverseFloat32", "32");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseFloat32", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM ContinuousType");
        assertEquals(32, btd.getSize().getValue().intValue(),
                "size must reflect the user's dialog answer, not the old hardcoded 64");
    }

    @Test
    @DisplayName("R10 (reverse) – ASEM ComposedType created → Array exists")
    void r10_asemComposedTypeCreated_arrayCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemComposedType(vsum, tempDir, "reverseComposed");

        Array array = util.getCorrespondingInAmalthea(vsum, null, Array.class);
        assertNotNull(array, "Array must be created for the ASEM ComposedType");
    }

    @Test
    @DisplayName("P12 (reverse) – ComposedType.numberElements copied to Array at creation")
    void p12_asemComposedTypeCreated_numberElementsCopied(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemComposedType(vsum, tempDir, "reverseComposed", 15);

        Array array = util.getCorrespondingInAmalthea(vsum, null, Array.class);
        assertNotNull(array, "Array must be created for the ASEM ComposedType");
        assertEquals(15, array.getNumberElements());
    }

    @Test
    @DisplayName("P12 (reverse) – ComposedType.numberElements changed → Array.numberElements updated")
    void p12_asemComposedTypeNumberElementsChanged_arrayUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemComposedType(vsum, tempDir, "reverseComposed", 15);
        util.setComposedTypeNumberElements(vsum, "reverseComposed", 40);

        Array array = util.getCorrespondingInAmalthea(vsum, null, Array.class);
        assertNotNull(array);
        assertEquals(40, array.getNumberElements());
    }
}
