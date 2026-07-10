package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;
import org.eclipse.app4mc.amalthea.model.DataTypeDefinition;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.ComposedType;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
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
 * AmaltheaToAsemTest
 *
 * Tests all E and P rules where changes originate on the AMALTHEA side.
 *
 * Key pattern: helpers return the name (String) of created elements.
 * Assertions retrieve elements fresh from the VSUM view by name.
 * This avoids stale EMF object references across view commits.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AmaltheaToAsemTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // ── E1 / E2 — Component ↔ Module ─────────────────────────────────────────

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

    // ── P1 — Component.name → Module.name ────────────────────────────────────

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

    // ── E5 / E6 — Runnable ↔ Method ──────────────────────────────────────────

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

    // ── P3 — Runnable.name → Method.name ─────────────────────────────────────

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

    // ── E9 / E10 / E11 — Label ↔ Message / Constant ──────────────────────────

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

    // ── P5 — Label.name propagation ───────────────────────────────────────────

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

    // ── P8 / P9 — Label.constant flipped ─────────────────────────────────────

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

    // ── E14–E16 — BaseTypeDefinition ↔ PrimitiveType ─────────────────────────

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

    // ── E17 — Array → ComposedType ────────────────────────────────────────────

    @Test
    @DisplayName("E17 – Array in DataTypeDefinition → ComposedType created")
    void e17_array_composedType(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addArrayTypeDefinition(vsum, "SpeedBuffer", 10);
        assertNotNull(util.getCorrespondingInAsem(vsum, "ComposedType", ComposedType.class));
    }

    // ── Table 5.1 footnote invariants ─────────────────────────────────────────

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
}
