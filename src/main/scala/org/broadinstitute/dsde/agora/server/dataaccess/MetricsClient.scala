package org.broadinstitute.dsde.agora.server.dataaccess

import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsObject, JsString}

/** Stub class for sending metrics to ... somewhere.
  * The current implementation of this class simply writes the metrics to Agora's standard log.
  * Metrics are written in JSON. We know the log entry will be sent to Kibana, where the JSON
  * will be parsed and we can query/analyze the data. This is obviously a primitive implementation;
  * at some point we'd want to move to New Relic or other full-featured metrics/alerting system.
  *
  * Hopefully by centralizing metrics logging via this class, it'll be easier to move to a different
  * metrics system in the future.
  *
  */
class MetricsClient extends LazyLogging {

  def recordMetric(metricType: String, metric: JsObject) = {
    val m = JsObject(metric.fields ++ Map("source" -> JsString("Agora"), "metricType" -> JsString(metricType)))

    logger.info(m.compactPrint)
  }

}
