package edu.virginia.cs.Framework

import java.io.Serializable

//noinspection ScalaUnusedSymbol
trait AstronautFramework extends Serializable {

  sealed abstract class Prod[A, B] extends Serializable

  case class Pair[A, B](x1: A, x2: B) extends Prod[A, B] with Serializable

  def fst[A, B]: Prod[A, B] => A = {
    case Pair(x, y) => x
  }

  def snd[A, B]: Prod[A, B] => B = {
    case Pair(x, y) => y
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
      case Cons(x, l0) => x
    }
  }

  def tl[A]: List[A] => List[A] = {
    case Nil() => Nil()
    case Cons(a0, m) => m
  }

  def rev[A]: List[A] => List[A] = {
    case Nil() => Nil()
    case Cons(x, l$prime) => app[A](rev(l$prime))(Cons(x, Nil()))
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

  case class Build_Tradespace(x1: Any => List[Prod[Any, Any]], x2: Prod[Any, Any] => Prod[Any, Any], x3: List[Prod[Any, Any]] => List[Prod[Any, Any]]) extends Tradespace with Serializable

  type SpecificationType = Any
  type ImplementationType = Any
  type MeasurementFunctionSetType = Any
  type MeasurementResultSetType = Any

  def synthesize: Tradespace => SpecificationType => List[Prod[ImplementationType, MeasurementFunctionSetType]] = {
    case Build_Tradespace(synthesize0, runBenchmark0, analyze_MyMap0) => synthesize0
  }

  def runBenchmark: Tradespace => Prod[ImplementationType, MeasurementFunctionSetType] => Prod[ImplementationType, MeasurementResultSetType] = {
    case Build_Tradespace(synthesize0, runBenchmark0, analyze_MyMap0) => runBenchmark0
  }

  def analyze_MyMap: Tradespace => List[Prod[ImplementationType, MeasurementFunctionSetType]] => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    case Build_Tradespace(synthesize0, runBenchmark0, analyze_MyMap0) => analyze_MyMap0
  }

  def analyze: Tradespace => List[Prod[ImplementationType, MeasurementFunctionSetType]] => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    (tradespace0: Tradespace) =>
      (input: List[Prod[Any, Any]]) =>
        map[Prod[Any, Any], Prod[Any, Any]](runBenchmark(tradespace0))(input)
  }

  def tradespace: Tradespace => SpecificationType => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    (tradespace0: Tradespace) =>
      (spec: Any) =>
        analyze_MyMap(tradespace0)(synthesize(tradespace0)(spec))
  }

  sealed abstract class ParetoFront extends Serializable

  case class Build_ParetoFront(x1: Tradespace, x2: List[Prod[ImplementationType, MeasurementResultSetType]] => List[Prod[ImplementationType, MeasurementResultSetType]]) extends ParetoFront with Serializable

  def pf_tradespace: ParetoFront => Tradespace = {
    case Build_ParetoFront(pf_tradespace0, paretoFilter0) => pf_tradespace0
  }

  def paretoFilter: ParetoFront => List[Prod[ImplementationType, MeasurementResultSetType]] => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    case Build_ParetoFront(pf_tradespace0, paretoFilter0) => paretoFilter0
  }

  def paretoFront: ParetoFront => SpecificationType => List[Prod[ImplementationType, MeasurementResultSetType]] = {
    (paretoFront0: ParetoFront) =>
      (spec: Any) =>
        paretoFilter(paretoFront0)(tradespace(pf_tradespace(paretoFront0))(spec))
  }

  sealed abstract class Trademaker extends Serializable

  case class Build_Trademaker(x1: Tradespace, x2: ParetoFront, x3: Any => List[Any], x4: Any => Any, x5: Any => Any, x6: Any => List[ImplementationType] => List[Any], x7: SpecificationType => Any, x8: Any => ImplementationType, x9: Any => MeasurementFunctionSetType) extends Trademaker with Serializable

  type FormalSpecificationType = Any
  type FormalImplementationType = Any
  type FormalAbstractMeasurementFunctionSet = Any
  type FormalConcreteMeasurementFunctionSet = Any

  def cFunction: Trademaker => FormalSpecificationType => List[FormalImplementationType] = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => cFunction0
  }

  def aFunction: Trademaker => FormalImplementationType => FormalSpecificationType = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => aFunction0
  }

  def lFunction: Trademaker => FormalSpecificationType => FormalAbstractMeasurementFunctionSet = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => lFunction0
  }

  def tFunction: Trademaker => FormalAbstractMeasurementFunctionSet => List[ImplementationType] => List[FormalConcreteMeasurementFunctionSet] = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => tFunction0
  }

  def sFunction: Trademaker => SpecificationType => FormalSpecificationType = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => sFunction0
  }

  def iFunction: Trademaker => FormalImplementationType => ImplementationType = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => iFunction0
  }

  def bFunction: Trademaker => FormalConcreteMeasurementFunctionSet => MeasurementFunctionSetType = {
    case Build_Trademaker(tm_Tradespace, tm_ParetoFront, cFunction0, aFunction0, lFunction0, tFunction0, sFunction0, iFunction0, bFunction0) => bFunction0
  }
}

