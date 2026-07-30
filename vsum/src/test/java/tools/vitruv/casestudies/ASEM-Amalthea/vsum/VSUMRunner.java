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
import edu.kit.ipd.sdq.metamodels.asem.base.TypedElement;
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

    /** Registers one Amalthea root and one ASEM Dummy root. */
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

    /** Returns a view containing all ASEM roots: the Dummy root plus every standalone Classifier root. */
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

    /** Returns the PrimitiveTypeRepository root. */
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

    /** Creates a Component under the Amalthea root (E1). */
    public String addComponent(VirtualModel vsum, String name) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Component comp = AmaltheaFactory.eINSTANCE.createComponent();
            comp.setName(name);
            getAmaltheaRoot(v).getComponentsModel().getComponents().add(comp);
        });
        return name;
    }

    /** Creates a Runnable under a Component (E5). */
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

    /** Creates a Label under a Component (E9, E10). */
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

    /** Creates a BaseTypeDefinition (E14, E15, E16). */
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

    /** Creates an Array-based DataTypeDefinition (E17). */
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

    /** Creates a Task (E18), defaulting the subtype dialog answer to "SoftwareTask". */
    public String addTask(VirtualModel vsum, String componentName, String taskName) {
        return addTask(vsum, componentName, taskName, "SoftwareTask");
    }

    /** Creates a Task (E18), scripting {@code subtypeChoice} as the Task-subtype dialog answer. */
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

    /** Attaches a PeriodicStimulus (recurrence/offset in milliseconds) to an existing Task (S10). */
    public void addPeriodicStimulus(VirtualModel vsum, String taskName,
                                     int recurrenceMs, int offsetMs) {
        addPeriodicStimulus(vsum, taskName, recurrenceMs, TimeUnit.MS, offsetMs, TimeUnit.MS);
    }

    /** Attaches a PeriodicStimulus with an explicit unit for recurrence and offset (S10). */
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

    /** Changes an existing PeriodicTask's period (ms) in ASEM (P18). */
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

    /** Changes an existing PeriodicTask's delay (ms) in ASEM (P19). */
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

    /** Creates an ISR under a Component (E20). */
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

    /** Adds a RunnableCall to a Task's or ISR's activityGraph, calling the given Runnable (Rule 2 note). */
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

    /** Adds a RunnableCall nested inside a ProbabilitySwitch (Rule 2 note). */
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

    /** Adds a LabelAccess (read or write) from a Runnable to a Label (S9). */
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

    /** Creates a constant=true Label already tagged "systemConstant" in a single transaction (E10). */
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

    /** Adds a Module as a child of the Dummy root (E3). */
    public String addModule(VirtualModel vsum, Path filePath, String name) {
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

    /** Creates a void, no-param Method under a Module (E7). */
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

    /** Creates a Method with a returnType, so E7's OCL guard does not fire. */
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

    /** Creates an Input under a Module (E26). */
    public String addInput(VirtualModel vsum, String moduleName, String inputName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Input input = DataexchangeFactoryImpl.eINSTANCE.createInput();
            input.setName(inputName);
            input.setConstant(false);
            module.getTypedElements().add(input);
        });
        return inputName;
    }

    /** Creates an Output under a Module (E27). */
    public String addOutput(VirtualModel vsum, String moduleName, String outputName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Output output = DataexchangeFactoryImpl.eINSTANCE.createOutput();
            output.setName(outputName);
            output.setConstant(false);
            module.getTypedElements().add(output);
        });
        return outputName;
    }

    /** Creates a SystemConstant under a Module (E28). */
    public String addSystemConstant(VirtualModel vsum, String moduleName, String constantName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            SystemConstant sc = DataexchangeFactoryImpl.eINSTANCE.createSystemConstant();
            sc.setName(constantName);
            sc.setConstant(true);
            module.getTypedElements().add(sc);
        });
        return constantName;
    }

    /** Registers an ASEM Task as its own root (E22). */
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

    /** Registers an ASEM InterruptTask as its own root (E24). */
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

    /** Registers an ASEM InitTask as its own root (E22). */
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

    /** Registers an ASEM SoftwareTask as its own root (E22). */
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

    /** Registers an ASEM PeriodicTask as its own root (E22). */
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

    /** Registers an ASEM TimeTableTask as its own root (E22). */
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

    /** Registers an ASEM BooleanType as its own root (E29). */
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

    /** Creates an UnsignedDiscreteType (E30). Answers the resulting size dialog with "32" (current default). */
    public String addAsemUnsignedDiscreteType(VirtualModel vsum, Path filePath, String name) {
        return addAsemUnsignedDiscreteType(vsum, filePath, name, "32");
    }

    /** Creates an UnsignedDiscreteType (E30), scripting {@code sizeChoice} ("8"/"16"/"32") as the dialog answer. */
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

    /** Creates a SignedDiscreteType (E30). Answers the resulting size dialog with "32" (current default). */
    public String addAsemSignedDiscreteType(VirtualModel vsum, Path filePath, String name) {
        return addAsemSignedDiscreteType(vsum, filePath, name, "32");
    }

    /** Creates a SignedDiscreteType (E30), scripting {@code sizeChoice} ("8"/"16"/"32") as the dialog answer. */
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

    /** Creates a ContinuousType (E31). Answers the resulting size dialog with "64" (current default). */
    public String addAsemContinuousType(VirtualModel vsum, Path filePath, String name) {
        return addAsemContinuousType(vsum, filePath, name, "64");
    }

    /** Creates a ContinuousType (E31), scripting {@code sizeChoice} ("32"/"64") as the dialog answer. */
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

    /** Registers an ASEM ComposedType as its own root (E32). */
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

    /** Creates a Message under a Module (E12). */
    public String addMessage(VirtualModel vsum, String moduleName,
                              String messageName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Message msg = DataexchangeFactoryImpl.eINSTANCE.createMessage();
            msg.setName(messageName);
            msg.setConstant(false);
            module.getTypedElements().add(msg);
        });
        return messageName;
    }

    /** Creates a Constant under a Module (E13). */
    public String addConstant(VirtualModel vsum, String moduleName,
                               String constantName) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            Module module = findModuleInView(v, moduleName);
            Constant c = DataexchangeFactoryImpl.eINSTANCE.createConstant();
            c.setName(constantName);
            c.setConstant(true);
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

    /** Sets TypedElement.constant on an ASEM Message/Constant/Input/Output/SystemConstant (P8, P9). */
    public void setConstantInAsem(VirtualModel vsum, String name,
                                   Class<? extends TypedElement> type, boolean constant) {
        CommittableView view = getAsemView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            TypedElement el = findInAsemView(v, type, name);
            if (el != null)
                el.setConstant(constant);
        });
    }

    /** Sets an attached PeriodicStimulus's recurrence value (P18). */
    public void setStimulusRecurrence(VirtualModel vsum, String taskName, int valueMs) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            org.eclipse.app4mc.amalthea.model.Task task = findByNameAndType(
                    getAmaltheaRoot(v), org.eclipse.app4mc.amalthea.model.Task.class, taskName);
            PeriodicStimulus stimulus = (PeriodicStimulus) task.getStimuli().stream()
                    .filter(s -> s instanceof PeriodicStimulus).findFirst().orElse(null);
            if (stimulus != null) {
                stimulus.getRecurrence().setValue(BigInteger.valueOf(valueMs));
            }
        });
    }

    /** Sets an attached PeriodicStimulus's offset value (P19). */
    public void setStimulusOffset(VirtualModel vsum, String taskName, int valueMs) {
        CommittableView view = getAmaltheaView(vsum).withChangeRecordingTrait();
        modifyView(view, v -> {
            org.eclipse.app4mc.amalthea.model.Task task = findByNameAndType(
                    getAmaltheaRoot(v), org.eclipse.app4mc.amalthea.model.Task.class, taskName);
            PeriodicStimulus stimulus = (PeriodicStimulus) task.getStimuli().stream()
                    .filter(s -> s instanceof PeriodicStimulus).findFirst().orElse(null);
            if (stimulus != null) {
                stimulus.getOffset().setValue(BigInteger.valueOf(valueMs));
            }
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
