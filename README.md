# AMALTHEA ↔ ASEM

**Vitruv-Based Bidirectional Model Consistency Preservation**

A Maven project implementing bidirectional change propagation between the AMALTHEA metamodel (APP4MC 3.3.0) and the ASEM metamodel (KIT) using the Vitruv framework, based on the consistency preservation rules.

## Table of Contents

- [1. AMALTHEA Metamodel Description](#1-amalthea-metamodel-description)
- [2. ASEM Metamodel Description](#2-asem-metamodel-description)
- [3. Semantic Overlaps and Consistency Preservation Rules](#3-semantic-overlaps-and-consistency-preservation-rules)
- [4. Building and Running](#4-building-and-running)

---

## 1. AMALTHEA Metamodel Description

*APP4MC AMALTHEA — Automotive Software Modelling Framework*

URI: `http://app4mc.eclipse.org/amalthea/3.3.0` | Version: `3.3.0`

### 1.1 Overview

AMALTHEA (APP4MC) is a comprehensive EMF-based metamodel for automotive embedded software. It captures the full software/hardware/OS stack: software tasks, ISRs, runnables, labels, channels, hardware platform topology, operating system configuration, memory mapping, event chains, and timing constraints. AMALTHEA is used as the primary model in the Eclipse APP4MC tool platform.

For the purpose of the AMALTHEA ↔ ASEM consistency case study, the relevant sub-models are the Software Model (`SWModel`), the Components Model (`ComponentsModel`), and the type system (`BaseTypeDefinition`, `Array`).

### 1.2 Scope: Classes Relevant to ASEM Mapping

The full AMALTHEA metamodel is very large. This document focuses on the classes that participate in the semantic overlap with ASEM.

| AMALTHEA Class | Location in Model | Role in Mapping |
|---|---|---|
| **Component** | `ComponentsModel.components` | Maps to ASEM `Module` (Rule 1) |
| **Task** | `SWModel.tasks` | Maps to ASEM Task-like constructs (Rule 2) |
| **ISR** | `SWModel.isrs` | Maps to ASEM `InterruptTask` (Rule 3) |
| **Runnable** | `SWModel.runnables` | Maps to ASEM `Method` (Rule 4) |
| **Label** (constant=F) | `SWModel.labels` | Maps to ASEM `Variable` / `Message` (Rule 6) |
| **Label** (constant=T) | `SWModel.labels` | Maps to ASEM `Constant` / `Parameter` (Rule 5) |
| **BaseTypeDefinition** | `SWModel.typeDefinitions` | Maps to ASEM `PrimitiveType` (Rules 7–9) |
| **Array** | `SWModel.typeDefinitions` | Maps to ASEM `ComposedType` (Rule 10) |

### 1.3 Component

Location: `ComponentsModel.components[]`. The `Component` class represents an architectural software component. It groups runnables, labels, processes, semaphores, and OS events. In the AMALTHEA ↔ ASEM mapping it is the top-level structural unit that maps to an ASEM `Module`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (via `INamed`) | 0..1 | Human-readable name of the component. Propagated to `Module.name`. |
| `ports` | `ComponentPort` | 0..* | Interface ports of the component. Containment. |
| `processes` | `AbstractProcess` | 0..* | Tasks/ISRs that belong to this component. |
| `runnables` | `Runnable` | 0..* | Runnables belonging to this component. Maps to `Module.methods[]`. |
| `labels` | `Label` | 0..* | Labels (shared data variables) belonging to this component. Maps to `Module` elements[`Message`]. |
| `semaphores` | `Semaphore` | 0..* | Semaphores used within this component. |
| `osEvents` | `OsEvent` | 0..* | OS events used within this component. |

### 1.4 Task

Location: `SWModel.tasks[]`. A `Task` is a schedulable unit of execution managed by the OS. It extends `Process`, which extends `AbstractProcess`. Tasks contain a call sequence of runnables via their `ActivityGraph`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (via `INamed`) | 0..1 | Task name. Maps to ASEM `Task.Name`. |
| `stimuli` | `Stimulus` | 0..* | Activation stimuli for this task. |
| `preemption` | `Preemption` (enum) | 0..1 | Preemption type: `preemptive` / `cooperative` / `non_preemptive`. |
| `activityGraph` | `ActivityGraph` | 0..1 | The task's execution graph, containing `RunnableCall`s. Called runnables map to ASEM `Method[]` processes. |
| `size` | `DataSize` | 0..1 | Memory size of this task (inherited from `AbstractMemoryElement`). |
| `multipleTaskActivationLimit` | `EInt` | 0..1 | Max simultaneous activations. Default `0` = unlimited. |

Additional sub-constraints (\*\*\*): `self->collect(processes)->ret.oclIsUndefined()` AND `self->collect(processes)->collect(arguments)->size()=0`.

### 1.5 ISR (Interrupt Service Routine)

Location: `SWModel.isrs[]`. An `ISR` is an interrupt-driven process. It shares the same `Process` supertype as `Task` and maps to ASEM `InterruptTask`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (via `INamed`) | 0..1 | ISR name. Maps to ASEM `InterruptTask.Name`. |
| `category` | `ISRCategory` (enum) | 0..1 | `CATEGORY_1` or `CATEGORY_2`, matching AUTOSAR ISR categories. |
| `stimuli` | `Stimulus` | 0..* | Triggering stimuli. |
| `activityGraph` | `ActivityGraph` | 0..1 | Execution graph with runnable calls. Called runnables map to ASEM `Method[]` processes. |

### 1.6 Runnable

Location: `SWModel.runnables[]`. A `Runnable` is the smallest allocatable unit of code. It corresponds to a function or procedure. In the mapping to ASEM, a `Runnable` becomes a void `Method` with no parameters.

OCL constraints that must hold for the corresponding ASEM `Method`: (1) `self.ret.oclIsUndefined()` — the method has no return type; (2) `self->collect(arguments)->size()=0` — the method has no parameters.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (via `INamed`) | 0..1 | Runnable name. Maps to ASEM `Method.name`. |
| `activityGraph` | `ActivityGraph` | 0..1 | The runnable's execution body (Ticks, LabelAccesses, RunnableCalls, etc.). |
| `parameters` | `RunnableParameter` | 0..* | Formal parameters of the runnable. |
| `activations` | `Activation` | 0..* | Activation sources for the runnable. |
| `section` | `Section` | 0..1 | Memory section placement. |
| `callback` | `EBoolean` | 0..1 | True if this runnable is used as a callback. Default: `false`. |
| `service` | `EBoolean` | 0..1 | True if this runnable is used as a service. Default: `false`. |
| `asilLevel` | `ASILType` (enum) | 0..1 | ASIL safety level: `A`, `B`, `C`, `D`, `QM`. |
| `namespace` | `Namespace` | 0..1 | Optional namespace membership (via `INamespaceMember`). |

### 1.7 Label

Location: `SWModel.labels[]`. A `Label` represents a named, typed piece of shared data. The boolean `constant` attribute is the key discriminator for the ASEM mapping:

- `constant = false` → ASEM `Variable` / `Message` / `Argument` / `Input` / `Output` (Rule 6)
- `constant = true` → ASEM `Parameter` / `Constant` / `Systemconstant` (Rule 5)

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (via `INamed`) | 0..1 | Label name. Maps to ASEM `TypedElement.name`. |
| `dataType` | `TypeDefinition` | 0..1 | The data type of this label. Maps to ASEM `TypedElement.type` (Classifier reference). |
| `constant` | `EBoolean` | 0..1 | `true` = constant label (Rule 5); `false` = variable label (Rule 6). Default: `false`. |
| `bVolatile` | `EBoolean` | 0..1 | Whether the label value is volatile. Default: `false`. |
| `dataStability` | `LabelDataStability` | 0..1 | Data stability protection mode. |
| `stabilityLevel` | `DataStabilityLevel` | 0..1 | Level at which stability is enforced. |
| `section` | `Section` | 0..1 | Memory section of this label. |
| `size` | `DataSize` | 0..1 | Memory size of the label (inherited from `AbstractMemoryElement`). |
| `namespace` | `Namespace` | 0..1 | Optional namespace membership. |

### 1.8 BaseTypeDefinition

Location: `SWModel.typeDefinitions[]`. A `BaseTypeDefinition` describes a primitive data type by name, size, and optional aliases for target environments (e.g. C, AUTOSAR). The size (in bits) determines which ASEM `PrimitiveType` it maps to.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (via `INamed`) | 0..1 | Type name, e.g. `float32`, `uint8`, `bool`. |
| `size` | `DataSize` | 0..1 | Size of the type in bits. The value determines the ASEM mapping: `size=1` → `BooleanType`; `{8,16,32}` → Discrete; `{32,64}` → Continuous. |
| `aliases` | `Alias` | 0..* | Named aliases in target environments. Used to disambiguate `size=32` (`sint32` vs `float32`). |
| `namespace` | `Namespace` | 0..1 | Optional namespace membership. |

| Size Value | ASEM Type | Rule & Condition |
|---|---|---|
| 1 bit | `BooleanType` | Rule 7: `Inv: self.size = 1` |
| 8, 16, 32 bit | `UnsignedDiscreteType` | Rule 8: `Inv: Set{8,16,32}->includes(self.size)` — unsigned alias |
| 8, 16, 32 bit | `SignedDiscreteType` | Rule 8: `Inv: Set{8,16,32}->includes(self.size)` — signed alias |
| 32, 64 bit | `ContinuousType` | Rule 9: `Inv: Set{32,64}->includes(self.size)` — float alias |

### 1.9 Array

Location: `SWModel.typeDefinitions[]` (as a `DataTypeDefinition.dataType`). An `Array` is a compound type specifying a fixed number of elements of a given base type. It maps to ASEM `ComposedType`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `numberElements` | `EInt` | 0..1 | Number of elements in the array. Maps to ASEM `ComposedType` size attribute. |
| `dataType` | `DataType` | 0..1 | The element type of the array. Maps to ASEM `ComposedType.primitiveType` (basicType). |

### 1.10 Supporting Classes (referenced in mappings)

**ActivityGraph** — Contained in `Runnable` and `Process`. Holds a list of `ActivityGraphItem`s (Ticks, LabelAccess, RunnableCall, etc.). When mapping Task→ASEM, the `RunnableCall`s within the `ActivityGraph` represent the "called runnables."

**DataSize** — Value-object holding a `BigInteger` value and a `DataSizeUnit` (bit, kbit, B, kB, …). Used in `BaseTypeDefinition.size` to express type width in bits.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `value` | `EBigInteger` | 0..1 | Numeric size value. Default: `0`. |
| `unit` | `DataSizeUnit` | 0..1 | Unit: bit │ kbit │ Mbit │ B │ kB │ MB │ … |

**Alias** — Used inside `BaseTypeDefinition` to record the name of a type in a specific target environment.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `target` | `EString` | 0..1 | Name of the target environment (e.g. `C`, `AUTOSAR`). |
| `alias` | `EString` | 0..1 | The corresponding type name in that environment (e.g. `float`, `uint8_t`). |

### 1.11 Inheritance Summary (Classes Relevant to Mapping)

| Class | Key Supertypes | Used In Rule(s) |
|---|---|---|
| **Component** | `ReferableBaseObject`, `ITaggable`, `INamed`, `INamespaceMember` | Rule 1 |
| **Task** | `Process` → `AbstractProcess` → `AbstractMemoryElement` → `ReferableBaseObject` | Rule 2 |
| **ISR** | `Process` → `AbstractProcess` → `AbstractMemoryElement` → `ReferableBaseObject` | Rule 3 |
| **Runnable** | `AbstractMemoryElement`, `IExecutable`, `INamespaceMember` | Rule 4 |
| **Label** | `AbstractMemoryElement`, `IDisplayName`, `INamespaceMember` | Rules 5, 6 |
| **BaseTypeDefinition** | `TypeDefinition` → `ReferableBaseObject`, `INamespaceMember` | Rules 7, 8, 9 |
| **Array** | `CompoundType` → `BaseObject`, `DataType` | Rule 10 |

---

## 2. ASEM Metamodel Description

*Autonomous Systems Engineering Model (ASEM)*

URI: `edu.kit.ipd.sdq.metamodels.asem`

### 2.1 Overview

ASEM (Autonomous Systems Engineering Model) is an EMF-based metamodel developed at KIT (Karlsruhe Institute of Technology). It models the structural and behavioural aspects of software components in autonomous embedded systems, capturing elements such as modules, classes, methods, data exchange variables, and primitive types.

The metamodel is organised into four sub-packages: `base`, `classifiers`, `dataexchange`, and `primitivetypes`. The root package (`asem`) also contains a `Dummy` placeholder class.

### 2.2 Package Structure

| Sub-package | URI / nsPrefix | Purpose |
|---|---|---|
| **base** | `edu.kit.ipd.sdq.metamodels.asem.base` | Abstract base concepts: naming, identity, typed elements |
| **classifiers** | `edu.kit.ipd.sdq.metamodels.asem.classifiers` | Classifier hierarchy: `Classifier`, `ComposedType`, `Component`, `Class`, `Module`, `Task`, `InterruptTask`, `InitTask`, `SoftwareTask`, `PeriodicTask`, `TimeTableTask` |
| **dataexchange** | `edu.kit.ipd.sdq.metamodels.asem.dataexchange` | Data exchange elements: `Variable`, `Message`, `Method`, `Parameter`, `ReturnType`, `Constant`, `SystemConstant`, `Argument`, `Input`, `Output` |
| **primitivetypes** | `edu.kit.ipd.sdq.metamodels.asem.primitivetypes` | Primitive type hierarchy: `PrimitiveType`, `ContinuousType`, `UnsignedDiscreteType`, `SignedDiscreteType`, `BooleanType`, `PrimitiveTypeRepository` |

### 2.3 Package: base

**Named** (abstract) — Abstract supertype for all elements that carry a human-readable name.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` | 0..1 | Human-readable name of the element. |

**Identifiable** (abstract) — Abstract supertype for all elements that require a unique string identifier.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `id` | `EString` | 1..1 | Unique identifier. Marked as `iD=true`. Default value: empty string. |

**TypedElement** (abstract) — Combines `Identifiable` and `Named`. Represents any element that has an associated type. Supertypes: `Identifiable`, `Named`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Inherited from `Named`. |
| `id` | `EString` (inherited) | 1..1 | Inherited from `Identifiable`. |
| `type` | `classifiers::Classifier` | 0..1 | Reference to the type classifier of this element. |

### 2.4 Package: classifiers

**Classifier** (abstract) — Abstract base for all type classifiers. Supertype: `Named`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Name of the classifier. |
| `methods` | `dataexchange::Method` | 0..* | Contained methods belonging to this classifier. Opposite: none. Containment reference. |

**ComposedType** — A classifier that wraps a primitive type (e.g. an array element type). Supertype: `Classifier`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `primitiveType` | `primitivetypes::PrimitiveType` | 0..1 | The primitive type that this composed type is based on. |
| `numberElements` | `EInt` | 0..1 | Element count, mirroring AMALTHEA `Array.numberElements` (Rule 10 / P12). Default `0`. |

**Component** (abstract) — Abstract base for software components. Supertypes: `Identifiable`, `Classifier`, `Named`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `id` | `EString` (inherited) | 1..1 | Unique component identifier. |
| `name` | `EString` (inherited) | 0..1 | Component name. |
| `typedElements` | `base::TypedElement` | 0..* | Typed data elements owned by this component (Variables, Messages, Constants, etc.). Containment. |
| `methods` | `dataexchange::Method` | 0..* | Methods (operations) of this component. Inherited from `Classifier`. Containment. |

**Class** — A concrete component modelling an object-oriented class. Supertype: `Component`. Inherits all attributes and references from `Component`. No additional features.

**Module** — A concrete component modelling a software module (e.g. an AUTOSAR software component). Supertype: `Component`. Inherits all attributes and references from `Component`. No additional features. This is the primary mapping target for AMALTHEA `Component`.

**Task** — A schedulable unit of execution, modelling AMALTHEA's `Task`. Supertype: `Classifier`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Task name. |
| `processes` | `dataexchange::Method` | 0..* | Methods called by this Task, populated from AMALTHEA's `Task.activityGraph` → `RunnableCall`s (see Rule 2). |

`Task` is concrete (instantiable directly) but in practice every AMALTHEA `Task` is mapped onto one of its four concrete subtypes below, chosen interactively (see Rule 2 for the mechanism — AMALTHEA has no field that signals which subtype applies, so the choice is asked for at Task-creation time rather than guessed).

**InterruptTask** — The concrete Task subtype for interrupt-driven execution, modelling AMALTHEA's `ISR`. Supertype: `Task`. No additional own features. The mapping to `InterruptTask` is always unconditional (Rule 3) since AMALTHEA already separates `ISR` from `Task` as distinct classes — no selection needed.

**InitTask**, **SoftwareTask**, **TimeTableTask** — three of the remaining four concrete Task subtypes , together with `InterruptTask` above and `PeriodicTask` below making all five. Supertype: `Task`. No additional own features beyond `Task` — added purely to distinguish "kind of Task" without inventing new AMALTHEA-side data.

**PeriodicTask** — Supertype: `Task`. Unlike the three subtypes above, this one does carry its own data.

| Attribute | Type | Multiplicity | Description |
|---|---|---|---|
| `period` | `EInt` | 0..1 | How often the task repeats, in milliseconds. Synced from AMALTHEA's `PeriodicStimulus.recurrence` (a unit-tagged `Time`) — see Rule 2. |
| `delay` | `EInt` | 0..1 | Delay before the first run, in milliseconds. Synced from AMALTHEA's `PeriodicStimulus.offset`. |

Both fields are always expressed in milliseconds regardless of the unit AMALTHEA's `Time` object used (seconds down to picoseconds get converted; sub-millisecond precision is truncated by integer division — acceptable for the schedules this project deals with).

### 2.5 Package: dataexchange

**Variable** — A typed, named data element with read/write flags. Supertype: `base::TypedElement`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Variable name. |
| `type` | `classifiers::Classifier` (inherited) | 0..1 | Type of the variable. |
| `readable` | `EBoolean` | 0..1 | Whether this variable can be read. Default: `false`. |
| `writable` | `EBoolean` | 0..1 | Whether this variable can be written. Default: `false`. |

**Message** — A variable intended for inter-module communication. Supertype: `Variable`. Inherits `readable`, `writable`, `name`, `type`. No additional features beyond `Variable`.

**Method** — An operation with parameters, an optional return type, and local variables. Supertypes: `Identifiable`, `Named`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Method name. |
| `id` | `EString` (inherited) | 1..1 | Method identifier. |
| `parameters` | `Parameter` | 0..* | Input/output parameters of this method. Containment. Opposite: `Parameter.method`. |
| `returnType` | `ReturnType` | 0..1 | Return type of the method. Containment. Opposite: `ReturnType.method`. If null, the method is void. |
| `variables` | `Variable` | 0..* | Local variables declared inside the method. Containment. |

**Parameter** — A method parameter, which is itself a typed variable. Supertype: `Variable`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `position` | `EInt` | 0..1 | Zero-based position of this parameter in the method signature. Default: `0`. |
| `method` | `Method` | 0..1 | Back-reference to the owning method. Opposite: `Method.parameters`. |

**ReturnType** — The return type of a method, represented as a `TypedElement`. Supertype: `base::TypedElement`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `method` | `Method` | 0..1 | Back-reference to the owning method. Opposite: `Method.returnType`. |

**Constant** — A typed element holding a fixed value. Supertype: `base::TypedElement`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `value` | `EString` | 0..1 | The literal constant value. |

**SystemConstant** — A constant that specifically represents a system-level configuration value (as opposed to a plain, arbitrary constant). Supertype: `Constant`. No additional own features beyond `Constant`. Mapping target for Rule 5: a constant=true AMALTHEA `Label` carrying a `Tag` named/typed `"systemConstant"` becomes a `SystemConstant`; otherwise it stays a plain `Constant` (see Rule 5's NOTE for the full discriminator).

**Argument** — A value passed at a specific method-call site. Supertype: `Variable`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Argument name. |
| `type` | `classifiers::Classifier` (inherited) | 0..1 | Type of the argument. |
| `readable` / `writable` | `EBoolean` (inherited) | 0..1 | Inherited from `Variable`, not meaningfully used here. |

Added to the ecore per Rule 6's target list, but **not wired to any reaction** — structurally it corresponds to AMALTHEA's `CallArgument` (`RunnableCall.arguments`, a value at one specific call site), not to `Label` (a general, reusable data element). Wiring it the same way as `Message`/`Input`/`Output` would model the wrong relationship; it needs its own rule keyed off `CallArgument` instead. See Rule 6's NOTE.

**Input**, **Output** — Non-constant data elements distinguished by their access direction. Supertype: `Variable`. No additional own features beyond `Variable`. Mapping target for Rule 6: a constant=false AMALTHEA `Label` accessed only via read `LabelAccess`es becomes `Input`; accessed only via write `LabelAccess`es becomes `Output`; mixed or not-yet-known access stays `Message` (the pre-existing fallback). See Rule 6's NOTE for the retroactive-swap mechanism (a `LabelAccess` is normally added after its `Label`, so most Labels start as `Message` and get reclassified once the access pattern becomes clear).

### 2.6 Package: primitivetypes

**PrimitiveType** (abstract) — Abstract base for all primitive types. Supertype: `classifiers::Classifier`. Inherits `name` and `methods` from `Classifier`.

**ContinuousType** — Represents continuous (floating-point) numeric types, e.g. `float32`, `double64`. Supertype: `PrimitiveType`. Corresponds to AMALTHEA `BaseTypeDefinition` with size ∈ {32, 64}.

**UnsignedDiscreteType** — Represents unsigned integer types, e.g. `uint8`, `uint16`, `uint32`. Supertype: `PrimitiveType`. Corresponds to AMALTHEA `BaseTypeDefinition` with size ∈ {8, 16, 32} where alias indicates unsigned.

**SignedDiscreteType** — Represents signed integer types, e.g. `sint8`, `sint16`, `sint32`. Supertype: `PrimitiveType`. Corresponds to AMALTHEA `BaseTypeDefinition` with size ∈ {8, 16, 32} where alias indicates signed.

**BooleanType** — Represents a boolean (logical) type. Supertype: `PrimitiveType`. Corresponds to AMALTHEA `BaseTypeDefinition` with size = 1.

**PrimitiveTypeRepository** — A named container that holds a collection of primitive types, acting as a library. Supertype: `base::Named`.

| Attribute / Reference | Type | Multiplicity | Description |
|---|---|---|---|
| `name` | `EString` (inherited) | 0..1 | Repository name. |
| `primitiveTypes` | `PrimitiveType` | 0..* | All primitive types in this repository. Containment. |

> [!NOTE]
> This class now corresponds to AMALTHEA's `SWModel` (specifically its `typeDefinitions` list): both are effectively a single, model-wide "library of types" container, one per model. One-directional only (AMALTHEA → ASEM) since `SWModel` is a singleton created once at VSUM setup — there's no "ASEM creates a new one" case to mirror in reverse. `SWModel` has no name of its own (`extends BaseObject`, not `Named`), so the repository's name is fixed (`"PrimitiveTypes"`) rather than copied, the same workaround already used for `ComposedType.name`. `primitiveTypes` (the containment list) is deliberately left empty — every `BooleanType`/`DiscreteType`/`ContinuousType` is already persisted as its own independent resource root, and an EObject can only have one container, so nesting them here too would silently break that existing mechanism. See `AmaltheaToAsem.reactions`, section "SWModel ↔ PrimitiveTypeRepository," for the reaction.

### 2.7 Inheritance Hierarchy Summary

| Class | Direct Supertype(s) | Package |
|---|---|---|
| **Named** (abstract) | — | base |
| **Identifiable** (abstract) | — | base |
| **TypedElement** (abstract) | `Identifiable`, `Named` | base |
| **Classifier** (abstract) | `Named` | classifiers |
| **ComposedType** | `Classifier` | classifiers |
| **Component** (abstract) | `Identifiable`, `Classifier`, `Named` | classifiers |
| **Class** | `Component` | classifiers |
| **Module** | `Component` | classifiers |
| **Task** | `Classifier` | classifiers |
| **InterruptTask** | `Task` | classifiers |
| **InitTask** | `Task` | classifiers |
| **SoftwareTask** | `Task` | classifiers |
| **PeriodicTask** | `Task` | classifiers |
| **TimeTableTask** | `Task` | classifiers |
| **Variable** | `TypedElement` | dataexchange |
| **Message** | `Variable` | dataexchange |
| **Argument** | `Variable` | dataexchange |
| **Input** | `Variable` | dataexchange |
| **Output** | `Variable` | dataexchange |
| **Parameter** | `Variable` | dataexchange |
| **Method** | `Identifiable`, `Named` | dataexchange |
| **ReturnType** | `TypedElement` | dataexchange |
| **Constant** | `TypedElement` | dataexchange |
| **SystemConstant** | `Constant` | dataexchange |
| **PrimitiveType** (abstract) | `Classifier` | primitivetypes |
| **ContinuousType** | `PrimitiveType` | primitivetypes |
| **UnsignedDiscreteType** | `PrimitiveType` | primitivetypes |
| **SignedDiscreteType** | `PrimitiveType` | primitivetypes |
| **BooleanType** | `PrimitiveType` | primitivetypes |
| **PrimitiveTypeRepository** | `Named` | primitivetypes |

---

## 3. Semantic Overlaps and Consistency Preservation Rules

Semantic overlap exists when two metamodels, developed independently for different purposes, describe conceptually equivalent or corresponding information. When both models are kept in a Virtual Single Underlying Model (VSUM) via the Vitruv framework, any change in one model must be automatically propagated to keep the other consistent.

### 3.1 Semantic Overlap Summary

The ten correspondences below cover every row in Table 5.1 of Mazkatli et al. (2016).

| Rule | AMALTHEA | ASEM / Condition |
|---|---|---|
| **R1** | `Component` | `Module` — no OCL filter |
| **R2** | `Task` | `InitTask` / `SoftwareTask` / `PeriodicTask` / `TimeTableTask` — chosen interactively per Task, see §3.2 |
| **R3** | `ISR` | `InterruptTask` — no OCL filter |
| **R4** | `Runnable` | `Method` — `returnType=null` AND `parameters.size=0` |
| **R5** | `Label` (constant=true) | `Parameter` / `Constant` / `Systemconstant` — `self.constant=true` |
| **R6** | `Label` (constant=false) | `Variable` / `Message` / `Argument` / `Input` / `Output` — `self.constant=false` |
| **R7** | `BaseTypeDefinition` | `BooleanType` — `self.size=1` |
| **R8** | `BaseTypeDefinition` | `UnsignedDiscreteType` / `SignedDiscreteType` — `Set{8,16,32}->includes(self.size)` |
| **R9** | `BaseTypeDefinition` | `ContinuousType` — `Set{32,64}->includes(self.size)` |
| **R10** | `Array` | `ComposedType` — no OCL filter |

### 3.2 Correspondence Rules in Detail

#### Rule 1 — Component ↔ Module

- **OCL filter:** none — every `Component` maps to exactly one `Module`

| AMALTHEA | ASEM |
|---|---|
| `Component.name` | `Module.name` |
| `Component.runnables[]` | `Module.methods[]` (see Rule 4) |
| `Component.labels[]` | `Module.typedElements[]` (see footnote *) |

**Sub-constraint \* — Module elements must be Messages**

```ocl
Context asem::classifiers::Module
  Inv: self->collect(elements)->oclIsTypeOf(Message)
  -- Only Message instances in typedElements correspond to Component labels
```

**Sub-constraint \*\* — Module methods must be void with no arguments**

```ocl
Context asem::classifiers::Module
  Inv: self->collect(methods)->ret.oclIsUndefined()
  Inv: self->collect(methods)->collect(arguments)->size()=0
```

#### Rule 2 — Task ↔ InitTask / SoftwareTask / PeriodicTask / TimeTableTask

| AMALTHEA | ASEM |
|---|---|
| `Task.name` | `Task.name` |
| Task priority (via `TaskAllocation`) | reached via a tagged correspondence (`"taskPriority"`), not copied into ASEM |
| `Task.activityGraph` → RunnableCalls **** | `Task.processes[]` |

**Sub-constraint \*\*\* — processes must be void with no arguments**

```ocl
Context Task
  Inv: self->collect(processes)->ret.oclIsUndefined()
  Inv: self->collect(processes)->collect(arguments)->size()=0
```

**Footnote \*\*\*\* — called runnables:** all `Runnable` instances reachable via the Task's `activityGraph.items` of type `RunnableCall`, including ones nested inside `Switch`/`ProbabilitySwitch` entries rather than sitting directly on the `activityGraph`. These become entries in `Task.processes[]`.

> This flags this exact case as a real limitation of MIR — a `RunnableCall` reachable only through nested `CallSequence`/`LabelSwitch`/`ProbabilitySwitch` structures can't be declared in plain MIR without a custom `"..."` . The project doesn't inherit this limitation: `RunnableCallCreated` reacts to the `RunnableCall`'s type directly (not a declared traversal path), and AMALTHEA's own `ActivityGraphItem.getContainingExecutable()` is a derived reference that walks up through arbitrary nesting on its own — confirmed with a test where the call is buried inside a `ProbabilitySwitch` entry (`AmaltheaToAsemTest#r2_runnableCall_nestedInProbabilitySwitch_populatesProcesses`), passing consistently across repeated runs.

> [!NOTE]
> Priority storage in AMALTHEA 3.3 is indirect — it lives as a `SchedulingParameter` on `TaskAllocation`, not a direct attribute of `Task`. Navigation: `MappingModel → taskAllocation → schedulingParameters`. Implemented as a second, tagged Vitruv correspondence (`Task ↔ SchedulingParameter`, tag `"taskPriority"`) rather than copying the value into a new ASEM field — the value is read live from AMALTHEA via the tag when needed.
>
> AMALTHEA's `Task` has four possible ASEM subtypes (`InitTask`/`SoftwareTask`/`PeriodicTask`/`TimeTableTask`) and nothing on `Task` (`preemption`, `multipleTaskActivationLimit`, `stimuli`) reliably signals which one applies — this is the same "mapping one element to many elements" ambiguity . Rather than guess or default to one fixed subtype, the choice is asked for interactively: `TaskCreated`'s routine opens a single-selection dialog (via Vitruv's `UserInteractor`, the same mechanism the pcmjava case study uses for its own component-kind selection in `Java2PcmClassifier.reactions`) listing the four subtypes, and creates whichever one is chosen. In interactive use this is a real prompt to the person driving the change; in automated tests it's answered by a scripted `TestUserInteraction` response (`VSUMRunner.addTask(..., subtypeChoice)`), defaulting to `"SoftwareTask"` when a test doesn't care which subtype it gets. `InterruptTask` (Rule 3) stays unconditional — `ISR` is already a distinct AMALTHEA class, so no selection is needed there.

> [!NOTE]
> **PeriodicTask.period/delay** : synced from AMALTHEA's `PeriodicStimulus.recurrence`/`.offset` the moment a `PeriodicStimulus` is attached to a Task's `stimuli` list, via the DSL's `element X inserted in Type[feature]` trigger (`Process.stimuli` is a plain, non-containment reference — `Stimulus` objects actually live under `StimuliModel` — so this doesn't need `eContainer` navigation the way P11 would). Values are normalized to whole milliseconds regardless of which AMALTHEA `TimeUnit` was used. Reverse direction (ASEM `PeriodicTask.period`/`.delay` changed → AMALTHEA `PeriodicStimulus.recurrence`/`.offset` updated) is a plain two-attribute property sync, always writing back in milliseconds. **Scope limit:** only the moment of attaching a `PeriodicStimulus` is handled on the AMALTHEA→ASEM side — editing `recurrence.value`/`offset.value` on an *already-attached* stimulus afterward is not detected (same class of problem as P11: a nested `Time` object with no way back to its owning Task without `eContainer`). The reverse direction also doesn't auto-create a `PeriodicStimulus` if a `PeriodicTask`'s period/delay is set before any stimulus exists on the AMALTHEA side — same "no auto-create" scope choice as `Argument`.

#### Rule 3 — ISR ↔ InterruptTask

- **OCL filter:** none — every `ISR` maps to exactly one `InterruptTask`

| AMALTHEA | ASEM |
|---|---|
| `ISR.name` | `InterruptTask.name` |
| ISR priority (via `ISRAllocation`) | reached via a tagged correspondence (`"isrPriority"`), not copied into ASEM |
| `ISR.activityGraph` → RunnableCalls **** | `InterruptTask.processes[]` (inherited from `Task`) |

> The same sub-constraint \*\*\* and footnote \*\*\*\* from Rule 2 apply here without change. ISR priority is simpler than Task priority: `ISRAllocation.priority` is a plain int, not a map lookup, so the tagged correspondence points directly at the `ISRAllocation`.

#### Rule 4 — Runnable ↔ Method

```ocl
Context asem::dataexchange::Method
  Inv void_return:  self.returnType.oclIsUndefined()
  Inv no_args:      self.parameters->size() = 0
```

| AMALTHEA | ASEM |
|---|---|
| `Runnable.name` | `Method.name` |

> `RunnableParameter` instances in AMALTHEA do **not** propagate to ASEM `Method` parameters — the OCL constraint explicitly forbids parameters on the `Method` side.

#### Rule 5 — Label (constant=true) ↔ Constant / Systemconstant

```ocl
Context amalthea::Label
  Inv is_constant:  self.constant = true
```

| AMALTHEA | ASEM |
|---|---|
| `Label.name` | `Constant.name` / `SystemConstant.name` |
| `Label.dataType` | `Constant.type` / `SystemConstant.type` (see Rules 7–9) |

> [!NOTE]
> ASEM's `Parameter` class is structurally a `Method`'s formal argument (`position`, `method` opposite reference) — it doesn't represent a stored constant value the way `Constant` does. Treated as a documentation error rather than something to implement against; removed from this rule's target list.
>
> `SystemConstant` is implemented as a real, separate ASEM class (confirmed with the supervisor, no longer a "may be" guess). The discriminator: a constant=true `Label` carrying a `Tag` whose `name` or `tagType` is `"systemConstant"` (AMALTHEA's existing generic `ITaggable` mechanism) becomes a `SystemConstant`; otherwise it stays a plain `Constant`. This convention was chosen because no AMALTHEA field cleanly signals "system" vs. "plain" constant on its own, and the thesis figure (Table 5.1's original source) that might define the intended rule wasn't available.

#### Rule 6 — Label (constant=false) ↔ Variable / Message / Argument / Input / Output

```ocl
Context amalthea::Label
  Inv is_variable:  self.constant = false
```

| AMALTHEA | ASEM |
|---|---|
| `Label.name` | `Message.name` / `Input.name` / `Output.name` |
| `Label.dataType` | `Message.type` / `Input.type` / `Output.type` |
| `Label.labelAccesses[].access` | discriminates the target: read-only → `Input`, write-only → `Output`, mixed or not-yet-known → `Message` |

> [!NOTE]
> `Argument` is added to the ASEM ecore but **not wired** to any Label reaction — it structurally corresponds to AMALTHEA's `CallArgument` (tied to `RunnableCall.arguments`, a value passed at a specific call site), not to `Label` (a general data element). Wiring it into these rules would model the wrong relationship; it needs its own design pass keyed off `CallArgument` instead.
>
> The Input/Output split uses `Label.labelAccesses[].access` (`read`/`write`), an already-modeled AMALTHEA field. Since a `LabelAccess` is normally added *after* the `Label` it references, most Labels initially become `Message` at creation time and get retroactively swapped to `Input`/`Output` once a `LabelAccess` is added and the access pattern becomes unambiguous (read-only or write-only).

> The Rule 1 sub-constraint \* further restricts this: within a `Module`, only `Message` instances (not plain `Variable`) correspond to `Label`s.

#### Rule 7 — BaseTypeDefinition (size=1) ↔ BooleanType

```ocl
Context amalthea::BaseTypeDefinition
  Inv is_bool:  self.size.value = 1
```

| AMALTHEA | ASEM |
|---|---|
| `BaseTypeDefinition.name` | `BooleanType.name` (e.g. `bool`, `boolean`) |

> [!NOTE]
> Reverse direction (ASEM `BooleanType` created → AMALTHEA `BaseTypeDefinition`): size is fixed at 1 bit, not asked for — the OCL constraint above pins it to exactly one valid value, so there's no real decision to make. Contrast with Rules 8/9 below, where the size is genuinely ambiguous and is asked for interactively.

#### Rule 8 — BaseTypeDefinition (size∈{8,16,32}) ↔ UnsignedDiscreteType / SignedDiscreteType

```ocl
Context amalthea::BaseTypeDefinition
  Inv is_discrete:  Set{8,16,32}->includes(self.size.value)
```

| AMALTHEA | ASEM |
|---|---|
| `BaseTypeDefinition.name` | `UnsignedDiscreteType.name` / `SignedDiscreteType.name` |
| `BaseTypeDefinition.aliases` | Signed vs. unsigned disambiguation (prefix `u`/`s`) |

> [!IMPORTANT]
> Size 32 appears in both Rule 8 and Rule 9. Tie-breaker: if the alias contains `float` or `double` → Rule 9 (`ContinuousType`); otherwise → Rule 8 (`DiscreteType`).

> [!NOTE]
> Reverse direction (ASEM `UnsignedDiscreteType`/`SignedDiscreteType` created → AMALTHEA `BaseTypeDefinition`): 8/16/32 bit are all real, different types, and ASEM's classes don't carry a bit-width of their own — genuinely ambiguous, so the size is asked for interactively (Vitruv's `UserInteractor`, same mechanism as Rule 2's Task subtype dialog) rather than defaulted to 32 and silently losing information about narrower types.

#### Rule 9 — BaseTypeDefinition (size∈{32,64}) ↔ ContinuousType

```ocl
Context amalthea::BaseTypeDefinition
  Inv is_continuous:  Set{32,64}->includes(self.size.value)
```

| AMALTHEA | ASEM |
|---|---|
| `BaseTypeDefinition.name` | `ContinuousType.name` (e.g. `float32`, `double64`) |
| `BaseTypeDefinition.aliases` | Alias containing `float`/`double` triggers this mapping |

> [!NOTE]
> Reverse direction (ASEM `ContinuousType` created → AMALTHEA `BaseTypeDefinition`): 32-bit (`float`) and 64-bit (`double`) are genuinely different types with different precision, not one type with cosmetic variation — asked for interactively, same as Rule 8, instead of defaulting to 64 and silently turning every `float` into a `double`.

> [!NOTE]
> Every `BaseTypeDefinition`'s `DataSize` is tagged back to its owner (`"dataSizeOwner"`, the same way used for Task priority in Rule 2) so a later `DataSize.value` change can find its way back without `eContainer`. The routine is idempotent by design (only tears down and rebuilds if the current ASEM type doesn't already match what the current size dispatches to) rather than trying to detect "is this really a later change" — the DSL fires `attribute replaced` identically for a genuine later change and for a value's very first assignment during initial object construction, so there's no reliable way to tell those apart from the event alone. See the comment above `resizeBaseType` in `AmaltheaToAsem.reactions` for the full story, including why an earlier version of this fix (without the idempotency check) broke previously-passing tests.
>
> **Edge case worth knowing about:** the alias — not just the size — decides Discrete vs. Continuous , and a resize never touches the alias, only the size. So a Discrete-family type (no `float`/`double` alias) can never *become* Continuous purely by resizing into 32/64 — it resizes into "nothing matches" instead: the old type is correctly removed, but no replacement gets built, since neither Rule 8 (alias says not-float/double) nor Rule 9 (needs a float/double alias) applies.

#### Rule 10 — Array ↔ ComposedType

- **OCL filter:** none — every `Array` maps to exactly one `ComposedType`

| AMALTHEA | ASEM |
|---|---|
| `Array.numberElements` | `ComposedType.numberElements` |
| `Array.dataType` | `ComposedType.primitiveType` (element type) |


> [!NOTE]
> `ComposedType.numberElements` (`EInt`, added to `asem.ecore`) now implements P12 (see §3.4): the element count is copied on creation in both directions and kept in sync afterwards — changing `Array.numberElements` in AMALTHEA updates the corresponding `ComposedType`, and vice versa.
### 3.3 OCL Invariant Summary

```ocl
-- Rule 4: Runnable ↔ Method
Context asem::dataexchange::Method
  Inv void_return:  self.returnType.oclIsUndefined()
  Inv no_args:      self.parameters->size() = 0

-- Rule 5: Label (constant) ↔ Constant
Context amalthea::Label
  Inv constant_label:  self.constant = true

-- Rule 6: Label (variable) ↔ Variable / Message
Context amalthea::Label
  Inv variable_label:  self.constant = false

-- Rule 7: BaseTypeDefinition ↔ BooleanType
Context amalthea::BaseTypeDefinition
  Inv bool_type:  self.size.value = 1

-- Rule 8: BaseTypeDefinition ↔ DiscreteType
Context amalthea::BaseTypeDefinition
  Inv discrete_type:  Set{8,16,32}->includes(self.size.value)

-- Rule 9: BaseTypeDefinition ↔ ContinuousType
Context amalthea::BaseTypeDefinition
  Inv continuous_type:  Set{32,64}->includes(self.size.value)

-- Rule 1 sub-constraint (*): Module elements must be Messages
Context asem::classifiers::Module
  Inv elements_are_messages:
      self.typedElements->forAll(e | e.oclIsTypeOf(asem::dataexchange::Message))

-- Rule 1 sub-constraint (**): Module methods must be void with no args
Context asem::classifiers::Module
  Inv methods_void:     self.methods->forAll(m | m.returnType.oclIsUndefined())
  Inv methods_no_args:  self.methods->forAll(m | m.parameters->size() = 0)
```

### 3.4 Consistency Preservation Rules

Rules are grouped into four categories: **Existence (E)**, **Property (P)**, **Structural (S)**, and **Completeness (C)**.


#### Existence Rules — element creation and deletion

| ID | Trigger | Action |
|---|---|---|
| E1 | AMALTHEA `Component` created | create ASEM `Module` with same name; add correspondence |
| E2 | AMALTHEA `Component` deleted | delete corresponding ASEM `Module`; remove correspondence |
| E3 | ASEM `Module` created | create AMALTHEA `Component` with same name; add correspondence |
| E4 | ASEM `Module` deleted | delete corresponding AMALTHEA `Component`; remove correspondence |
| E5 | AMALTHEA `Runnable` created | create ASEM `Method` (`returnType=null`, `parameters=[]`) with same name; add to `Module.methods` |
| E6 | AMALTHEA `Runnable` deleted | delete corresponding ASEM `Method` from `Module.methods` |
| E7 | ASEM `Method` created (void, no params) | create AMALTHEA `Runnable` with same name; add to `Component.runnables` |
| E8 | ASEM `Method` deleted | delete corresponding AMALTHEA `Runnable` |
| E9 | AMALTHEA `Label` (constant=false) created | create ASEM `Message` (mixed/no access yet), `Input` (read-only), or `Output` (write-only) — disambiguated via `LabelAccess`, see Rule 6 |
| E10 | AMALTHEA `Label` (constant=true) created | create ASEM `Constant`, or `SystemConstant` if tagged `"systemConstant"` — see Rule 5 |
| E11 | AMALTHEA `Label` deleted | delete whichever of `Message`/`Constant`/`Input`/`Output`/`SystemConstant` currently corresponds |
| E12 | ASEM `Message` created | create AMALTHEA `Label` (constant=false) with same name |
| E13 | ASEM `Constant` created | create AMALTHEA `Label` (constant=true) with same name |
| E14 | AMALTHEA `BaseTypeDefinition` (size=1) created | create ASEM `BooleanType` with same name |
| E15 | AMALTHEA `BaseTypeDefinition` (size∈{8,16,32}) created | create `UnsignedDiscreteType` or `SignedDiscreteType` (disambiguate via alias) |
| E16 | AMALTHEA `BaseTypeDefinition` (size∈{32,64}, float alias) created | create ASEM `ContinuousType` with same name |
| E17 | AMALTHEA `Array` created | create ASEM `ComposedType`; set size from `numberElements` |
| E18 | AMALTHEA `Task` created | interactively create one ASEM `InitTask`/`SoftwareTask`/`PeriodicTask`/`TimeTableTask` — see Rule 2 |
| E19 | AMALTHEA `Task` deleted | delete corresponding ASEM Task subtype |
| E20 | AMALTHEA `ISR` created | create ASEM `InterruptTask` with same name — Rule 3 |
| E21 | AMALTHEA `ISR` deleted | delete corresponding ASEM `InterruptTask` |
| E22 | ASEM `InitTask`/`SoftwareTask`/`PeriodicTask`/`TimeTableTask` created | create AMALTHEA `Task` with same name |
| E23 | ASEM `InitTask`/`SoftwareTask`/`PeriodicTask`/`TimeTableTask` deleted | delete corresponding AMALTHEA `Task` |
| E24 | ASEM `InterruptTask` created | create AMALTHEA `ISR` with same name |
| E25 | ASEM `InterruptTask` deleted | delete corresponding AMALTHEA `ISR` |
| E26 | ASEM `Input` created | create AMALTHEA `Label` (constant=false) with same name |
| E27 | ASEM `Output` created | create AMALTHEA `Label` (constant=false) with same name |
| E28 | ASEM `SystemConstant` created | create AMALTHEA `Label` (constant=true), tagged `"systemConstant"` |
| E29 | ASEM `BooleanType` created | create AMALTHEA `BaseTypeDefinition` (size=1) — reverse of Rule 7 |
| E30 | ASEM `UnsignedDiscreteType`/`SignedDiscreteType` created | create AMALTHEA `BaseTypeDefinition`, size asked interactively — reverse of Rule 8 |
| E31 | ASEM `ContinuousType` created | create AMALTHEA `BaseTypeDefinition`, size asked interactively — reverse of Rule 9 |
| E32 | ASEM `ComposedType` created | create AMALTHEA `Array` — reverse of Rule 10 |
| E33 🆕 | AMALTHEA `SWModel` created | create ASEM `PrimitiveTypeRepository`, named `"PrimitiveTypes"` — not part of R1–R10, see §2.4 |

#### Property Rules — attribute value changes

| ID | Trigger | Action |
|---|---|---|
| P1 | `Component.name` changed | set `Module.name` = new name |
| P2 | `Module.name` changed | set `Component.name` = new name |
| P3 | `Runnable.name` changed | set `Method.name` = new name |
| P4 | `Method.name` changed | set `Runnable.name` = new name |
| P5 | `Label.name` changed | set the corresponding `Message`/`Constant`/`Input`/`Output`/`SystemConstant`'s `.name` = new name |
| P6 | `Message.name` changed | set `Label.name` = new name |
| P7 | `Constant.name` changed | set `Label.name` = new name |
| P8 | `Label.constant` changed false → true | replace ASEM `Message`/`Input`/`Output` with `Constant` or `SystemConstant`; update correspondence |
| P9 | `Label.constant` changed true → false | replace ASEM `Constant`/`SystemConstant` with `Message`/`Input`/`Output`; update correspondence |
| P10 | `Label.dataType` changed | update `Message.type` / `Constant.type` to corresponding ASEM `PrimitiveType` |
| P11 🆕 | `BaseTypeDefinition.size` changed | replace ASEM `PrimitiveType` with type matching new size (re-apply Rules 7–9) — **implemented**, see NOTE on P11 above and in `AmaltheaToAsem.reactions` |
| P12 🆕 | `Array.numberElements` changed | update `ComposedType.numberElements`, and vice versa — **implemented**, both directions |
| P13 | `Task.name` changed | set corresponding ASEM Task subtype's `.name` = new name |
| P14 | `ISR.name` changed | set `InterruptTask.name` = new name |
| P15 | ASEM Task subtype `.name` changed | set AMALTHEA `Task.name` = new name |
| P16 | ASEM `InterruptTask.name` changed | set AMALTHEA `ISR.name` = new name |
| P17 | ASEM `Input`/`Output`/`SystemConstant` `.name` changed | set corresponding `Label.name` = new name |
| P18 🆕 | `PeriodicTask.period` changed | update `PeriodicStimulus.recurrence` (milliseconds) — see NOTE on Rule 2 above |
| P19 🆕 | `PeriodicTask.delay` changed | update `PeriodicStimulus.offset` (milliseconds) |

#### Structural Rules — containment and reference changes

| ID | Trigger | Action |
|---|---|---|
| S1 | `Runnable` added to `Component.runnables` | add corresponding ASEM `Method` to `Module.methods` |
| S2 | `Runnable` removed from `Component.runnables` | remove corresponding ASEM `Method` from `Module.methods` |
| S3 | `Method` added to `Module.methods` | add corresponding AMALTHEA `Runnable` to `Component.runnables` |
| S4 | `Method` removed from `Module.methods` | remove corresponding AMALTHEA `Runnable` from `Component.runnables` |
| S5 | `Label` added to `Component.labels` | add corresponding ASEM element (`Message`/`Constant`/`Input`/`Output`/`SystemConstant`) to `Module.typedElements` |
| S6 | `Label` removed from `Component.labels` | remove corresponding ASEM element from `Module.typedElements` |
| S7 | `Message` added to `Module.typedElements` | add corresponding AMALTHEA `Label` (constant=false) to `Component.labels` |
| S8 | `Constant` added to `Module.typedElements` | add corresponding AMALTHEA `Label` (constant=true) to `Component.labels` |
| S9 | `LabelAccess` inserted into `Label.labelAccesses` | retroactively swap ASEM `Message` ↔ `Input`/`Output` once the read/write pattern becomes known — see Rule 6 |
| S10 🆕 | `PeriodicStimulus` inserted into `Task.stimuli` | sync `PeriodicTask.period`/`delay` from `recurrence`/`offset` — see NOTE on Rule 2 above |

#### Completeness Rules — model-wide invariants

| ID | Invariant |
|---|---|
| C1 | Every `Component` has a corresponding `Module`, and vice versa |
| C2 | Every `Runnable` in a `Component` has a corresponding void no-param `Method` in the corresponding `Module`, and vice versa |
| C3 | Every `Label` (constant=false) in a `Component` has a corresponding `Message`/`Input`/`Output` in `Module.typedElements`, and vice versa |
| C4 | Every `Label` (constant=true) in a `Component` has a corresponding `Constant`/`SystemConstant` in `Module.typedElements`, and vice versa |
| C5 | Every `BaseTypeDefinition` (size=1) has a corresponding `BooleanType`, and vice versa |
| C6 | Every `BaseTypeDefinition` (size∈{8,16,32}) has a corresponding `DiscreteType`, and vice versa |
| C7 | Every `BaseTypeDefinition` (size∈{32,64}, float alias) has a corresponding `ContinuousType`, and vice versa |
| C8 | Every `Array` has a corresponding `ComposedType`; `Array.numberElements` must equal `ComposedType` size, and vice versa |
| C9 | Every `Task` has a corresponding ASEM Task subtype, and vice versa |
| C10 | Every `ISR` has a corresponding `InterruptTask`, and vice versa |
| C11 🆕 | Exactly one `PrimitiveTypeRepository` exists per model, corresponding to the model's `SWModel` |

---

## 4. Building and Running

This is a four-module Maven project. To build all modules, run the tests, and verify the change propagation rules, use:

```bash
mvn clean verify
```

- `clean` removes any previous build artifacts (`target/` directories) across all modules.
- `verify` runs the full lifecycle up through integration tests — compiling, running unit tests (`AmaltheaToAsemTest`, `AsemToAmaltheaTest`), and executing the `VSUMRunner` to validate bidirectional consistency.

Run it from the project root, where the parent `pom.xml` lives:

```bash
cd path/to/project-root
mvn clean verify
```

If a specific module needs to be built in isolation (e.g. while iterating on the Reactions DSL rules), use `-pl` with `-am` to also build its dependencies:

```bash
mvn clean verify -pl <module-name> -am
```

> [!TIP]
> If the build fails on a fresh clone with an MWE2 URI resolver error, make sure the `.genmodel`/`.ecore` files have been generated first (`mvn clean install` on the metamodel module before running `verify` on the full reactor).
