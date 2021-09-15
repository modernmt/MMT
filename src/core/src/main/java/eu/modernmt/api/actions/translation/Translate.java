package eu.modernmt.api.actions.translation;

import eu.modernmt.api.actions.util.ContextUtils;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.model.TranslationResponse;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.tags.format.InputFormat;

import java.util.UUID;
import java.util.Vector;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translate", method = HttpMethod.GET)
public class Translate extends ObjectAction<TranslationResponse> {

    public static final int MAX_QUERY_LENGTH = 5000;

    @Override
    protected TranslationResponse execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, DecoderException, ProcessingException {
        Params params = (Params) _params;

        TranslationResponse result = new TranslationResponse(params.priority);
        result.verbose = params.verbose;

        Preprocessor.Options processing = new Preprocessor.Options();
        processing.format = params.format;
        processing.splitByNewline = params.splitByNewline;

        if (params.context != null) {
            result.translation = ModernMT.translation.get(params.user, params.direction, processing, params.query, params.context, params.nbest, params.priority, params.timeout
            	,params.s,params.t,params.w);
        } else if (params.contextString != null) {
            result.context = ModernMT.translation.getContextVector(params.user, params.direction, params.contextString, params.contextLimit);
            result.translation = ModernMT.translation.get(params.user, params.direction, processing, params.query, result.context, params.nbest, params.priority, params.timeout
            	,params.s,params.t,params.w);
        } else {
            result.translation = ModernMT.translation.get(params.user, params.direction, processing, params.query, params.nbest, params.priority, params.timeout
            	,params.s,params.t,params.w);
        }

        return result;
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final InputFormat.Type format;
        public final boolean splitByNewline;
        public final UUID user;
        public final LanguageDirection direction;
        public final String query;
        public final ContextVector context;
        public final String contextString;
        public final int contextLimit;
        public final int nbest;
        public final Priority priority;
        public final boolean verbose;
        public final long timeout;
        public String[] s = null;
        public String[] t = null;
        public String[] w = null;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            format = getEnum("if", InputFormat.Type.class, null);
            splitByNewline = getBoolean("split_nl", false);

            user = getUUID("user", null);

            query = getString("q", true);
            if (query.length() > MAX_QUERY_LENGTH)
                throw new ParameterParsingException("q", query.substring(0, 10) + "...",
                        "max query length of " + MAX_QUERY_LENGTH + " exceeded");

            LanguageDirection engineDirection = ModernMT.getNode().getEngine().getLanguageIndex().asSingleLanguagePair();
            direction = engineDirection != null ?
                    getLanguagePair("source", "target", engineDirection) :
                    getLanguagePair("source", "target");

            contextLimit = getInt("context_limit", 10);
            nbest = getInt("nbest", 0);

            priority = getEnum("priority", Priority.class, Priority.NORMAL);
            verbose = getBoolean("verbose", false);
            timeout = getLong("timeout", 0L);

            String weights = getString("context_vector", false, null);

            int countST = 1;
            Vector<String> sST = new Vector<String>();
            Vector<String> tST = new Vector<String>();
            Vector<String> wST = new Vector<String>();
            while(true) {
            	String sN = getString("s"+countST, false, null);
            	if(sN == null) {
            		break;
            	}
            	String tN = getString("t"+countST, false, null);
            	String wN = getString("w"+countST, false, null);
            	sST.add(sN);
            	tST.add(tN);
            	wST.add(wN);
            	countST++;
            }
            if(sST.size() > 0) {
            	s = sST.toArray(new String[] {});
            	t = tST.toArray(new String[] {});
            	w = wST.toArray(new String[] {});
            }

            if (weights != null) {
                context = ContextUtils.parseParameter("context_vector", weights);
                contextString = null;
            } else {
                context = null;
                contextString = getString("context", false, null);
            }
        }
    }
}