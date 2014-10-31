package com.socrata.geospace.config

import com.socrata.thirdparty.curator.{CuratorConfig, DiscoveryConfig}
import com.socrata.thirdparty.metrics.MetricsOptions
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
 * Contains configuration values from the application config file
 * @param config Configuration object
 */
class GeospaceConfig(config: Config) {
  val port = config.getInt("geospace.port")
  val gracefulShutdownMs = config.getMilliseconds("geospace.graceful-shutdown-time").toInt
  val maxMultiPolygonComplexity = config.getInt("geospace.max-multipolygon-complexity")
  val cache = config.getConfig("geospace.cache")
  val curatedDomains = config.getStringList("geospace.curated-domains")
  val sodaSuggester = new SodaSuggesterConfig(config.getConfig("geospace.soda-suggester"))

  val curator = new CuratorConfig(config, "curator")
  val discovery = new DiscoveryConfig(config, "service-advertisement")
  val sodaFountain = new CuratedServiceConfig(config.getConfig("soda-fountain"))
  val coreServer = new CuratedServiceConfig(config.getConfig("core-server"))

  val metrics = MetricsOptions(config.getConfig("metrics"))

  val debugString = config.root.render()
}

/**
 * Contains configuration values for getting georegion suggestions through Soda Fountain
 */
class SodaSuggesterConfig(config: Config) {
  val resourceName = config.getString("resource-name")
}

/**
 * Contains configuration values to provision a curated service
 */
class CuratedServiceConfig(config: Config) {
  val serviceName = config.getString("service-name")
  val maxRetries  = config.getInt("max-retries")
}
