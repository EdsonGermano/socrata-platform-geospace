package com.socrata.geospace.http.curatedregions

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.codec.JsonCodec
import com.rojoma.json.util.{AutomaticJsonCodecBuilder, Strategy, JsonKeyStrategy}
import com.socrata.geospace.http.config.CuratedRegionsConfig
import com.socrata.geospace.lib.client.SodaResponse
import com.socrata.geospace.lib.errors.UnexpectedSodaResponse
import com.socrata.soda.external.SodaFountainClient
import com.socrata.soda.external.SodaFountainClient.Result
import com.vividsolutions.jts.geom.MultiPolygon
import javax.servlet.http.{HttpServletResponse => HttpStatus}
import org.slf4j.LoggerFactory
import scala.util.{Success, Failure, Try}
import com.rojoma.json.v3.conversions._


@JsonKeyStrategy(Strategy.Underscore)
case class Suggestion(resourceName: String, name: String, domain: String)
object Suggestion {
  implicit val codec = AutomaticJsonCodecBuilder[Suggestion]
}

/**
 * Queries a georegion dataset stored in Soda Server
 * to suggest datasets that match the provided criteria.
 * @param sodaFountain    Soda Fountain instance
 * @param config          Config information about georegion suggestion
 */
class CuratedRegionSuggester(sodaFountain: SodaFountainClient, config: CuratedRegionsConfig)
                            extends CuratedRegionSuggesterSoqlizer {
  val logger = LoggerFactory.getLogger(getClass)

  private def getRows(soql: String): Result = sodaFountain.query(config.resourceName, None, Iterable(("$query", soql)))

  /**
   * Sends a SoQL query to Soda Fountain to filter the georegions dataset
   * based on the domain list and bounding shape provided.
   * @param domains        The set of domains that returned georegion datasets can be in.
   *                       Example: data.cityofchicago.gov ; geo.socrata.com
   * @param intersectsWith The multipolygon that returned georegion datasets must overlap with.
   * @return               A list of georegion datasets that match the provided criteria.
   */
  def suggest(domains: Seq[String], intersectsWith: Option[MultiPolygon]): Try[Seq[Suggestion]] = {
    val query = makeQuery(domains, intersectsWith)
    logger.info(s"Querying Soda Fountain resource ${config.resourceName} with query $query")

    for { jValue      <- SodaResponse.check(getRows(query), HttpStatus.SC_OK)
          suggestions <- parseSuggestions(jValue) }
    yield suggestions
  }

  private def parseSuggestions(jValue: JValue): Try[Seq[Suggestion]] =
    JsonCodec[Seq[Suggestion]].decode(jValue.toV2) match {
      case Some(suggestions:Seq[Suggestion]) => Success(suggestions)
      case _  => Failure(UnexpectedSodaResponse("Suggestions could not be parsed out of Soda response JSON", jValue))
    }
}