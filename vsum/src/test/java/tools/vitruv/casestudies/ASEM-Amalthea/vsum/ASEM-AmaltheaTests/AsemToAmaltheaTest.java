package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;

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
 * AsemToAmaltheaTest
 *
 * Tests all E and P rules where changes originate on the ASEM side.
 * Uses name-based lookups — never holds stale EMF object references
 * across view commits.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AsemToAmaltheaTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // ── E3 / E4 — Module ↔ Component ─────────────────────────────────────────

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

    // ── P2 — Module.name → Component.name ────────────────────────────────────

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

    // ── E7 / E8 — Method ↔ Runnable ──────────────────────────────────────────

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
        assertTrue(comp.getRunnables().contains(runnable));
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

    // ── P4 — Method.name → Runnable.name ─────────────────────────────────────

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

    // ── E12 — Message → Label (constant=false) ────────────────────────────────

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
        assertTrue(comp.getLabels().contains(label));
    }

    // ── E13 — Constant → Label (constant=true) ───────────────────────────────

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
        assertTrue(comp.getLabels().contains(label));
    }

    // ── P6 / P7 — name propagation ────────────────────────────────────────────

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

    // ── Bidirectional round-trip ──────────────────────────────────────────────

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
}
