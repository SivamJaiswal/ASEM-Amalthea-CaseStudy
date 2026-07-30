package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Array;
import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;
import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.ISR;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Constant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Input;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Method;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Output;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.SystemConstant;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests Existence rules (E1-E33) where changes originate on the ASEM side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AsemToAmaltheaExistenceTest {

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

    // E34 — Message/Constant/Input/Output/SystemConstant deleted → Label deleted

    @Test
    @DisplayName("E34 – Message deleted → Label removed")
    void e11_messageDeleted_labelRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "SignalModule");
        util.addMessage(vsum, "SignalModule", "signalX");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "signalX", Label.class));

        util.deleteFromAsem(vsum, "signalX", Message.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "signalX", Label.class),
                "Label must be removed");
    }

    @Test
    @DisplayName("E34 – Constant deleted → Label removed")
    void e11_constantDeleted_labelRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ConfigModule");
        util.addConstant(vsum, "ConfigModule", "MAX_VAL");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "MAX_VAL", Label.class));

        util.deleteFromAsem(vsum, "MAX_VAL", Constant.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "MAX_VAL", Label.class),
                "Label must be removed");
    }

    @Test
    @DisplayName("E34 – Input deleted → Label removed")
    void e11_inputDeleted_labelRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "SensorModule");
        util.addInput(vsum, "SensorModule", "inSignal");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "inSignal", Label.class));

        util.deleteFromAsem(vsum, "inSignal", Input.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "inSignal", Label.class),
                "Label must be removed");
    }

    @Test
    @DisplayName("E34 – Output deleted → Label removed")
    void e11_outputDeleted_labelRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ActuatorModule");
        util.addOutput(vsum, "ActuatorModule", "outSignal");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "outSignal", Label.class));

        util.deleteFromAsem(vsum, "outSignal", Output.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "outSignal", Label.class),
                "Label must be removed");
    }

    @Test
    @DisplayName("E34 – SystemConstant deleted → Label removed")
    void e11_systemConstantDeleted_labelRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "CalibModule");
        util.addSystemConstant(vsum, "CalibModule", "SYS_MAX");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "SYS_MAX", Label.class));

        util.deleteFromAsem(vsum, "SYS_MAX", SystemConstant.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "SYS_MAX", Label.class),
                "Label must be removed");
    }

    // E22-E25 — ASEM Task/InterruptTask → AMALTHEA Task/ISR

    @Test
    @DisplayName("E22 – ASEM Task created → AMALTHEA Task exists")
    void e22_asemTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemTask(vsum, tempDir, "reverseTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM Task");
        assertEquals("reverseTask", task.getName());
    }

    @Test
    @DisplayName("E23 – ASEM Task deleted → AMALTHEA Task removed")
    void e23_asemTaskDeleted_taskRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemTask(vsum, tempDir, "reverseTaskToDelete");
        assertNotNull(util.getCorrespondingInAmalthea(
                vsum, "reverseTaskToDelete", org.eclipse.app4mc.amalthea.model.Task.class));

        util.deleteFromAsem(vsum, "reverseTaskToDelete",
                edu.kit.ipd.sdq.metamodels.asem.classifiers.Task.class);

        assertNull(util.getCorrespondingInAmalthea(
                vsum, "reverseTaskToDelete", org.eclipse.app4mc.amalthea.model.Task.class),
                "AMALTHEA Task must be removed");
    }

    @Test
    @DisplayName("E24 – ASEM InterruptTask created → AMALTHEA ISR exists")
    void e24_asemInterruptTaskCreated_isrCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemInterruptTask(vsum, tempDir, "reverseISR");

        ISR isr = util.getCorrespondingInAmalthea(vsum, "reverseISR", ISR.class);
        assertNotNull(isr, "AMALTHEA ISR must be created for the ASEM InterruptTask");
        assertEquals("reverseISR", isr.getName());
    }

    @Test
    @DisplayName("E25 – ASEM InterruptTask deleted → AMALTHEA ISR removed")
    void e25_asemInterruptTaskDeleted_isrRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemInterruptTask(vsum, tempDir, "reverseISRToDelete");
        assertNotNull(util.getCorrespondingInAmalthea(vsum, "reverseISRToDelete", ISR.class));

        util.deleteFromAsem(vsum, "reverseISRToDelete",
                edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask.class);

        assertNull(util.getCorrespondingInAmalthea(vsum, "reverseISRToDelete", ISR.class),
                "AMALTHEA ISR must be removed");
    }

    @Test
    @DisplayName("E22 – ASEM InitTask created → AMALTHEA Task exists")
    void e22_asemInitTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemInitTask(vsum, tempDir, "reverseInitTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseInitTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM InitTask");
        assertEquals("reverseInitTask", task.getName());
    }

    @Test
    @DisplayName("E22 – ASEM SoftwareTask created → AMALTHEA Task exists")
    void e22_asemSoftwareTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSoftwareTask(vsum, tempDir, "reverseSoftwareTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseSoftwareTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM SoftwareTask");
        assertEquals("reverseSoftwareTask", task.getName());
    }

    @Test
    @DisplayName("E22 – ASEM PeriodicTask created → AMALTHEA Task exists")
    void e22_asemPeriodicTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemPeriodicTask(vsum, tempDir, "reversePeriodicTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reversePeriodicTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM PeriodicTask");
        assertEquals("reversePeriodicTask", task.getName());
    }

    @Test
    @DisplayName("E22 – ASEM TimeTableTask created → AMALTHEA Task exists")
    void e22_asemTimeTableTaskCreated_taskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemTimeTableTask(vsum, tempDir, "reverseTimeTableTask");

        org.eclipse.app4mc.amalthea.model.Task task = util.getCorrespondingInAmalthea(
                vsum, "reverseTimeTableTask", org.eclipse.app4mc.amalthea.model.Task.class);
        assertNotNull(task, "AMALTHEA Task must be created for the ASEM TimeTableTask");
        assertEquals("reverseTimeTableTask", task.getName());
    }

    // E26-E28 — Input/Output/SystemConstant → Label

    @Test
    @DisplayName("E26 – Input created → non-constant Label")
    void e26_inputCreated_nonConstantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "SensorModule");
        util.addInput(vsum, "SensorModule", "rawInput");

        Label label = util.getCorrespondingInAmalthea(vsum, "rawInput", Label.class);
        assertNotNull(label, "Label must be created for Input");
        assertFalse(label.isConstant(), "Label.constant must be false");
    }

    @Test
    @DisplayName("E27 – Output created → non-constant Label")
    void e27_outputCreated_nonConstantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "ActuatorModule");
        util.addOutput(vsum, "ActuatorModule", "rawOutput");

        Label label = util.getCorrespondingInAmalthea(vsum, "rawOutput", Label.class);
        assertNotNull(label, "Label must be created for Output");
        assertFalse(label.isConstant(), "Label.constant must be false");
    }

    @Test
    @DisplayName("E28 – SystemConstant created → constant Label")
    void e28_systemConstantCreated_constantLabelCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addModule(vsum, tempDir, "CalibrationModule");
        util.addSystemConstant(vsum, "CalibrationModule", "SYS_MAX");

        Label label = util.getCorrespondingInAmalthea(vsum, "SYS_MAX", Label.class);
        assertNotNull(label, "Label must be created for SystemConstant");
        assertTrue(label.isConstant(), "Label.constant must be true");
    }

    // E29-E32 — PrimitiveType/ComposedType → BaseTypeDefinition/Array

    @Test
    @DisplayName("E29 – ASEM BooleanType created → BaseTypeDefinition size=1 bit")
    void e29_asemBooleanTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemBooleanType(vsum, tempDir, "reverseBool");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseBool", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM BooleanType");
        assertEquals(1, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("E30 – ASEM UnsignedDiscreteType created → BaseTypeDefinition size=32 bit")
    void e30_asemUnsignedDiscreteTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemUnsignedDiscreteType(vsum, tempDir, "reverseUint");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseUint", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM UnsignedDiscreteType");
        assertEquals(32, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("E30 – user picks 8 → BaseTypeDefinition size=8 bit")
    void e30_asemUnsignedDiscreteTypeCreated_userPicks8_size8(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemUnsignedDiscreteType(vsum, tempDir, "reverseUint8", "8");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseUint8", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM UnsignedDiscreteType");
        assertEquals(8, btd.getSize().getValue().intValue(),
                "size must reflect the user's dialog answer, not the old hardcoded 32");
    }

    @Test
    @DisplayName("E30 – ASEM SignedDiscreteType created → BaseTypeDefinition size=32 bit")
    void e30_asemSignedDiscreteTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSignedDiscreteType(vsum, tempDir, "reverseSint");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseSint", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM SignedDiscreteType");
        assertEquals(32, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("E30 – user picks 16 → BaseTypeDefinition size=16 bit")
    void e30_asemSignedDiscreteTypeCreated_userPicks16_size16(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemSignedDiscreteType(vsum, tempDir, "reverseSint16", "16");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseSint16", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM SignedDiscreteType");
        assertEquals(16, btd.getSize().getValue().intValue(),
                "size must reflect the user's dialog answer, not the old hardcoded 32");
    }

    @Test
    @DisplayName("E31 – ASEM ContinuousType created → BaseTypeDefinition size=64 bit")
    void e31_asemContinuousTypeCreated_baseTypeDefinitionCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemContinuousType(vsum, tempDir, "reverseFloat");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseFloat", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM ContinuousType");
        assertEquals(64, btd.getSize().getValue().intValue());
    }

    @Test
    @DisplayName("E31 – user picks 32 → BaseTypeDefinition size=32 bit")
    void e31_asemContinuousTypeCreated_userPicks32_size32(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemContinuousType(vsum, tempDir, "reverseFloat32", "32");

        BaseTypeDefinition btd = util.getCorrespondingInAmalthea(vsum, "reverseFloat32", BaseTypeDefinition.class);
        assertNotNull(btd, "BaseTypeDefinition must be created for the ASEM ContinuousType");
        assertEquals(32, btd.getSize().getValue().intValue(),
                "size must reflect the user's dialog answer, not the old hardcoded 64");
    }

    @Test
    @DisplayName("E32 – ASEM ComposedType created → Array exists")
    void e32_asemComposedTypeCreated_arrayCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addAsemComposedType(vsum, tempDir, "reverseComposed");

        Array array = util.getCorrespondingInAmalthea(vsum, null, Array.class);
        assertNotNull(array, "Array must be created for the ASEM ComposedType");
    }
}
