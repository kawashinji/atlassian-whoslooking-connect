package filters

import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AddResponseHeader extends Filter {
  def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    val result = f(rh)
    result.map(_.withHeaders("Strict-Transport-Security" -> "max-age=31536000; includeSubDomains"))
  }
}