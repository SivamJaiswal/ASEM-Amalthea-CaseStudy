package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;

import org.eclipse.app4mc.amalthea.model.Alias;
import org.eclipse.app4mc.amalthea.model.Amalthea;
import org.eclipse.app4mc.amalthea.model.AmaltheaFactory;
import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;
import org.eclipse.app4mc.amalthea.model.Component;
import org.eclipse.app4mc.amalthea.model.DataSize;
import org.eclipse.app4mc.amalthea.model.DataSizeUnit;
import org.eclipse.app4mc.amalthea.model.Label;
import org.eclipse.app4mc.amalthea.model.Runnable;

import mir.reactions.amaltheaToAsem.AmaltheaToAsemChangePropagationSpecification;
import mir.reactions.asemToAmalthea.AsemToAmaltheaChangePropagationSpecification;

import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** Builds the vsum/sample-data baseline used by VSUMExample — run this once before the first interactive run. */
public class VSUMSampleDataGenerator {
  private static final String AMALTHEA_FILE = "/amalthea.amxmi";

  public static void main(String[] args) throws IOException {
    Path storageFolder = Path.of("vsum/sample-data").toAbsolutePath();
    VirtualModel vsum = createDefaultVirtualModel(storageFolder);

    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = AmaltheaFactory.eINSTANCE.createAmalthea();
          root.setComponentsModel(AmaltheaFactory.eINSTANCE.createComponentsModel());
          root.setSwModel(AmaltheaFactory.eINSTANCE.createSWModel());
          v.registerRoot(root, URI.createFileURI(storageFolder + AMALTHEA_FILE));
        });

    // ExampleComponent stays first (index 0) — VSUMExample.main() looks it up by index
    // to attach the interactively-created Task.
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = v.getRootObjects(Amalthea.class).iterator().next();
          Component component = AmaltheaFactory.eINSTANCE.createComponent();
          component.setName("ExampleComponent");
          root.getComponentsModel().getComponents().add(component);
        });

    // SensorComponent — a Runnable (E5) and a plain non-constant Label (E9, Message).
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = v.getRootObjects(Amalthea.class).iterator().next();
          Component sensor = AmaltheaFactory.eINSTANCE.createComponent();
          sensor.setName("SensorComponent");
          root.getComponentsModel().getComponents().add(sensor);

          Runnable runnable = AmaltheaFactory.eINSTANCE.createRunnable();
          runnable.setName("readSensor");
          root.getSwModel().getRunnables().add(runnable);
          sensor.getRunnables().add(runnable);

          Label label = AmaltheaFactory.eINSTANCE.createLabel();
          label.setName("sensorValue");
          label.setConstant(false);
          root.getSwModel().getLabels().add(label);
          sensor.getLabels().add(label);
        });

    // ActuatorComponent — a constant Label (E10, Constant).
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = v.getRootObjects(Amalthea.class).iterator().next();
          Component actuator = AmaltheaFactory.eINSTANCE.createComponent();
          actuator.setName("ActuatorComponent");
          root.getComponentsModel().getComponents().add(actuator);

          Label maxSpeed = AmaltheaFactory.eINSTANCE.createLabel();
          maxSpeed.setName("MAX_SPEED");
          maxSpeed.setConstant(true);
          root.getSwModel().getLabels().add(maxSpeed);
          actuator.getLabels().add(maxSpeed);
        });

    // A signed 16-bit BaseTypeDefinition (E15, SignedDiscreteType).
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = v.getRootObjects(Amalthea.class).iterator().next();
          BaseTypeDefinition btd = AmaltheaFactory.eINSTANCE.createBaseTypeDefinition();
          btd.setName("TemperatureReading");
          DataSize size = AmaltheaFactory.eINSTANCE.createDataSize();
          size.setValue(BigInteger.valueOf(16));
          size.setUnit(DataSizeUnit.BIT);
          btd.setSize(size);
          Alias alias = AmaltheaFactory.eINSTANCE.createAlias();
          alias.setAlias("TemperatureReading");
          alias.setTarget("ASEM");
          btd.getAliases().add(alias);
          root.getSwModel().getTypeDefinitions().add(btd);
        });
  }

  private static VirtualModel createDefaultVirtualModel(Path storageFolder) throws IOException {
    tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondencePackage.eINSTANCE.eClass();
    Iterable<ChangePropagationSpecification> specs =
        List.of(
            new AmaltheaToAsemChangePropagationSpecification(),
            new AsemToAmaltheaChangePropagationSpecification());
    return new VirtualModelBuilder()
        .withStorageFolder(storageFolder)
        .withUserInteractorForResultProvider(
            new TestUserInteraction.ResultProvider(new TestUserInteraction()))
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
