package videosvc.actions;

import play.Logger;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

public class LoggingAction extends play.mvc.Action.Simple {

    private final Logger.ALogger logger = Logger.of(getClass());

    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
        logger.debug("---> " + ctx);
        return delegate.call(ctx);
    }
}