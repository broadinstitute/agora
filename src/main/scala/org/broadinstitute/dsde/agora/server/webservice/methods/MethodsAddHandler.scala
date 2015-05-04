package org.broadinstitute.dsde.agora.server.webservice.methods

import java.util.{Scanner, Date}
import akka.actor.Actor
import com.novus.salat._
import com.novus.salat.global._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.{AgoraAddRequest, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.broadinstitute.wdl.WdlParser
import spray.http.StatusCodes._
import spray.routing.RequestContext

/**
 * Created by dshiga on 5/4/15.
 */
class MethodsAddHandler extends Actor {

  implicit val system = context.system

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest) =>
      try {
        validatePayload(agoraAddRequest)
        add(requestContext, agoraAddRequest)
      } catch {
        case e: WdlParser.SyntaxError => context.parent ! RequestComplete(BadRequest, "Syntax error in payload: " + e.getMessage())
      }
      context.stop(self)
  }

  private def validatePayload(agoraAddRequest: AgoraAddRequest): Unit = {
    val wdlSource = new Scanner(agoraAddRequest.payload).useDelimiter("\\A").next();
    val parser = new WdlParser();
    val tokens = new WdlParser.TokenStream(parser.lex(wdlSource, "wdl string"));
    val ast = parser.parse(tokens).toAst();
  }

  private def add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest): Unit = {
    val agoraEntity = AgoraEntity.fromAgoraAddRequest(agoraAddRequest, createDate = Option(new Date())) 
    val method = AgoraDao.createAgoraDao.insert(agoraEntity)
    context.parent ! grater[AgoraEntity].toJSON(method)
  }
}
