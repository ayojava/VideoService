package videosvc;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;

public class Global extends GlobalSettings {

    private final Logger.ALogger l = Logger.of(getClass());


    @Override
    public void beforeStart(Application app) {
        l.debug("===> WebService starting up ...");
    }

    @Override
    public void onStart(Application app) {
        l.debug("===> WebService has started");
    }

    @Override
    public void onStop(Application app) {
        l.debug("<=== WebService shutting down ...\n");
    }

    @Override
    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        l.error("! Exception while processing request: " + request);
        l.error(t.getClass().getSimpleName() + ": message = " + t.getMessage(), t);
        return super.onError(request, t);
    }

    @Override
    public Action onRequest(Http.Request request, Method actionMethod) {

        String paramList = parameterList(actionMethod);
        l.debug("---> Request (" + request.method() + " " + request.path() + ") => "
                + actionMethod.getName() + "(" + paramList + ")");

        showRequestForAddVideo(request, actionMethod);

        return super.onRequest(request, actionMethod);
    }

    private void showRequestForAddVideo(Http.Request request, Method actionMethod) {
        if (actionMethod.getName().equals("addVideo")) {        // --nonExistingMethod
            showRequest(request);
        }
    }

    private void showRequest(Http.Request request) {
        l.debug("------------------------------------------------------------------");
        Set<Map.Entry<String, String[]>> headers = request.headers().entrySet();
        for (Map.Entry<String, String[]> header : headers) {
            String key = header.getKey();
            String[] values = header.getValue();
            for (String value : values) {
                l.debug("HEADER - " + key + ": " + value);
            }
        }
//        request.headers().entrySet().forEach(hs -> {
//            for (String value : hs.getValue()) {
//                l.debug("HEADER - " + hs.getKey() + ": " + value);
//            }
//        });
//        request.headers().entrySet().forEach(hs -> {
//            Arrays.stream(hs.getValue()).forEach(v -> l.debug("HEADER - " + hs.getKey() + ": " + v));
//        });
        l.debug("BODY: " + request.body().toString());
        l.debug("------------------------------------------------------------------");
    }

    private String parameterList(Method actionMethod) {

        int count = actionMethod.getParameterCount();
        Class<?>[] types = actionMethod.getParameterTypes();
        Parameter[] params = actionMethod.getParameters();

        String paramList = "";
        for ( int i = 0; i < count; i++) {
            paramList += params[i].getType();   // + " " + params[i].getName();
            if (i < params.length - 1) {
                paramList += ", ";
            }
        }

        return paramList;
    }
}