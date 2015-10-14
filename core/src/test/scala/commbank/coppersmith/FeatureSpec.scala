package commbank.coppersmith

import org.scalacheck.Prop.forAll

import org.specs2._
import org.specs2.execute._, Typecheck._
import org.specs2.matcher.TypecheckMatchers._

import Feature._, Value._, Type._
import Metadata.ValueType._
import Arbitraries._

import test.thrift.Customer

object MetadataSpec extends Specification with ScalaCheck { def is = s2"""
  Metadata
  ===========
    All value types are covered $valueTypes
"""

  def valueTypes = forAll { (namespace: Namespace, name: Name, desc: String, fType: Type, value: Value) => {
    /* Actual value is ignored - this primarily exists to make sure a compiler warning
     * is raised if a new value type is added without adding a test for it. Would also
     * good if warning was raised if instance wasn't added to Arbitraries.
     */
    value match {
      case Integral(_) =>
        Metadata[Customer, Integral](namespace, name, desc, fType).valueType must_== IntegralType
      case Decimal(_) =>
        Metadata[Customer, Decimal] (namespace, name, desc, fType).valueType must_== DecimalType
      case Str(_) =>
        Metadata[Customer, Str]     (namespace, name, desc, fType).valueType.must_==(StringType)
    }
  }}
}

object FeatureTypeConversionsSpec extends Specification with ScalaCheck {
  def is = s2"""
  Feature conversions
  ===========
    Integral features convert to continuous and to categorical  $integralConversions
    Decimal features convert to continuous and to categorical  $decimalConversions
    String features cannot convert to continuous  $stringConversions
"""

  def integralConversions = {
    val feature = Patterns.general[Customer, Value.Integral, Value.Integral](
      "ns", "name", "Desc", Type.Categorical, _.id, c => Some(c.age), (customer, ctx) => customer.time
    )
    Seq(
      feature.metadata.featureType === Type.Categorical,
      feature.as(Continuous).metadata.featureType === Type.Continuous,
      feature.as(Categorical).metadata.featureType === Type.Categorical,
      feature.as(Categorical).as(Continuous).metadata.featureType === Type.Continuous
    )
  }

  def decimalConversions = {
    val feature = Patterns.general[Customer, Value.Decimal, Value.Decimal](
      "ns", "name", "Description", Type.Categorical, _.id, c => Some(c.age.toDouble), (customer, ctx) => customer.time
    )
    Seq(
      feature.metadata.featureType === Type.Categorical,
      feature.as(Continuous).metadata.featureType === Type.Continuous,
      feature.as(Categorical).metadata.featureType === Type.Categorical,
      feature.as(Categorical).as(Continuous).metadata.featureType === Type.Continuous
    )
  }

  def stringConversions = {
    val feature = Patterns.general[Customer, Value.Str, Value.Str](
      "ns", "name", "Description", Type.Categorical, _.id, c => Some(c.name), (c, ctx) => c.time
    )
    feature.metadata.featureType === Type.Categorical
    typecheck("feature.as(Continuous)") must not succeed
  }
}

object HydroMetadataSpec extends Specification with ScalaCheck { def is = s2"""
  Metadata.asHydroPsv creates expected Hydro metadata $hydroPsv
"""

  def hydroPsv = forAll { (namespace: Namespace, name: Name, desc: Description, fType: Type, value: Value) => {
    val (metadata, expectedValueType) = value match {
      case Integral(_) => (Metadata[Customer, Integral](namespace, name, desc, fType), "int")
      case Decimal(_)  => (Metadata[Customer, Decimal] (namespace, name, desc, fType), "double")
      case Str(_)      => (Metadata[Customer, Str]     (namespace, name, desc, fType), "string")
    }

    val expectedFeatureType = fType match {
      case Continuous  => "continuous"
      case Categorical => "categorical"
    }

    val hydroMetadata = metadata.asHydroPsv

    hydroMetadata must_==
      s"${namespace.toLowerCase}.${name.toLowerCase}|$expectedValueType|$expectedFeatureType"
  }}
}
