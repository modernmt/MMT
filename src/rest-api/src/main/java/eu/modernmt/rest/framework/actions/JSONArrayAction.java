package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonArray;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

public abstract class JSONArrayAction extends JSONAction {

    @Override
    protected final JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable {
        JsonArray array = execute(req, params);
        return array == null ? null : new JSONArrayActionResult(array);
    }

    protected abstract JsonArray execute(RESTRequest req, Parameters params) throws Throwable;

}
