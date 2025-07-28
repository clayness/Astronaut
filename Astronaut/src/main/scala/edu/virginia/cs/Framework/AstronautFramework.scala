package edu.virginia.cs.Framework

trait AstronautFramework extends Serializable {

  sealed abstract class Prod[A, B] extends Serializable

  case class Pair[A, B](x1: A, x2: B) extends Prod[A, B] with Serializable

  def fst[A, B]: Prod[A, B] => A = {
    case Pair(x, _) => x
  }

  def snd[A, B]: Prod[A, B] => B = {
    case Pair(_, y) => y
  }

  sealed abstract class List[A] extends Serializable

  case class Nil[A]() extends List[A] with Serializable

  case class Cons[A](x1: A, x2: List[A]) extends List[A] with Serializable

  def app[A]: List[A] => List[A] => List[A] = {
    (l: List[A]) =>
      (m: List[A]) =>
        l match {
          case Nil() => m
          case Cons(a0, l1) => Cons(a0, app(l1)(m))
        }
  }

  def hd[A]: A => List[A] => A = {
    (default: A) => {
      case Nil() => default
      case Cons(x, _) => x
    }
  }

  def tl[A]: List[A] => List[A] = {
    case Nil() => Nil()
    case Cons(_, m) => m
  }

  def map[A, B]: (A => B) => List[A] => List[B] = {
    (f: A => B) =>
      (l: List[A]) =>
        l match {
          case Nil() => Nil()
          case Cons(a0, t) => Cons(f(a0), map(f)(t))
        }
  }

  def combine[A, B]: List[A] => List[B] => List[Prod[A, B]] = {
    (l: List[A]) =>
      (l$prime: List[B]) =>
        l match {
          case Nil() => Nil()
          case Cons(x, tl0) => l$prime match {
            case Nil() => Nil()
            case Cons(y, tl$prime) => Cons(Pair(x, y), combine(tl0)(tl$prime))
          }
        }
  }

  sealed abstract class Tradespace extends Serializable

  case class Build_Tradespace(x1: Any => List[Prod[AnyRef, Any]], x2: Prod[AnyRef, Any] => Prod[AnyRef, Any], x3: List[Prod[AnyRef, Any]] => List[Prod[AnyRef, Any]]) extends Tradespace with Serializable

  type SpecificationType = Any
  type ImplementationType = AnyRef
  type MeasurementFunctionSetType = Any
  type MeasurementResultSetType = Any

  private def synthesize: Tradespace => SpecificationType => List[Prod[ImplementationType, MeasurementFunctionSetType]] = {
    case Build_Tradespace(synthesize0, _, _) => synthesize0
  }

  private def analyze_MyMap: Tradespace => List[Prod[ImplementationType, MeasurementFunctionSetType]] => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    case Build_Tradespace(_, _, analyze_MyMap0) => analyze_MyMap0
  }

  def tradespace: Tradespace => SpecificationType => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    (tradespace0: Tradespace) =>
      (spec: Any) =>
        analyze_MyMap(tradespace0)(synthesize(tradespace0)(spec))
  }

  sealed abstract class ParetoFront extends Serializable

  case class Build_ParetoFront(x1: Tradespace, x2: List[Prod[ImplementationType, MeasurementResultSetType]] => List[Prod[ImplementationType, MeasurementResultSetType]]) extends ParetoFront with Serializable

  sealed abstract class Trademaker extends Serializable

  case class Build_Trademaker(x1: Tradespace, x2: ParetoFront, x3: Any => List[Any], x4: Any => Any, x5: Any => Any, x6: Any => List[ImplementationType] => List[Any], x7: SpecificationType => Any, x8: Any => ImplementationType, x9: Any => MeasurementFunctionSetType) extends Trademaker with Serializable

  type FormalSpecificationType = Any
  type FormalImplementationType = Any
  type FormalAbstractMeasurementFunctionSet = Any
  type FormalConcreteMeasurementFunctionSet = Any

}

