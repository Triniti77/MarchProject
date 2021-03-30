package aws;

import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract public class AWSSession {
    protected static final OkHttpClient client = new OkHttpClient();
    protected Map<String, String> params;

    abstract Builder builder();
    abstract public SessionConnector getConnector();

    abstract public class Builder {
        protected Builder() {
            params = new HashMap<>();
        }

        public Builder withParams(Map<String, String> inputParams) {
            params.putAll(inputParams);
            return this;
        }

        abstract public SessionConnector build();
    }

    abstract public class SessionConnector {
        protected SessionConnector() {}
//
//        protected SessionConnector(Map<String, String> params_) {
//            params.putAll(params_);
//        }

        abstract public SessionConnector authorize() throws IOException;
        abstract public SessionConnector refresh() throws IOException;
        abstract public AuthTokens getTokens();
    }

    public interface AuthTokens {
        String getMethod();
        String getAccessToken();
        String getRefreshToken();
        String getIdToken();
    }
}
