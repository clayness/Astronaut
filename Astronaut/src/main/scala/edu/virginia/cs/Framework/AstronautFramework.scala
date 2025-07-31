package edu.virginia.cs.Framework

import edu.virginia.cs.Framework.Types._

trait AstronautFramework extends Serializable {

  type SpecificationType = DBSpecification
  type ImplementationType = DBImplementation
  type MeasurementFunctionSetType = DBConcreteMeasurementFunctionSet
  type MeasurementResultSetType = DBMeasurementResult
  type FormalSpecificationType = DBFormalSpecification
  type FormalImplementationType = DBFormalImplementation
  type FormalAbstractMeasurementFunctionSet = DBFormalAbstractMeasurementFunctionSet
  type FormalConcreteMeasurementFunctionSet = DBFormalConcreteMeasurementFunctionSet

  def tradespace: Tradespace => SpecificationType => List[(ImplementationType, MeasurementResultSetType)] = {
    (tradespace0: Tradespace) =>
      (spec: SpecificationType) =>
        analyze(tradespace0)(synthesize(tradespace0)(spec))
  }

  private def synthesize: Tradespace => SpecificationType => List[(ImplementationType, MeasurementFunctionSetType)] = {
    case Build_Tradespace(synthesize0, _) => synthesize0
  }

  private def analyze: Tradespace => List[(ImplementationType, MeasurementFunctionSetType)] => List[(ImplementationType, MeasurementResultSetType)] = {
    case Build_Tradespace(_, analyze0) => analyze0
  }

  sealed abstract class Tradespace extends Serializable

  case class Build_Tradespace(x1: SpecificationType => List[(ImplementationType, MeasurementFunctionSetType)],
                              x2: List[(ImplementationType, MeasurementFunctionSetType)] => List[(ImplementationType, MeasurementResultSetType)])
    extends Tradespace with Serializable
}

