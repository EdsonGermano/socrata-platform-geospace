package com.socrata.geospace.regioncache

import com.rojoma.json.ast.JString
import com.typesafe.config.Config
import com.socrata.geospace.client.GeoToSoda2Converter
import com.socrata.geospace.regioncache.SpatialIndex.Entry
import com.socrata.thirdparty.geojson.FeatureJson
import org.geoscript.feature._
import scala.util.Success

class SpatialRegionCache(config: Config) extends RegionCache[SpatialIndex[Int]](config) {

  /**
   * Generates a SpatialIndex for the dataset given the set of features
   * @param features Features from which to generate a SpatialIndex
   * @return SpatialIndex containing the dataset features
   */
  override def getEntryFromFeatures(features: Seq[Feature]): SpatialIndex[Int] = SpatialIndex(features)

  /**
   * Generates a SpatialIndex for the dataset given feature JSON
   * @param features Feature JSON from which to generate a SpatialIndex
   * @return SpatialIndex containing the dataset features
   */
  override def getEntryFromFeatureJson(features: Seq[FeatureJson]): SpatialIndex[Int] = {
    logger.info("Converting {} features to SpatialIndex entries...", features.length.toString())
    var i = 0
    val entries = features.flatMap { case FeatureJson(properties, geometry, _) =>
      val entryOpt = properties.get(GeoToSoda2Converter.FeatureIdColName).
        collect { case JString(id) => Entry(geometry, id.toInt) }
      if (!entryOpt.isDefined) logger.warn("dataset feature with missing feature ID property")
      i += 1
      if (i % 1000 == 0) depressurize()
      entryOpt
    }
    new SpatialIndex(entries)
  }

  /**
   * Returns indices in descending order of size by # of coordinates
   * @return Indices in descending order of size by # of coordinates
   */
  override def indicesBySizeDesc(): Seq[(String, Int)] = {
    cache.keys.toSeq.map(key => (key, cache.get(key).get.value)).
      collect { case (key, Some(Success(index))) => (key.toString, index.numCoordinates) }.
      sortBy(_._2).
      reverse
  }

  /**
   * Returns a list of regions as tuples of the form (regionName, numCoordinates)
   * in order from the biggest to the smallest.
   */
  def regions: Seq[(String, Int)] = indicesBySizeDesc()
}
