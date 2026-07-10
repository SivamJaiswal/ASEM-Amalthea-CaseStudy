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
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.SWModel;

import edu.kit.ipd.sdq.metamodels.asem.AsemFactory;
import edu.kit.ipd.sdq.metamodels.asem.Dummy;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Classifier;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.ComposedType;
import edu.kit.ipd.sdq.metamodels.asem.classifiers.impl.ClassifiersFactoryImpl;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Constant;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Method;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.ReturnType;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.impl.DataexchangeFactoryImpl;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.BooleanType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.ContinuousType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.SignedDiscreteType;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.UnsignedDiscreteType;

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

/**
 * VSUMRunner — mirrors BrakeCaseStudy TestUtil.
 *
 * ASEM root structure:
 *   Dummy (root, registered at asem.asem) contains Module objects.
 *   All ASEM modifications open a view selecting Dummy.class,
 *   navigate to Dummy, then modify its contained Modules.
 *   This keeps all changes in the same tracked resource.
 */
public class VSUMRunner {

    public TestUserInteraction userInteraction = new TestUserInteraction();

    // Fixed URIs — one resource per metamodel, registered once at startup
    private static final String AMALTHEA_FILE = "/amalthea.amxmi";
    private static final String ASEM_FILE      = "/asem.asem";

    // ── VSUM setup ────────────────────────────────────────────────────────────

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

    // ── View helpers ──────────────────────────────────────────────────────────

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

    // ── Correspondence lookup ─────────────────────────────────────────────────

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
        // also check standalone Component/Runnable/Label roots directly (auto-created by
        // AsemToAmalthea.reactions via persistProjectRelative, since none of them have a
        // correspondence/containment path back to the Amalthea root's SWModel/ComponentsModel)
        for (EObject root : getDefaultView(vsum,
                List.of(Component.class, Runnable.class, Label.class)).getRootObjects()) {
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

    // ── AMALTHEA helpers ──────────────────────────────────────────────────────

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

    // ── ASEM helpers — all navigate through Dummy root ────────────────────────

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

    // ── Private utilities ─────────────────────────────────────────────────────

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
