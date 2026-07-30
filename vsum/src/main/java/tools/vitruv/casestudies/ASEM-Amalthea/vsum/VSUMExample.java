package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;

import org.eclipse.app4mc.amalthea.model.Amalthea;
import org.eclipse.app4mc.amalthea.model.AmaltheaFactory;
import org.eclipse.app4mc.amalthea.model.Component;

import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.impl.PrimitivetypesFactoryImpl;

import mir.reactions.amaltheaToAsem.AmaltheaToAsemChangePropagationSpecification;
import mir.reactions.asemToAmalthea.AsemToAmaltheaChangePropagationSpecification;

import tools.vitruv.change.interaction.CliInteractionResultProviderImpl;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** Example how to define and use a VSUM, run interactively — requires a real terminal. */
public class VSUMExample {

  public static void main(String[] args) throws IOException {
    Path storageFolder = Path.of("vsum/sample-data").toAbsolutePath();
    VirtualModel vsum = createDefaultVirtualModel(storageFolder);

    // E18 — adds a Task, which asks (via the CLI) which ASEM Task subtype to use.
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = v.getRootObjects(Amalthea.class).iterator().next();
          Component component = root.getComponentsModel().getComponents().get(0);
          org.eclipse.app4mc.amalthea.model.Task task =
              AmaltheaFactory.eINSTANCE.createTask();
          task.setName("ExampleTask");
          root.getSwModel().getTasks().add(task);
          component.getProcesses().add(task);
        });

    // E30 — creates an UnsignedDiscreteType in ASEM, which asks (via the CLI) the bit size.
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          var type = PrimitivetypesFactoryImpl.eINSTANCE.createUnsignedDiscreteType();
          type.setName("ExampleUnsignedType");
          v.registerRoot(type,
              URI.createFileURI(storageFolder + "/asem/type_ExampleUnsignedType.asem"));
        });

    // E30 — creates a SignedDiscreteType in ASEM, which asks the same bit-size question.
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          var type = PrimitivetypesFactoryImpl.eINSTANCE.createSignedDiscreteType();
          type.setName("ExampleSignedType");
          v.registerRoot(type,
              URI.createFileURI(storageFolder + "/asem/type_ExampleSignedType.asem"));
        });

    // E31 — creates a ContinuousType in ASEM, which asks (via the CLI) 32- or 64-bit.
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          var type = PrimitivetypesFactoryImpl.eINSTANCE.createContinuousType();
          type.setName("ExampleContinuousType");
          v.registerRoot(type,
              URI.createFileURI(storageFolder + "/asem/type_ExampleContinuousType.asem"));
        });
  }

  private static VirtualModel createDefaultVirtualModel(Path storageFolder) throws IOException {
    // Registers the correspondence metamodel before any existing correspondences are loaded from disk.
    tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondencePackage.eINSTANCE.eClass();
    Iterable<ChangePropagationSpecification> specs =
        List.of(
            new AmaltheaToAsemChangePropagationSpecification(),
            new AsemToAmaltheaChangePropagationSpecification());
    return new VirtualModelBuilder()
        .withStorageFolder(storageFolder)
        .withUserInteractorForResultProvider(new CliInteractionResultProviderImpl())
        .withChangePropagationSpecifications(specs)
        .buildAndInitialize();
  }

  private static View getDefaultView(VirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private static void modifyView(
      CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }
}
