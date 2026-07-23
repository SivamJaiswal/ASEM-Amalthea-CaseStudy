package tools.vitruv.methodologist.template.vsum;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import org.eclipse.app4mc.amalthea.model.Amalthea;
import org.eclipse.app4mc.amalthea.model.AmaltheaFactory;
import org.eclipse.app4mc.amalthea.model.Alias;
import org.eclipse.app4mc.amalthea.model.Array;
import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;
import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.DataSize;
import org.eclipse.app4mc.amalthea.model.DataSizeUnit;
import org.eclipse.app4mc.amalthea.model.DataTypeDefinition;
import org.eclipse.app4mc.amalthea.model.IExecutable;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.LabelAccess;
import org.eclipse.app4mc.amalthea.model.LabelAccessEnum;
import org.eclipse.app4mc.amalthea.model.PeriodicStimulus;
import org.eclipse.app4mc.amalthea.model.ProbabilitySwitch;
import org.eclipse.app4mc.amalthea.model.ProbabilitySwitchEntry;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.RunnableCall;
import org.eclipse.app4mc.amalthea.model.SWModel;
import org.eclipse.app4mc.amalthea.model.Tag;
import org.eclipse.app4mc.amalthea.model.Time;
import org.eclipse.app4mc.amalthea.model.TimeUnit;

import edu.kit.ipd.sdq.metamodels.asem.AsemFactory;
import edu.kit.ipd.sdq.metamodels.asem.Dummy;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Classifier;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.InterruptTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.InitTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.SoftwareTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.PeriodicTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.TimeTableTask;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.ComposedType;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Task;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.impl.ClassifiersFactoryImpl;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Constant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Input;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Method;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Output;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.ReturnType;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.SystemConstant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.impl.DataexchangeFactoryImpl;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.BooleanType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.ContinuousType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.PrimitiveTypeRepository;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.SignedDiscreteType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.UnsignedDiscreteType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.impl.PrimitivetypesFactoryImpl;

import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

import mir.reactions.amaltheaToAsem.AmaltheaToAsemChangePropagationSpecification;
import mir.reactions.asemToAmalthea.AsemToAmaltheaChangePropagationSpecification;

/*
 * ASEM root structure: Dummy (root, registered at asem.asem) contains Module objects.
 */
public class VSUMRunner {

    public TestUserInteraction userInteraction = new TestUserInteraction();

    // Fixed URIs — one resource per metamodel, registered once at startup
    private static final String AMALTHEA_FILE = "/amalthea.amxmi";
    private static final String ASEM_FILE      = "/asem.asem";

    //VSUM setup

    public InternalVirtualModel createDefaultVirtualModel(Path projectPath)
            throws IOException {
        Iterable<ChangePropagationSpecification> specs = List.of(
                new AmaltheaToAsemChangePropagationSpecification(),
                new AsemToAmaltheaChangePropagationSpecification());
        InternalVirtualModel model = new VirtualModelBuilder()
                .withStorageFolder(projectPath)
                .withUserInteractorForResultProvider(
                        new TestUserInteraction.ResultProvider(userInteraction))
                .withChangePropagationSpecifications(specs)
                .buildAndInitialize();
        model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
        return model;
    }

    /**
     * Registers one Amalthea root and one ASEM Dummy root.
     * Must be called once per test immediately after createDefaultVirtualModel.
     */
    public void registerRootObjects(VirtualModel vsum, Path filePath) {
        // Register Amalthea root
        CommittableView aView = getDefaultView(vsum, List.of(Amalthea.class))
                .withChangeRecordingTrait();
        modifyView(aView, v -> {
            Amalthea root = AmaltheaFactory.eINSTANCE.createAmalthea();
            root.setComponentsModel(AmaltheaFactory.eINSTANCE.createComponentsModel());
            root.setSwModel(AmaltheaFactory.eINSTANCE.createSWModel());
            v.registerRoot(root,
                    URI.createFileURI(filePath + AMALTHEA_FILE));
        });

        // Register ASEM Dummy root — all Modules are children of Dummy
        CommittableView sView = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(sView, v -> {
            Dummy dummy = AsemFactory.eINSTANCE.createDummy();
            v.registerRoot(dummy,
                    URI.createFileURI(filePath + ASEM_FILE));
        });
    }

    //View helpers 

    public void modifyView(CommittableView view, Consumer<CommittableView> fn) {
        fn.accept(view);
        view.commitChanges();
    }

    public View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
        var selector = vsum.createSelector(
                ViewTypeFactory.createIdentityMappingViewType("default"));
        selector.getSelectableElements().stream()
                .filter(e -> rootTypes.stream().anyMatch(t -> t.isInstance(e)))
                .forEach(e -> selector.setSelected(e, true));
        return selector.createView();
    }

    public View getAmaltheaView(VirtualModel vsum) {
        return getDefaultView(vsum, List.of(Amalthea.class));
    }

    /**
     * Returns a view containing all ASEM roots: the Dummy root plus every standalone
     * Classifier root (Module, ComposedType, BooleanType, Unsigned/SignedDiscreteType,
     * ContinuousType, ...) persisted independently by AmaltheaToAsem.reactions.
     */
    public View getAsemView(VirtualModel vsum) {
        return getDefaultView(vsum, List.of(Dummy.class, Classifier.class));
    }

    public Amalthea getAmaltheaRoot(View view) {
        return view.getRootObjects(Amalthea.class).iterator().next();
    }

    public Dummy getAsemRoot(View view) {
        var dummies = view.getRootObjects(Dummy.class);
        if (!dummies.isEmpty()) return dummies.iterator().next();
        return null;
    }

    /**
     * PrimitiveTypeRepository extends Named, not Classifier, so it doesn't show up in
     * getAsemView's selectable set — needs its own view, same reason getCorrespondingInAmalthea
     * has a fallback search for other independently-persisted roots.
     */
    public PrimitiveTypeRepository getPrimitiveTypeRepository(VirtualModel vsum) {
        var repos = getDefaultView(vsum, List.of(PrimitiveTypeRepository.class))
                .getRootObjects(PrimitiveTypeRepository.class);
        return repos.isEmpty() ? null : repos.iterator().next();
    }

    //Correspondence lookup
    public <T extends EObject> T getCorrespondingInAsem(VirtualModel vsum,
                                                         String sourceName,
                                                         Class<T> targetType) {
        for (EObject root : getAsemView(vsum).getRootObjects()) {
            T found = findByNameAndType(root, targetType, sourceName);
            if (found != null) return found;
        }
        return null;
    }

    public <T extends EObject> T getCorrespondingInAmalthea(VirtualModel vsum,
                                                              String sourceName,
                                                              Class<T> targetType) {
        for (EObject root : getAmaltheaView(vsum).getRootObjects()) {
            T found = findByNameAndType(root, targetType, sourceName);
            if (found != null) return found;
        }
        // also check standalone Component/Runnable/Label/Task/ISR/BaseTypeDefinition/Array roots
        for (EObject root : getDefaultView(vsum,
                List.of(Component.class, Runnable.class, Label.class,
                        org.eclipse.app4mc.amalthea.model.Task.class,
                        org.eclipse.app4mc.amalthea.model.ISR.class,
                        BaseTypeDefinition.class, Array.class)).getRootObjects()) {
            T found = findByNameAndType(root, targetType, sourceName);
            if (found != null) return found;
        }
        return null;
    }

    private <T extends EObject> T findByNameAndType(EObject root,
                                                      Class<T> type, String name) {
        if (type.isInstance(root)) {
            T candidate = type.cast(root);
            String n = getName(candidate);
            if (name == null || name.equals(n)) return candidate;
        }
        for (EObject child : root.eContents()) {
            T result = findByNameAndType(child, type, name);
            if (result != null) return result;
        }
        return null;
    }

    public String getName(EObject obj) {
        try {
            return (String) obj.getClass().getMethod("getName").invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    //AMALTHEA helpers

    public String addComponent(VirtualModel vsum, String name) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Component comp = AmaltheaFactory.eINSTANCE.createComponent();
            comp.setName(name);
            getAmaltheaRoot(v).getComponentsModel().getComponents().add(comp);
        });
        return name;
    }

    public String addRunnable(VirtualModel vsum, String componentName,
                               String runnableName) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Component comp = findByNameAndType(
                    getAmaltheaRoot(v), Component.class, componentName);
            Runnable r = AmaltheaFactory.eINSTANCE.createRunnable();
            r.setName(runnableName);
            getAmaltheaRoot(v).getSwModel().getRunnables().add(r);
            comp.getRunnables().add(r);
        });
        return runnableName;
    }

    public String addLabel(VirtualModel vsum, String componentName,
                            String labelName, boolean constant) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Component comp = findByNameAndType(
                    getAmaltheaRoot(v), Component.class, componentName);
            Label label = AmaltheaFactory.eINSTANCE.createLabel();
            label.setName(labelName);
            label.setConstant(constant);
            getAmaltheaRoot(v).getSwModel().getLabels().add(label);
            comp.getLabels().add(label);
        });
        return labelName;
    }

    public String addBaseTypeDefinition(VirtualModel vsum, String name,
                                         int sizeBits, String alias) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            BaseTypeDefinition btd =
                    AmaltheaFactory.eINSTANCE.createBaseTypeDefinition();
            btd.setName(name);
            DataSize ds = AmaltheaFactory.eINSTANCE.createDataSize();
            ds.setValue(BigInteger.valueOf(sizeBits));
            ds.setUnit(DataSizeUnit.BIT);
            btd.setSize(ds);
            if (alias != null) {
                Alias a = AmaltheaFactory.eINSTANCE.createAlias();
                a.setAlias(alias);
                a.setTarget("ASEM");
                btd.getAliases().add(a);
            }
            getAmaltheaRoot(v).getSwModel().getTypeDefinitions().add(btd);
        });
        return name;
    }

    /** Changes an existing BaseTypeDefinition's size in bits (P11 propagation test). */
    public void changeBaseTypeDefinitionSize(VirtualModel vsum, String name, int newSizeBits) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            BaseTypeDefinition btd = findByNameAndType(
                    getAmaltheaRoot(v), BaseTypeDefinition.class, name);
            if (btd != null) btd.getSize().setValue(BigInteger.valueOf(newSizeBits));
        });
    }

    public String addArrayTypeDefinition(VirtualModel vsum, String name,
                                          int elements) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Array array = AmaltheaFactory.eINSTANCE.createArray();
            array.setNumberElements(elements);
            DataTypeDefinition dtd =
                    AmaltheaFactory.eINSTANCE.createDataTypeDefinition();
            dtd.setName(name);
            dtd.setDataType(array);
            getAmaltheaRoot(v).getSwModel().getTypeDefinitions().add(dtd);
        });
        return name;
    }

    /** Changes an existing Array's numberElements (P12 propagation test). */
    public void setArrayNumberElements(VirtualModel vsum, int newElements) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Array array = findByNameAndType(getAmaltheaRoot(v), Array.class, null);
            if (array != null) array.setNumberElements(newElements);
        });
    }

    /**
     * Creates a Task. Task creation asks (via userInteraction) which ASEM Task
     * subtype to use — defaults to "SoftwareTask" so existing callers don't need
     * to script a response themselves.
     */
    public String addTask(VirtualModel vsum, String componentName, String taskName) {
        return addTask(vsum, componentName, taskName, "SoftwareTask");
    }

    /**
     * Creates a Task, scripting {@code subtypeChoice} ("InitTask" / "SoftwareTask" /
     * "PeriodicTask" / "TimeTableTask") as the answer to the resulting Task-subtype
     * selection dialog.
     */
    public String addTask(VirtualModel vsum, String componentName, String taskName,
                           String subtypeChoice) {
        userInteraction.onNextMultipleChoiceSingleSelection().respondWith(subtypeChoice);
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Component comp = findByNameAndType(
                    getAmaltheaRoot(v), Component.class, componentName);
            org.eclipse.app4mc.amalthea.model.Task task =
                    AmaltheaFactory.eINSTANCE.createTask();
            task.setName(taskName);
            getAmaltheaRoot(v).getSwModel().getTasks().add(task);
            comp.getProcesses().add(task);
        });
        return taskName;
    }

    /** Attaches a PeriodicStimulus (recurrence/offset in milliseconds) to an existing Task. */
    public void addPeriodicStimulus(VirtualModel vsum, String taskName,
                                     int recurrenceMs, int offsetMs) {
        addPeriodicStimulus(vsum, taskName, recurrenceMs, TimeUnit.MS, offsetMs, TimeUnit.MS);
    }

    /** Attaches a PeriodicStimulus with an explicit unit for recurrence and offset. */
    public void addPeriodicStimulus(VirtualModel vsum, String taskName,
                                     int recurrenceValue, TimeUnit recurrenceUnit,
                                     int offsetValue, TimeUnit offsetUnit) {
        CommittableView view = getDefaultView(vsum,
                List.of(Amalthea.class, org.eclipse.app4mc.amalthea.model.Task.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            Amalthea root = getAmaltheaRoot(v);
            if (root.getStimuliModel() == null) {
                root.setStimuliModel(AmaltheaFactory.eINSTANCE.createStimuliModel());
            }
            org.eclipse.app4mc.amalthea.model.Task task = null;
            for (EObject r : v.getRootObjects()) {
                task = findByNameAndType(r, org.eclipse.app4mc.amalthea.model.Task.class, taskName);
                if (task != null) break;
            }
            PeriodicStimulus stimulus = AmaltheaFactory.eINSTANCE.createPeriodicStimulus();
            Time recurrence = AmaltheaFactory.eINSTANCE.createTime();
            recurrence.setValue(BigInteger.valueOf(recurrenceValue));
            recurrence.setUnit(recurrenceUnit);
            stimulus.setRecurrence(recurrence);
            Time offset = AmaltheaFactory.eINSTANCE.createTime();
            offset.setValue(BigInteger.valueOf(offsetValue));
            offset.setUnit(offsetUnit);
            stimulus.setOffset(offset);
            root.getStimuliModel().getStimuli().add(stimulus);
            task.getStimuli().add(stimulus);
        });
    }

    /** Changes an existing PeriodicTask's period (ms) in ASEM (P-rule reverse-sync test). */
    public void setPeriodicTaskPeriod(VirtualModel vsum, String taskName, int periodMs) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            for (EObject root : v.getRootObjects()) {
                PeriodicTask task = findByNameAndType(root, PeriodicTask.class, taskName);
                if (task != null) {
                    task.setPeriod(periodMs);
                    return;
                }
            }
        });
    }

    /** Changes an existing PeriodicTask's delay (ms) in ASEM (P-rule reverse-sync test). */
    public void setPeriodicTaskDelay(VirtualModel vsum, String taskName, int delayMs) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            for (EObject root : v.getRootObjects()) {
                PeriodicTask task = findByNameAndType(root, PeriodicTask.class, taskName);
                if (task != null) {
                    task.setDelay(delayMs);
                    return;
                }
            }
        });
    }

    public String addISR(VirtualModel vsum, String componentName, String isrName) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Component comp = findByNameAndType(
                    getAmaltheaRoot(v), Component.class, componentName);
            org.eclipse.app4mc.amalthea.model.ISR isr =
                    AmaltheaFactory.eINSTANCE.createISR();
            isr.setName(isrName);
            getAmaltheaRoot(v).getSwModel().getIsrs().add(isr);
            comp.getProcesses().add(isr);
        });
        return isrName;
    }

    /** Adds a RunnableCall to a Task's or ISR's activityGraph, calling the given Runnable. */
    public void addRunnableCall(VirtualModel vsum, String callerName, String calleeRunnableName) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            IExecutable caller = findByNameAndType(
                    getAmaltheaRoot(v), org.eclipse.app4mc.amalthea.model.Task.class, callerName);
            if (caller == null) {
                caller = findByNameAndType(
                        getAmaltheaRoot(v), org.eclipse.app4mc.amalthea.model.ISR.class, callerName);
            }
            Runnable callee = findByNameAndType(
                    getAmaltheaRoot(v), Runnable.class, calleeRunnableName);
            if (caller.getActivityGraph() == null) {
                caller.setActivityGraph(AmaltheaFactory.eINSTANCE.createActivityGraph());
            }
            RunnableCall call = AmaltheaFactory.eINSTANCE.createRunnableCall();
            call.setRunnable(callee);
            caller.getActivityGraph().getItems().add(call);
        });
    }

    /**
     * Adds a RunnableCall buried two levels deep inside a ProbabilitySwitch
     * (Task/ISR.activityGraph -> ProbabilitySwitch -> ProbabilitySwitchEntry -> RunnableCall),
     * instead of directly on the Task/ISR's activityGraph. Used to verify whether
     * AMALTHEA's derived ActivityGraphItem.containingExecutable resolves correctly
     * through nested call structures, not just flat ones.
     */
    public void addNestedRunnableCall(VirtualModel vsum, String callerName, String calleeRunnableName) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            IExecutable caller = findByNameAndType(
                    getAmaltheaRoot(v), org.eclipse.app4mc.amalthea.model.Task.class, callerName);
            if (caller == null) {
                caller = findByNameAndType(
                        getAmaltheaRoot(v), org.eclipse.app4mc.amalthea.model.ISR.class, callerName);
            }
            Runnable callee = findByNameAndType(
                    getAmaltheaRoot(v), Runnable.class, calleeRunnableName);
            if (caller.getActivityGraph() == null) {
                caller.setActivityGraph(AmaltheaFactory.eINSTANCE.createActivityGraph());
            }
            ProbabilitySwitch probSwitch = AmaltheaFactory.eINSTANCE.createProbabilitySwitch();
            ProbabilitySwitchEntry entry = AmaltheaFactory.eINSTANCE.createProbabilitySwitchEntry();
            entry.setProbability(1.0);
            RunnableCall call = AmaltheaFactory.eINSTANCE.createRunnableCall();
            call.setRunnable(callee);

            entry.getItems().add(call);
            probSwitch.getEntries().add(entry);
            caller.getActivityGraph().getItems().add(probSwitch);
        });
    }

    /** Adds a LabelAccess (read or write) from a Runnable to a Label. */
    public void addLabelAccess(VirtualModel vsum, String runnableName, String labelName, boolean isRead) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Runnable runnable = findByNameAndType(
                    getAmaltheaRoot(v), Runnable.class, runnableName);
            Label label = findByNameAndType(
                    getAmaltheaRoot(v), Label.class, labelName);
            if (runnable.getActivityGraph() == null) {
                runnable.setActivityGraph(AmaltheaFactory.eINSTANCE.createActivityGraph());
            }
            LabelAccess access = AmaltheaFactory.eINSTANCE.createLabelAccess();
            access.setData(label);
            access.setAccess(isRead ? LabelAccessEnum.READ : LabelAccessEnum.WRITE);
            runnable.getActivityGraph().getItems().add(access);
        });
    }

    /**
     * Creates a constant=true Label already tagged "systemConstant" in a single transaction.
     * The tag must be present BEFORE the Label is first tracked, since the SystemConstant
     * discriminator reacts on Label creation, not on a later Tag addition — creating first
     * and tagging afterward (as two separate commits) would create a plain Constant first.
     */
    public String addSystemConstantTaggedLabel(VirtualModel vsum, String componentName, String labelName) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Amalthea root = getAmaltheaRoot(v);
            if (root.getCommonElements() == null) {
                root.setCommonElements(AmaltheaFactory.eINSTANCE.createCommonElements());
            }
            Component comp = findByNameAndType(root, Component.class, componentName);
            Label label = AmaltheaFactory.eINSTANCE.createLabel();
            label.setName(labelName);
            label.setConstant(true);
            Tag tag = AmaltheaFactory.eINSTANCE.createTag();
            tag.setName("systemConstant");
            root.getCommonElements().getTags().add(tag);
            label.getTags().add(tag);
            root.getSwModel().getLabels().add(label);
            comp.getLabels().add(label);
        });
        return labelName;
    }

    public void renameInAmalthea(VirtualModel vsum, String oldName,
                                   Class<? extends EObject> type, String newName) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            EObject el = findByNameAndType(getAmaltheaRoot(v), type, oldName);
            setName(el, newName);
        });
    }

    public void setConstant(VirtualModel vsum, String labelName, boolean constant) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Label label = findByNameAndType(
                    getAmaltheaRoot(v), Label.class, labelName);
            if (label != null) label.setConstant(constant);
        });
    }

    public void deleteFromAmalthea(VirtualModel vsum, String name,
                                    Class<? extends EObject> type) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            EObject el = findByNameAndType(getAmaltheaRoot(v), type, name);
            if (el != null)
                EcoreUtil.remove(el);
        });
    }

    //ASEM helpers — all navigate through Dummy root

    /**
     * Adds a Module as a child of the Dummy root.
     * All subsequent ASEM operations find this Module via getAsemView → Dummy.
     */
    public String addModule(VirtualModel vsum, Path filePath, String name) {
        // Register Module as its own root — ASEM has no container class
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = ClassifiersFactoryImpl.eINSTANCE.createModule();
            module.setName(name);
            v.registerRoot(module,
                    URI.createFileURI(filePath.toString() + "/asem_" + name + ".asem"));
        });
        return name;
    }

    public String addVoidMethod(VirtualModel vsum, String moduleName,
                                 String methodName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Method method = DataexchangeFactoryImpl.eINSTANCE.createMethod();
            method.setName(methodName);
            // returnType null + parameters empty = Rule 4 OCL invariant
            module.getMethods().add(method);
        });
        return methodName;
    }

    public String addVoidMethodWithReturnType(VirtualModel vsum,
                                               String moduleName, String methodName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Method method = DataexchangeFactoryImpl.eINSTANCE.createMethod();
            method.setName(methodName);
            method.setReturnType(
                    DataexchangeFactoryImpl.eINSTANCE.createReturnType());
            module.getMethods().add(method);
        });
        return methodName;
    }

    public String addInput(VirtualModel vsum, String moduleName, String inputName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Input input = DataexchangeFactoryImpl.eINSTANCE.createInput();
            input.setName(inputName);
            module.getTypedElements().add(input);
        });
        return inputName;
    }

    public String addOutput(VirtualModel vsum, String moduleName, String outputName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Output output = DataexchangeFactoryImpl.eINSTANCE.createOutput();
            output.setName(outputName);
            module.getTypedElements().add(output);
        });
        return outputName;
    }

    public String addSystemConstant(VirtualModel vsum, String moduleName, String constantName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            SystemConstant sc = DataexchangeFactoryImpl.eINSTANCE.createSystemConstant();
            sc.setName(constantName);
            module.getTypedElements().add(sc);
        });
        return constantName;
    }

    /** Registers an ASEM Task as its own root — mirrors addModule, since ASEM has no container class. */
    public String addAsemTask(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            Task task = ClassifiersFactoryImpl.eINSTANCE.createTask();
            task.setName(name);
            v.registerRoot(task,
                    URI.createFileURI(filePath.toString() + "/asem_task_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemInterruptTask(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            InterruptTask interruptTask = ClassifiersFactoryImpl.eINSTANCE.createInterruptTask();
            interruptTask.setName(name);
            v.registerRoot(interruptTask,
                    URI.createFileURI(filePath.toString() + "/asem_interrupttask_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemInitTask(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            InitTask initTask = ClassifiersFactoryImpl.eINSTANCE.createInitTask();
            initTask.setName(name);
            v.registerRoot(initTask,
                    URI.createFileURI(filePath.toString() + "/asem_inittask_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemSoftwareTask(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            SoftwareTask softwareTask = ClassifiersFactoryImpl.eINSTANCE.createSoftwareTask();
            softwareTask.setName(name);
            v.registerRoot(softwareTask,
                    URI.createFileURI(filePath.toString() + "/asem_softwaretask_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemPeriodicTask(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            PeriodicTask periodicTask = ClassifiersFactoryImpl.eINSTANCE.createPeriodicTask();
            periodicTask.setName(name);
            v.registerRoot(periodicTask,
                    URI.createFileURI(filePath.toString() + "/asem_periodictask_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemTimeTableTask(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            TimeTableTask timeTableTask = ClassifiersFactoryImpl.eINSTANCE.createTimeTableTask();
            timeTableTask.setName(name);
            v.registerRoot(timeTableTask,
                    URI.createFileURI(filePath.toString() + "/asem_timetabletask_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemBooleanType(VirtualModel vsum, Path filePath, String name) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            BooleanType type = PrimitivetypesFactoryImpl.eINSTANCE.createBooleanType();
            type.setName(name);
            v.registerRoot(type,
                    URI.createFileURI(filePath.toString() + "/asem_type_" + name + ".asem"));
        });
        return name;
    }

    /** Creates an UnsignedDiscreteType. Answers the resulting size dialog with "32" (current default). */
    public String addAsemUnsignedDiscreteType(VirtualModel vsum, Path filePath, String name) {
        return addAsemUnsignedDiscreteType(vsum, filePath, name, "32");
    }

    /** Creates an UnsignedDiscreteType, scripting {@code sizeChoice} ("8"/"16"/"32") as the dialog answer. */
    public String addAsemUnsignedDiscreteType(VirtualModel vsum, Path filePath, String name, String sizeChoice) {
        userInteraction.onNextMultipleChoiceSingleSelection().respondWith(sizeChoice);
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            UnsignedDiscreteType type = PrimitivetypesFactoryImpl.eINSTANCE.createUnsignedDiscreteType();
            type.setName(name);
            v.registerRoot(type,
                    URI.createFileURI(filePath.toString() + "/asem_type_" + name + ".asem"));
        });
        return name;
    }

    /** Creates a SignedDiscreteType. Answers the resulting size dialog with "32" (current default). */
    public String addAsemSignedDiscreteType(VirtualModel vsum, Path filePath, String name) {
        return addAsemSignedDiscreteType(vsum, filePath, name, "32");
    }

    /** Creates a SignedDiscreteType, scripting {@code sizeChoice} ("8"/"16"/"32") as the dialog answer. */
    public String addAsemSignedDiscreteType(VirtualModel vsum, Path filePath, String name, String sizeChoice) {
        userInteraction.onNextMultipleChoiceSingleSelection().respondWith(sizeChoice);
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            SignedDiscreteType type = PrimitivetypesFactoryImpl.eINSTANCE.createSignedDiscreteType();
            type.setName(name);
            v.registerRoot(type,
                    URI.createFileURI(filePath.toString() + "/asem_type_" + name + ".asem"));
        });
        return name;
    }

    /** Creates a ContinuousType. Answers the resulting size dialog with "64" (current default). */
    public String addAsemContinuousType(VirtualModel vsum, Path filePath, String name) {
        return addAsemContinuousType(vsum, filePath, name, "64");
    }

    /** Creates a ContinuousType, scripting {@code sizeChoice} ("32"/"64") as the dialog answer. */
    public String addAsemContinuousType(VirtualModel vsum, Path filePath, String name, String sizeChoice) {
        userInteraction.onNextMultipleChoiceSingleSelection().respondWith(sizeChoice);
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            ContinuousType type = PrimitivetypesFactoryImpl.eINSTANCE.createContinuousType();
            type.setName(name);
            v.registerRoot(type,
                    URI.createFileURI(filePath.toString() + "/asem_type_" + name + ".asem"));
        });
        return name;
    }

    public String addAsemComposedType(VirtualModel vsum, Path filePath, String name) {
        return addAsemComposedType(vsum, filePath, name, 0);
    }

    public String addAsemComposedType(VirtualModel vsum, Path filePath, String name,
                                       int numberElements) {
        CommittableView view = getDefaultView(vsum, List.of(Dummy.class))
                .withChangeRecordingTrait();
        modifyView(view, v -> {
            ComposedType composedType = ClassifiersFactoryImpl.eINSTANCE.createComposedType();
            composedType.setName(name);
            composedType.setNumberElements(numberElements);
            v.registerRoot(composedType,
                    URI.createFileURI(filePath.toString() + "/asem_composedtype_" + name + ".asem"));
        });
        return name;
    }

    /** Changes an existing ComposedType's numberElements (P12 propagation test). */
    public void setComposedTypeNumberElements(VirtualModel vsum, String composedTypeName,
                                               int newElements) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            for (EObject root : v.getRootObjects()) {
                ComposedType composedType = findByNameAndType(
                        root, ComposedType.class, composedTypeName);
                if (composedType != null) {
                    composedType.setNumberElements(newElements);
                    return;
                }
            }
        });
    }

    public String addMessage(VirtualModel vsum, String moduleName,
                              String messageName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Message msg = DataexchangeFactoryImpl.eINSTANCE.createMessage();
            msg.setName(messageName);
            module.getTypedElements().add(msg);
        });
        return messageName;
    }

    public String addConstant(VirtualModel vsum, String moduleName,
                               String constantName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Constant c = DataexchangeFactoryImpl.eINSTANCE.createConstant();
            c.setName(constantName);
            module.getTypedElements().add(c);
        });
        return constantName;
    }

    public void renameInAsem(VirtualModel vsum, String oldName,
                              Class<? extends EObject> type, String newName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            EObject el = findInAsemView(v, type, oldName);
            setName(el, newName);
        });
    }

    public void deleteFromAsem(VirtualModel vsum, String name,
                                Class<? extends EObject> type) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            EObject el = findInAsemView(v, type, name);
            if (el != null)
                EcoreUtil.remove(el);
        });
    }

    //Private utilities

    /** Scans all roots in the ASEM view for a Module with the given name. */
    private Module findModuleInView(CommittableView v, String name) {
        for (EObject root : v.getRootObjects()) {
            Module m = findByNameAndType(root, Module.class, name);
            if (m != null) return m;
        }
        return null;
    }

    /** Scans all roots in the ASEM view for an element of the given type and name. */
    private <T extends EObject> T findInAsemView(CommittableView v,
                                                   Class<T> type, String name) {
        for (EObject root : v.getRootObjects()) {
            T el = findByNameAndType(root, type, name);
            if (el != null) return el;
        }
        return null;
    }

        private void setName(EObject obj, String name) {
        if (obj == null) return;
        try {
            obj.getClass().getMethod("setName", String.class).invoke(obj, name);
        } catch (Exception e) {
            throw new RuntimeException("setName failed on " + obj, e);
        }
    }
}
