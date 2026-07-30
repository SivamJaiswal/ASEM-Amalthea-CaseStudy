package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.ComposedType;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.PeriodicTask;
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
 * Tests Property rules (P1-P19) where changes originate on the AMALTHEA side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AmaltheaToAsemPropertyTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
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

    // P11 — BaseTypeDefinition.size changed → old primitive removed, new one built

    @Test
    @DisplayName("P11 – BTD size changed 8 → 32 → old UnsignedDiscreteType replaced")
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
    @DisplayName("P11 – BTD size changed 1 → 8 → BooleanType replaced with UnsignedDiscreteType")
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
    @DisplayName("P11 – BTD size changed 32 → 64 → still ContinuousType")
    void p11_sizeChanged_32to64_continuousStaysContinuous(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "double_x", 32, "double_x");
        assertNotNull(util.getCorrespondingInAsem(vsum, "double_x", ContinuousType.class));

        util.changeBaseTypeDefinitionSize(vsum, "double_x", 64);

        ContinuousType resized = util.getCorrespondingInAsem(vsum, "double_x", ContinuousType.class);
        assertNotNull(resized, "a ContinuousType must still exist after resizing");
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

    // P12 — Array.numberElements ↔ ComposedType.numberElements

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

    // P13 — Task.name → ASEM Task subtype.name

    @Test
    @DisplayName("P13 – Task renamed → ASEM Task name updated")
    void p13_taskRenamed_asemTaskNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "oldTaskName");
        util.renameInAmalthea(vsum, "oldTaskName",
                org.eclipse.app4mc.amalthea.model.Task.class, "newTaskName");

        assertNull(util.getCorrespondingInAsem(vsum, "oldTaskName", Task.class));
        assertNotNull(util.getCorrespondingInAsem(vsum, "newTaskName", Task.class));
    }

    // P14 — ISR.name → ASEM InterruptTask.name

    @Test
    @DisplayName("P14 – ISR renamed → ASEM InterruptTask name updated")
    void p14_isrRenamed_interruptTaskNameUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "InterruptCtrl");
        util.addISR(vsum, "InterruptCtrl", "oldIsrName");
        util.renameInAmalthea(vsum, "oldIsrName",
                org.eclipse.app4mc.amalthea.model.ISR.class, "newIsrName");

        assertNull(util.getCorrespondingInAsem(vsum, "oldIsrName",
                edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask.class));
        assertNotNull(util.getCorrespondingInAsem(vsum, "newIsrName",
                edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask.class));
    }

    // P18, P19 — PeriodicTask.period/delay ↔ PeriodicStimulus.recurrence/offset

    @Test
    @DisplayName("P18, P19 – recurrence/offset edited after PeriodicStimulus already attached → PeriodicTask.period/delay re-synced")
    void p18p19_stimulusValueChangedAfterAttachment_periodicTaskUpdated(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "Scheduler");
        util.addTask(vsum, "Scheduler", "tickTask", "PeriodicTask");
        util.addPeriodicStimulus(vsum, "tickTask", 10, 5);

        PeriodicTask periodicTaskBefore = util.getCorrespondingInAsem(vsum, "tickTask", PeriodicTask.class);
        assertEquals(10, periodicTaskBefore.getPeriod());
        assertEquals(5, periodicTaskBefore.getDelay());

        util.setStimulusRecurrence(vsum, "tickTask", 20);
        util.setStimulusOffset(vsum, "tickTask", 15);

        PeriodicTask periodicTaskAfter = util.getCorrespondingInAsem(vsum, "tickTask", PeriodicTask.class);
        assertEquals(20, periodicTaskAfter.getPeriod(), "period must re-sync after a later recurrence edit");
        assertEquals(15, periodicTaskAfter.getDelay(), "delay must re-sync after a later offset edit");
    }
}
