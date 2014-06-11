package org.btc4all.gateway;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.restnucleus.WrappedRequest;

@Singleton
public class PlivoAuthFilter implements Filter {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    public final static String AUTH_HEADER = "x-plivo-signature";
    
    private String secret;
    private String basePath;
    
    @Inject
    public PlivoAuthFilter(String secret, String basePath) {
        this.secret = secret;
        this.basePath = basePath;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        WrappedRequest wrappedRequest = new WrappedRequest(
                (HttpServletRequest) request);
        
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String url = basePath + httpReq.getServletPath() + httpReq.getPathInfo();
        String queryString = httpReq.getQueryString();
        if (queryString != null)  {
            url = url + '?' +queryString;
        }
        String sig = httpReq.getHeader(AUTH_HEADER);
        String calcSig = null;
        
        Map<String, String> map = null;
        if (httpReq.getMethod().equalsIgnoreCase("POST") || httpReq.getMethod().equalsIgnoreCase("PUT")){
            String body = convertStreamToString(wrappedRequest.getInputStream());
            map = new HashMap<>();
            String[] pairs = body.split("\\&");
            for (int i=0; i<pairs.length; i++) {
                String[] fields = pairs[i].split("=");
                String name = URLDecoder.decode(fields[0], "UTF-8");
                String value = (fields.length>1)?URLDecoder.decode(fields[1], "UTF-8"):"";
                map.put(name, value);
            }
        }
        if (map==null){
            map = new HashMap<>();
        }
        List<String> params = new ArrayList<>();
        for (Entry<String,String> e :map.entrySet()){
            params.add(e.getKey()+e.getValue());
        }
        Collections.sort(params);
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        for (String s : params){
            sb.append(s);
        }
        String conc = sb.toString();
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA1_ALGORITHM);
        try{
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(conc.getBytes());
            calcSig = new String(Base64.encodeBase64((rawHmac)));
        }catch(Exception e){
            e.printStackTrace();
        }
        if (calcSig!=null && calcSig.equals(sig)){
            chain.doFilter(wrappedRequest, response);
        } else{
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(401);
        }
    }
    
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

    @Override
    public void destroy() {
    }

}
