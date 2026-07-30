package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;

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
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.SystemConstant;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.BooleanType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.ContinuousType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.PrimitiveTypeRepository;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.SignedDiscreteType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.UnsignedDiscreteType;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests Existence rules (E1-E33) where changes originate on the AMALTHEA side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AmaltheaToAsemExistenceTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // E1 / E2 — Component ↔ Module

    @Test
    @DisplayName("E1 – Component created → Module with same name exists in ASEM view")
    void e1_componentCreated_moduleCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "BrakeController");

        Module module = util.getCorrespondingInAsem(vsum, "BrakeController", Module.class);
        assertNotNull(module, "Module must be created for the new Component");
        assertEquals("BrakeController", module.getName());
    }

    @Test
    @DisplayName("E2 – Component deleted → Module is removed from ASEM view")
    void e2_componentDeleted_moduleRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "TempSensor");
        assertNotNull(util.getCorrespondingInAsem(vsum, "TempSensor", Module.class));

        util.deleteFromAmalthea(vsum, "TempSensor", Component.class);

        assertNull(util.getCorrespondingInAsem(vsum, "TempSensor", Module.class),
                "Module must be removed after Component is deleted");
    }

    // E5 / E6 — Runnable ↔ Method

    @Test
    @DisplayName("E5 – Runnable created → void no-param Method in parent Module")
    void e5_runnableCreated_methodCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "IgnitionCtrl");
        util.addRunnable(vsum, "IgnitionCtrl", "fireIgnition");

        Module module = util.getCorrespondingInAsem(vsum, "IgnitionCtrl", Module.class);
        Method method = util.getCorrespondingInAsem(vsum, "fireIgnition", Method.class);

        assertNotNull(method, "Method must be created for the Runnable");
        assertEquals("fireIgnition", method.getName());
        assertNull(method.getReturnType(),   "returnType must be null — Rule 4 OCL");
        assertTrue(method.getParameters().isEmpty(), "no parameters — Rule 4 OCL");
        assertTrue(module.getMethods().stream().anyMatch(m -> method.getName().equals(m.getName())),
                "Method must be in Module.methods");
    }

    @Test
    @DisplayName("E6 – Runnable deleted → Method removed")
    void e6_runnableDeleted_methodRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "GearBox");
        util.addRunnable(vsum, "GearBox", "shiftGear");
        assertNotNull(util.getCorrespondingInAsem(vsum, "shiftGear", Method.class));

        util.deleteFromAmalthea(vsum, "shiftGear", Runnable.class);

        assertNull(util.getCorrespondingInAsem(vsum, "shiftGear", Method.class),
                "Method must be removed");
    }

    // E9 / E10 / E11 — Label ↔ Message / Constant

    @Test
    @DisplayName("E9 – Non-constant Label → Message in Module.typedElements")
    void e9_nonConstantLabel_messageCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "SpeedSensor");
        util.addLabel(vsum, "SpeedSensor", "vehicleSpeed", false);

        Module module   = util.getCorrespondingInAsem(vsum, "SpeedSensor", Module.class);
        Message message = util.getCorrespondingInAsem(vsum, "vehicleSpeed", Message.class);

        assertNotNull(message, "Message must be created for non-constant Label");
        assertEquals("vehicleSpeed", message.getName());
        assertTrue(module.getTypedElements().stream()
                        .anyMatch(te -> message.getName().equals(te.getName())),
                "Message must be in Module.typedElements");
    }

    @Test
    @DisplayName("E10 – Constant Label → Constant in Module.typedElements")
    void e10_constantLabel_asemConstantCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Calibration");
        util.addLabel(vsum, "Calibration", "MAX_TORQUE", true);

        Module module     = util.getCorrespondingInAsem(vsum, "Calibration", Module.class);
        Constant constant = util.getCorrespondingInAsem(vsum, "MAX_TORQUE", Constant.class);

        assertNotNull(constant, "Constant must be created for constant Label");
        assertEquals("MAX_TORQUE", constant.getName());
        assertTrue(module.getTypedElements().stream()
                        .anyMatch(te -> constant.getName().equals(te.getName())),
                "Constant must be in Module.typedElements");
    }

    @Test
    @DisplayName("E11 – Non-constant Label deleted → Message removed")
    void e11_nonConstantLabelDeleted_messageRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "RPMSensor");
        util.addLabel(vsum, "RPMSensor", "engineRPM", false);
        assertNotNull(util.getCorrespondingInAsem(vsum, "engineRPM", Message.class));

        util.deleteFromAmalthea(vsum, "engineRPM", Label.class);

        assertNull(util.getCorrespondingInAsem(vsum, "engineRPM", Message.class),
                "Message must be removed");
    }

    @Test
    @DisplayName("E11 – Constant Label deleted → Constant removed")
    void e11_constantLabelDeleted_asemConstantRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "LimitConfig");
        util.addLabel(vsum, "LimitConfig", "MAX_RPM", true);
        assertNotNull(util.getCorrespondingInAsem(vsum, "MAX_RPM", Constant.class));

        util.deleteFromAmalthea(vsum, "MAX_RPM", Label.class);

        assertNull(util.getCorrespondingInAsem(vsum, "MAX_RPM", Constant.class),
                "Constant must be removed");
    }

    // E14–E16 — BaseTypeDefinition ↔ PrimitiveType

    @Test
    @DisplayName("E14 – BTD size=1 → BooleanType")
    void e14_sizeOne_booleanType(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "LogType", 1, null);
        assertNotNull(util.getCorrespondingInAsem(vsum, "LogType", BooleanType.class));
    }

    @Test
    @DisplayName("E15 – BTD size=8 unsigned alias → UnsignedDiscreteType")
    void e15_size8_unsigned(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "uint8", 8, "u8");
        assertNotNull(util.getCorrespondingInAsem(vsum, "uint8", UnsignedDiscreteType.class));
    }

    @Test
    @DisplayName("E15 – BTD size=16 signed alias → SignedDiscreteType")
    void e15_size16_signed(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "sint16", 16, "sint16");
        assertNotNull(util.getCorrespondingInAsem(vsum, "sint16", SignedDiscreteType.class));
    }

    @Test
    @DisplayName("E15 – BTD size=32 int alias → DiscreteType not ContinuousType")
    void e15_size32_intAlias(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "sint32", 32, "sint32");
        assertNotNull(util.getCorrespondingInAsem(vsum, "sint32", SignedDiscreteType.class));
        assertNull(util.getCorrespondingInAsem(vsum, "sint32", ContinuousType.class));
    }

    @Test
    @DisplayName("E16 – BTD size=32 float alias → ContinuousType not DiscreteType")
    void e16_size32_floatAlias(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "float32", 32, "float32");
        assertNotNull(util.getCorrespondingInAsem(vsum, "float32", ContinuousType.class));
        assertNull(util.getCorrespondingInAsem(vsum, "float32", UnsignedDiscreteType.class));
    }

    @Test
    @DisplayName("E16 – BTD size=64 double alias → ContinuousType")
    void e16_size64_doubleAlias(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "double64", 64, "double64");
        assertNotNull(util.getCorrespondingInAsem(vsum, "double64", ContinuousType.class));
    }

    // E33 — SWModel → PrimitiveTypeRepository 

    @Test
    @DisplayName("E33 – SWModel created → PrimitiveTypeRepository exists")
    void swModelCreated_primitiveTypeRepositoryExists(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        PrimitiveTypeRepository repo = util.getPrimitiveTypeRepository(vsum);
        assertNotNull(repo, "a PrimitiveTypeRepository must exist once the SWModel is created");
        assertEquals("PrimitiveTypes", repo.getName());
    }

    // E17 — Array → ComposedType

    @Test
    @DisplayName("E17 – Array in DataTypeDefinition → ComposedType created")
    void e17_array_composedType(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addArrayTypeDefinition(vsum, "SpeedBuffer", 10);
        assertNotNull(util.getCorrespondingInAsem(vsum, "ComposedType", ComposedType.class));
    }

    // E18-E21 — Task/ISR ↔ Task/InterruptTask (Rules 2 & 3)

    @Test
    @DisplayName("E18 – Task created → ASEM Task with same name exists")
    void e18_taskCreated_asemTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "controlLoop");

        Task task = util.getCorrespondingInAsem(vsum, "controlLoop", Task.class);
        assertNotNull(task, "ASEM Task must be created for the AMALTHEA Task");
        assertEquals("controlLoop", task.getName());
    }

    @Test
    @DisplayName("E19 – Task deleted → ASEM Task removed")
    void e19_taskDeleted_asemTaskRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "cleanupTask");
        assertNotNull(util.getCorrespondingInAsem(vsum, "cleanupTask", Task.class));

        util.deleteFromAmalthea(vsum, "cleanupTask", org.eclipse.app4mc.amalthea.model.Task.class);

        assertNull(util.getCorrespondingInAsem(vsum, "cleanupTask", Task.class),
                "ASEM Task must be removed");
    }

    @Test
    @DisplayName("E20 – ISR created → ASEM InterruptTask with same name exists")
    void e20_isrCreated_interruptTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "InterruptCtrl");
        util.addISR(vsum, "InterruptCtrl", "timerISR");

        InterruptTask interruptTask = util.getCorrespondingInAsem(vsum, "timerISR", InterruptTask.class);
        assertNotNull(interruptTask, "ASEM InterruptTask must be created for the AMALTHEA ISR");
        assertEquals("timerISR", interruptTask.getName());
    }

    @Test
    @DisplayName("E21 – ISR deleted → ASEM InterruptTask removed")
    void e21_isrDeleted_interruptTaskRemoved(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "InterruptCtrl");
        util.addISR(vsum, "InterruptCtrl", "watchdogISR");
        assertNotNull(util.getCorrespondingInAsem(vsum, "watchdogISR", InterruptTask.class));

        util.deleteFromAmalthea(vsum, "watchdogISR", org.eclipse.app4mc.amalthea.model.ISR.class);

        assertNull(util.getCorrespondingInAsem(vsum, "watchdogISR", InterruptTask.class),
                "ASEM InterruptTask must be removed");
    }

    // E18 — Task subtype selection (interactive dialog)

    @Test
    @DisplayName("E18 – Task created, user picks InitTask → ASEM InitTask exists")
    void e18_taskCreated_userPicksInitTask_initTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "bootTask", "InitTask");

        InitTask initTask = util.getCorrespondingInAsem(vsum, "bootTask", InitTask.class);
        assertNotNull(initTask, "ASEM InitTask must be created when the user selects InitTask");
        assertEquals("bootTask", initTask.getName());
        // must NOT also show up under a different subtype's exact class
        assertNull(util.getCorrespondingInAsem(vsum, "bootTask", PeriodicTask.class));
    }

    @Test
    @DisplayName("E18 – Task created, user picks PeriodicTask → ASEM PeriodicTask exists")
    void e18_taskCreated_userPicksPeriodicTask_periodicTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "tickTask", "PeriodicTask");

        PeriodicTask periodicTask = util.getCorrespondingInAsem(vsum, "tickTask", PeriodicTask.class);
        assertNotNull(periodicTask, "ASEM PeriodicTask must be created when the user selects PeriodicTask");
        assertEquals("tickTask", periodicTask.getName());
    }

    @Test
    @DisplayName("E18 – Task created, user picks TimeTableTask → ASEM TimeTableTask exists")
    void e18_taskCreated_userPicksTimeTableTask_timeTableTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "scheduleTask", "TimeTableTask");

        TimeTableTask timeTableTask = util.getCorrespondingInAsem(vsum, "scheduleTask", TimeTableTask.class);
        assertNotNull(timeTableTask, "ASEM TimeTableTask must be created when the user selects TimeTableTask");
        assertEquals("scheduleTask", timeTableTask.getName());
    }

    @Test
    @DisplayName("E18 – Task created with no scripted answer defaults to SoftwareTask")
    void e18_taskCreated_defaultChoice_softwareTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "plainTask");

        SoftwareTask softwareTask = util.getCorrespondingInAsem(vsum, "plainTask", SoftwareTask.class);
        assertNotNull(softwareTask, "ASEM SoftwareTask must be created by default");
    }

    // E10 discriminator — Label tagged systemConstant

    @Test
    @DisplayName("E10 – Label tagged systemConstant → SystemConstant instead of Constant")
    void e10_systemConstantTaggedLabel_systemConstantCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Calibration");
        util.addSystemConstantTaggedLabel(vsum, "Calibration", "SYS_LIMIT");

        SystemConstant systemConstant = util.getCorrespondingInAsem(vsum, "SYS_LIMIT", SystemConstant.class);
        assertNotNull(systemConstant, "tagged Label must become a SystemConstant");
    }
}
