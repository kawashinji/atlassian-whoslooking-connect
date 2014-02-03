package filters

import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.MDC

object CaptureRequestID extends Filter {
  def apply(f: (RequestHeader) => Future[SimpleResult])(r: RequestHeader): Future[SimpleResult] = {
    MDC.put("requestID", r.headers.get("Heroku-Request-ID").getOrElse("my ID"))
    f(r)
  }
}