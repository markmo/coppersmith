package commbank.coppersmith

import commbank.coppersmith.Feature._
import scala.reflect.runtime.universe.TypeTag

object FeatureStub {
  def apply[S : TypeTag, V  <: Value : TypeTag] = new FeatureStub[S, V]
}

/**
  *
  * @tparam S Feature source
  * @tparam V Value type
  */

class FeatureStub[S : TypeTag, V <: Value : TypeTag] {
  def asFeatureMetadata(featureType: Type,
                        namespace: Namespace,
                        name: Name,
                        desc: Description
                       ) = Metadata[S, V](namespace, name, desc, featureType)

}