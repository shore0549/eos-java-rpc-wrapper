package client.impl;

import client.constant.EosApiConstants;
import client.exception.EosApiError;
import client.exception.EosApiException;
import client.security.AuthenticationInterceptor;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.Proxy;

public class EosApiServiceGenerator {

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(EosApiConstants.API_BASE_URL)
                    .addConverterFactory(JacksonConverterFactory.create());

    private static Retrofit retrofit = builder.build();

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null, null, null);
    }

    public static <S> S createService(Class<S> serviceClass, String apiKey, String secret, Proxy proxy) {
        if (!StringUtils.isEmpty(apiKey) && !StringUtils.isEmpty(secret)) {
            AuthenticationInterceptor interceptor = new AuthenticationInterceptor(apiKey, secret);
            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.addInterceptor(interceptor);
                httpClient.proxy(proxy);
                builder.client(httpClient.build());
                retrofit = builder.build();
            }
        }
        return retrofit.create(serviceClass);
    }

    /**
     * Execute a REST call and block until the response is received.
     */
    public static <T> T executeSync(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                EosApiError apiError = getEosApiError(response);
                throw new EosApiException(apiError);
            }
        } catch (IOException e) {
            throw new EosApiException(e);
        }
    }

    /**
     * Extracts and converts the response error body into an object.
     */
    public static EosApiError getEosApiError(Response<?> response) throws IOException, EosApiException {
        return (EosApiError)retrofit.responseBodyConverter(EosApiError.class, new Annotation[0])
                .convert(response.errorBody());
    }
}
