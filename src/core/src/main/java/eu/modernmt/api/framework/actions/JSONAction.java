package eu.modernmt.api.framework.actions;

import com.google.gson.JsonElement;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.data.EmptyCorpusException;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.facade.exceptions.AuthenticationException;
import eu.modernmt.facade.exceptions.TestFailedException;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.RESTResponse;
import eu.modernmt.api.framework.routing.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class JSONAction implements Action {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public final void execute(RESTRequest req, RESTResponse resp) {
        try {
            unsecureExecute(req, resp);
        } catch (TemplateException e) {
            if (logger.isDebugEnabled())
                logger.debug("Template exception while executing action " + this, e);
            resp.resourceNotFound();
        } catch (Parameters.ParameterParsingException e) {
            resp.badRequest(e);
        } catch (UnsupportedLanguageException e) {
            if (logger.isDebugEnabled())
                logger.debug("Language direction '" + e.getLanguageDirection() + "' is not supported " + this, e);
            resp.badRequest(e);
        } catch (AuthenticationException e) {
            if (logger.isDebugEnabled())
                logger.debug("Authentication exception while executing action " + this, e);
            resp.forbidden(e);
        } catch (SystemShutdownException e) {
            if (logger.isDebugEnabled())
                logger.debug("Unable to complete action " + this + ": system is shutting down", e);
            resp.unavailable(e);
        } catch (DecoderUnavailableException | TestFailedException e) {
            resp.unavailable(e);
        } catch (EmptyCorpusException e) {
            if (logger.isDebugEnabled())
                logger.debug("Failed to import empty or poor quality corpus", e);
            resp.badRequest(e);
        } catch (Throwable e) {
            logger.error("Unexpected error: ", e);
            resp.unexpectedError(e);
        }
    }

    private void unsecureExecute(RESTRequest req, RESTResponse resp) throws Throwable {
        Parameters params = getParameters(req);
        JSONActionResult result = getResult(req, params);

        if (result == null) {
            resp.resourceNotFound();
        } else {
            result.beforeDump(req, params);
            JsonElement json = result.dump(this, req, params);

            resp.ok(json);
        }
    }

    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Parameters(req);
    }

    protected abstract JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable;

    protected void decorate(JsonElement element) {
        // Default implementation does nothing
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }

}
