package com.socrata.geospace.regioncache

import com.socrata.thirdparty.geojson.FeatureJson
import com.typesafe.config.Config
import org.geoscript.feature.Feature
import com.socrata.geospace.client.GeoToSoda2Converter
import com.rojoma.json.ast.{JNumber, JString}
import scala.util.Success

class MapRegionCache(config: Config) extends RegionCache[Map[String, Int]](config) {

  /**
   * Generates a SpatialIndex for the dataset given the set of features
   * @param features Features from which to generate a SpatialIndex
   * @return SpatialIndex containing the dataset features
   */
  // TODO : This is only used in a test route, leaving unimplemented for now.
  override def getEntryFromFeatures(features: Seq[Feature], keyName: String): Map[String, Int] = ???

  /**
   * Generates an in-memory map for the dataset from feature JSON, keyed off the specified field
   * @param features Feature JSON from which to generate a map
   * @return Map containing the dataset features
   */
  override def getEntryFromFeatureJson(features: Seq[FeatureJson], keyName: String): Map[String, Int] =
   features.flatMap { case FeatureJson(properties, _, _) =>
     properties.get(keyName).flatMap { case JString(key) =>
       properties.get(GeoToSoda2Converter.FeatureIdColName).collect { case JString(id) =>
         key -> id.toInt
       }
     }
   }.toMap

  /**
   * Returns indices in descending order of size by # of features
   * @return Indices in descending order of size by # of features
   */
  override def indicesBySizeDesc(): Seq[(RegionCacheKey, Int)] =
    cache.keys.toSeq.map(key => (key, cache.get(key).get.value)).
      collect { case (key: RegionCacheKey, Some(Success(map))) => (key, map.size) }.
      sortBy(_._2).
      reverse
}
