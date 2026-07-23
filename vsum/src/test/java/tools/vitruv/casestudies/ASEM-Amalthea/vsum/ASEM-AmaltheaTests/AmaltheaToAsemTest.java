package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.ISR;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;
import org.eclipse.app4mc.amalthea.model.DataTypeDefinition;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.ComposedType;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.InitTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.SoftwareTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.PeriodicTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.TimeTableTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Task;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Constant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Input;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Method;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Output;
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
 * AmaltheaToAsemTest
 *
 * Tests all E and P rules where changes originate on the AMALTHEA side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AmaltheaToAsemTest {

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

    // P1 — Component.name → Module.name

    @Test
    @DisplayName("P1 – Component renamed → Module name updated")
    void p1_componentRenamed_moduleNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "OldName");
        util.renameInAmalthea(vsum, "OldName", Component.class, "NewName");

        assertNull(util.getCorrespondingInAsem(vsum, "OldName", Module.class),
                "old Module name must not exist");
        assertNotNull(util.getCorrespondingInAsem(vsum, "NewName", Module.class),
                "Module with new name must exist");
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

    // P3 — Runnable.name → Method.name

    @Test
    @DisplayName("P3 – Runnable renamed → Method name updated")
    void p3_runnableRenamed_methodNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "ECU");
        util.addRunnable(vsum, "ECU", "oldRoutine");
        util.renameInAmalthea(vsum, "oldRoutine", Runnable.class, "newRoutine");

        assertNull(util.getCorrespondingInAsem(vsum, "oldRoutine", Method.class));
        assertNotNull(util.getCorrespondingInAsem(vsum, "newRoutine", Method.class));
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

    // P5 — Label.name propagation

    @Test
    @DisplayName("P5 – Non-constant Label renamed → Message name updated")
    void p5_nonConstantLabelRenamed_messageNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "ABS");
        util.addLabel(vsum, "ABS", "oldSignal", false);
        util.renameInAmalthea(vsum, "oldSignal", Label.class, "newSignal");

        assertNull(util.getCorrespondingInAsem(vsum, "oldSignal", Message.class));
        assertNotNull(util.getCorrespondingInAsem(vsum, "newSignal", Message.class));
    }

    @Test
    @DisplayName("P5 – Constant Label renamed → Constant name updated")
    void p5_constantLabelRenamed_constantNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Config");
        util.addLabel(vsum, "Config", "OLD_LIMIT", true);
        util.renameInAmalthea(vsum, "OLD_LIMIT", Label.class, "NEW_LIMIT");

        assertNull(util.getCorrespondingInAsem(vsum, "OLD_LIMIT", Constant.class));
        assertNotNull(util.getCorrespondingInAsem(vsum, "NEW_LIMIT", Constant.class));
    }

    // P8 / P9 — Label.constant flipped

    @Test
    @DisplayName("P8 – Label.constant false→true: Message replaced by Constant")
    void p8_labelFlippedToConstant(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Sensor");
        util.addLabel(vsum, "Sensor", "reading", false);
        assertNotNull(util.getCorrespondingInAsem(vsum, "reading", Message.class));

        util.setConstant(vsum, "reading", true);

        assertNull(util.getCorrespondingInAsem(vsum, "reading", Message.class),
                "Message must be removed");
        assertNotNull(util.getCorrespondingInAsem(vsum, "reading", Constant.class),
                "Constant must be created");
    }

    @Test
    @DisplayName("P9 – Label.constant true→false: Constant replaced by Message")
    void p9_labelFlippedToVariable(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Config");
        util.addLabel(vsum, "Config", "threshold", true);
        assertNotNull(util.getCorrespondingInAsem(vsum, "threshold", Constant.class));

        util.setConstant(vsum, "threshold", false);

        assertNull(util.getCorrespondingInAsem(vsum, "threshold", Constant.class),
                "Constant must be removed");
        assertNotNull(util.getCorrespondingInAsem(vsum, "threshold", Message.class),
                "Message must be created");
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

    // P11 — BaseTypeDefinition.size changed → old primitive removed, new one built

    @Test
    @DisplayName("P11 – BTD size changed 8 → 32 (same family) → old UnsignedDiscreteType replaced")
    void p11_sizeChanged_8to32_unsignedDiscreteReplaced(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "uint_x", 8, "u8");
        assertNotNull(util.getCorrespondingInAsem(vsum, "uint_x", UnsignedDiscreteType.class));

        util.changeBaseTypeDefinitionSize(vsum, "uint_x", 32);

        UnsignedDiscreteType resized =
                util.getCorrespondingInAsem(vsum, "uint_x", UnsignedDiscreteType.class);
        assertNotNull(resized, "a new UnsignedDiscreteType must exist after the resize");
        assertEquals("uint_x", resized.getName());
    }

    @Test
    @DisplayName("P11 – BTD size changed 1 → 8 (crosses families) → BooleanType replaced with UnsignedDiscreteType")
    void p11_sizeChanged_1to8_booleanReplacedWithDiscrete(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "flag_x", 1, "sint8");
        assertNotNull(util.getCorrespondingInAsem(vsum, "flag_x", BooleanType.class));

        util.changeBaseTypeDefinitionSize(vsum, "flag_x", 8);

        assertNull(util.getCorrespondingInAsem(vsum, "flag_x", BooleanType.class),
                "the old BooleanType correspondence must be gone after resizing away from size=1");
        SignedDiscreteType resized =
                util.getCorrespondingInAsem(vsum, "flag_x", SignedDiscreteType.class);
        assertNotNull(resized, "a new SignedDiscreteType must exist after the resize");
    }

    @Test
    @DisplayName("P11 – BTD size changed 32 → 64 (stays within ContinuousType family) → still ContinuousType")
    void p11_sizeChanged_32to64_continuousStaysContinuous(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "double_x", 32, "double_x");
        assertNotNull(util.getCorrespondingInAsem(vsum, "double_x", ContinuousType.class));

        util.changeBaseTypeDefinitionSize(vsum, "double_x", 64);

        ContinuousType resized = util.getCorrespondingInAsem(vsum, "double_x", ContinuousType.class);
        assertNotNull(resized, "a ContinuousType must still exist after resizing within the family");
        assertEquals("double_x", resized.getName());
    }

    @Test
    @DisplayName("P11 – BTD size changed to a value with no matching Rule 7–9 target → old type removed, nothing replaces it")
    void p11_sizeChanged_toUnmatchedSize_oldTypeRemovedNoReplacement(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "uint_y", 32, "u32");
        assertNotNull(util.getCorrespondingInAsem(vsum, "uint_y", UnsignedDiscreteType.class));

        util.changeBaseTypeDefinitionSize(vsum, "uint_y", 64);

        assertNull(util.getCorrespondingInAsem(vsum, "uint_y", UnsignedDiscreteType.class),
                "the old UnsignedDiscreteType must be gone even though nothing replaces it");
        assertNull(util.getCorrespondingInAsem(vsum, "uint_y", ContinuousType.class),
                "no ContinuousType should be created either — the alias never said float/double");
    }

    // SWModel → PrimitiveTypeRepository (new correspondence, not part of R1–R10)

    @Test
    @DisplayName("SWModel created → PrimitiveTypeRepository exists")
    void swModelCreated_primitiveTypeRepositoryExists(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        PrimitiveTypeRepository repo = util.getPrimitiveTypeRepository(vsum);
        assertNotNull(repo, "a PrimitiveTypeRepository must exist once the SWModel is created");
        assertEquals("PrimitiveTypes", repo.getName());
    }

    @Test
    @DisplayName("SWModel → PrimitiveTypeRepository is a singleton, not one per BaseTypeDefinition")
    void swModelCreated_primitiveTypeRepositoryIsSingleton(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "uint8", 8, "u8");
        util.addBaseTypeDefinition(vsum, "double64", 64, "double64");

        long repoCount = util.getDefaultView(vsum, java.util.List.of(PrimitiveTypeRepository.class))
                .getRootObjects(PrimitiveTypeRepository.class).size();
        assertEquals(1, repoCount, "only one PrimitiveTypeRepository must exist, regardless of how many types get created");
    }

    @Test
    @DisplayName("PrimitiveTypeRepository survives a BaseTypeDefinition being created and then deleted")
    void primitiveTypeRepository_survivesBaseTypeDefinitionDelete(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "temp_x", 8, "u8");
        assertNotNull(util.getPrimitiveTypeRepository(vsum));

        util.deleteFromAmalthea(vsum, "temp_x", BaseTypeDefinition.class);

        PrimitiveTypeRepository repo = util.getPrimitiveTypeRepository(vsum);
        assertNotNull(repo, "the repository must still exist after the BaseTypeDefinition is deleted");
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

    @Test
    @DisplayName("P12 – Array.numberElements copied to ComposedType at creation")
    void p12_arrayCreated_numberElementsCopied(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addArrayTypeDefinition(vsum, "SpeedBuffer", 10);
        ComposedType composedType =
                util.getCorrespondingInAsem(vsum, "ComposedType", ComposedType.class);
        assertNotNull(composedType);
        assertEquals(10, composedType.getNumberElements());
    }

    @Test
    @DisplayName("P12 – Array.numberElements changed → ComposedType.numberElements updated")
    void p12_arrayNumberElementsChanged_composedTypeUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addArrayTypeDefinition(vsum, "SpeedBuffer", 10);

        util.setArrayNumberElements(vsum, 25);

        ComposedType composedType =
                util.getCorrespondingInAsem(vsum, "ComposedType", ComposedType.class);
        assertNotNull(composedType);
        assertEquals(25, composedType.getNumberElements());
    }

    //Correspondence rules footnote invariants

    @Test
    @DisplayName("R1** – All Methods in Module must have null returnType and no parameters")
    void r1_moduleMethodsMustBeVoid(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "ECM");
        util.addRunnable(vsum, "ECM", "runA");
        util.addRunnable(vsum, "ECM", "runB");

        Module module = util.getCorrespondingInAsem(vsum, "ECM", Module.class);
        module.getMethods().forEach(m -> {
            assertNull(m.getReturnType(), "returnType must be null (footnote **)");
            assertTrue(m.getParameters().isEmpty(), "no parameters (footnote **)");
        });
    }

    @Test
    @DisplayName("R1* – All typedElements in Module must be Message instances")
    void r1_moduleTypedElementsMustBeMessages(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "SensorHub");
        util.addLabel(vsum, "SensorHub", "signal1", false);
        util.addLabel(vsum, "SensorHub", "signal2", false);

        Module module = util.getCorrespondingInAsem(vsum, "SensorHub", Module.class);
        module.getTypedElements().forEach(te ->
                assertInstanceOf(Message.class, te,
                        "all typedElements must be Message instances (footnote *)"));
    }

    // R2 / R3 — Task/ISR ↔ Task/InterruptTask

    @Test
    @DisplayName("R2 – Task created → ASEM Task with same name exists")
    void r2_taskCreated_asemTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "controlLoop");

        Task task = util.getCorrespondingInAsem(vsum, "controlLoop", Task.class);
        assertNotNull(task, "ASEM Task must be created for the AMALTHEA Task");
        assertEquals("controlLoop", task.getName());
    }

    @Test
    @DisplayName("R2 – Task deleted → ASEM Task removed")
    void r2_taskDeleted_asemTaskRemoved(@TempDir Path tempDir) throws Exception {
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
    @DisplayName("R2 – Task renamed → ASEM Task name updated")
    void r2_taskRenamed_asemTaskNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "oldTaskName");
        util.renameInAmalthea(vsum, "oldTaskName",
                org.eclipse.app4mc.amalthea.model.Task.class, "newTaskName");

        assertNull(util.getCorrespondingInAsem(vsum, "oldTaskName", Task.class));
        assertNotNull(util.getCorrespondingInAsem(vsum, "newTaskName", Task.class));
    }

    @Test
    @DisplayName("R3 – ISR created → ASEM InterruptTask with same name exists")
    void r3_isrCreated_interruptTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "InterruptCtrl");
        util.addISR(vsum, "InterruptCtrl", "timerISR");

        InterruptTask interruptTask = util.getCorrespondingInAsem(vsum, "timerISR", InterruptTask.class);
        assertNotNull(interruptTask, "ASEM InterruptTask must be created for the AMALTHEA ISR");
        assertEquals("timerISR", interruptTask.getName());
    }

    @Test
    @DisplayName("R2 – RunnableCall in Task's activityGraph does not crash propagation")
    void r2_runnableCall_doesNotCrashPropagation(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addRunnable(vsum, "Scheduler", "doWork");
        util.addTask(vsum, "Scheduler", "mainTask");

        assertDoesNotThrow(() -> util.addRunnableCall(vsum, "mainTask", "doWork"));
    }

    @Test
    @DisplayName("R2 (verification) – flat RunnableCall strictly populates Task.processes")
    void r2_runnableCall_flat_strictlyPopulatesProcesses(@TempDir Path tempDir) throws Exception {
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
    @DisplayName("R2 (verification) – RunnableCall nested inside a ProbabilitySwitch still populates Task.processes")
    void r2_runnableCall_nestedInProbabilitySwitch_populatesProcesses(@TempDir Path tempDir) throws Exception {
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

    // R2 — Task subtype selection (interactive dialog)

    @Test
    @DisplayName("R2 – Task created, user picks InitTask → ASEM InitTask exists")
    void r2_taskCreated_userPicksInitTask_initTaskCreated(@TempDir Path tempDir) throws Exception {
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
    @DisplayName("R2 – Task created, user picks PeriodicTask → ASEM PeriodicTask exists")
    void r2_taskCreated_userPicksPeriodicTask_periodicTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "tickTask", "PeriodicTask");

        PeriodicTask periodicTask = util.getCorrespondingInAsem(vsum, "tickTask", PeriodicTask.class);
        assertNotNull(periodicTask, "ASEM PeriodicTask must be created when the user selects PeriodicTask");
        assertEquals("tickTask", periodicTask.getName());
    }

    @Test
    @DisplayName("PeriodicTask fields – PeriodicStimulus attached (ms unit) → period/delay copied directly")
    void periodicTaskFields_stimulusInMillis_periodDelayCopied(@TempDir Path tempDir) throws Exception {
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
    @DisplayName("PeriodicTask fields – PeriodicStimulus in seconds → period/delay converted to milliseconds")
    void periodicTaskFields_stimulusInSeconds_convertedToMillis(@TempDir Path tempDir) throws Exception {
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

    @Test
    @DisplayName("R2 – Task created, user picks TimeTableTask → ASEM TimeTableTask exists")
    void r2_taskCreated_userPicksTimeTableTask_timeTableTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "scheduleTask", "TimeTableTask");

        TimeTableTask timeTableTask = util.getCorrespondingInAsem(vsum, "scheduleTask", TimeTableTask.class);
        assertNotNull(timeTableTask, "ASEM TimeTableTask must be created when the user selects TimeTableTask");
        assertEquals("scheduleTask", timeTableTask.getName());
    }

    @Test
    @DisplayName("R2 – Task created with no scripted answer defaults to SoftwareTask")
    void r2_taskCreated_defaultChoice_softwareTaskCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "plainTask");

        SoftwareTask softwareTask = util.getCorrespondingInAsem(vsum, "plainTask", SoftwareTask.class);
        assertNotNull(softwareTask, "ASEM SoftwareTask must be created by default");
    }

    // R5 / R6 — Label discriminator (Input/Output/SystemConstant)

    @Test
    @DisplayName("R6 – Label with only read accesses → Input")
    void r6_readOnlyLabel_inputCreated(@TempDir Path tempDir) throws Exception {
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
    @DisplayName("R6 – Label with only write accesses → Output")
    void r6_writeOnlyLabel_outputCreated(@TempDir Path tempDir) throws Exception {
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

    @Test
    @DisplayName("R5 – Label tagged systemConstant → SystemConstant instead of Constant")
    void r5_systemConstantTaggedLabel_systemConstantCreated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Calibration");
        util.addSystemConstantTaggedLabel(vsum, "Calibration", "SYS_LIMIT");

        SystemConstant systemConstant = util.getCorrespondingInAsem(vsum, "SYS_LIMIT", SystemConstant.class);
        assertNotNull(systemConstant, "tagged Label must become a SystemConstant");
    }
}
