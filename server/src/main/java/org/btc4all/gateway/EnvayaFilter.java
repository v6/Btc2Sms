package org.btc4all.gateway;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

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
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.btc4all.gateway.pojo.EnvayaRequest;
import org.restnucleus.WrappedRequest;

@Singleton
public class EnvayaFilter implements Filter {
    public final static String AUTH_HEADER = "X-Request-Signature";
    
    private String secret;
    private String basePath;
    
    @Inject
    public EnvayaFilter(String secret, String basePath) {
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
        
        EnvayaRequest envayaRequest = null;
        if (httpReq.getMethod().equalsIgnoreCase("POST") || httpReq.getMethod().equalsIgnoreCase("PUT")){
            envayaRequest = EnvayaRequest.fromBody(wrappedRequest.getInputStream());
        }
        httpReq.setAttribute("er", envayaRequest);
        try {
            calcSig = calculateSignature(url, (null!=envayaRequest)?envayaRequest.toMap():new ArrayList<NameValuePair>(), secret);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (calcSig!=null && calcSig.equals(sig)){
            chain.doFilter(wrappedRequest, response);
        } else{
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(401);
        }
    }
    
    public static String calculateSignature(String uri, List<NameValuePair> nvps, String pw) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MultivaluedMap<String,String> paramMap = new MultivaluedHashMap<>(nvps.size());
        for (NameValuePair nvp: nvps){
            paramMap.put(nvp.getName(), Arrays.asList(nvp.getValue()));
        }
        return calculateSignature(uri, paramMap, pw);
    }
    
    
    public static String calculateSignature(String url, MultivaluedMap<String,String> paramMap, String pw) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        if (null==url||null==paramMap||null==pw){
            return null;
        }
        List<String> params = new ArrayList<>();
        for (Entry<String,List<String>> m :paramMap.entrySet()){
            if (m.getValue().size()>0){
                params.add(m.getKey()+"="+m.getValue().get(0));
            }
        }
        Collections.sort(params);
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        for (String s : params){
            sb.append(",");
            sb.append(s);
        }
        sb.append(",");
        sb.append(pw);
        String value = sb.toString();
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(value.getBytes("utf-8"));

        return new String(Base64.encodeBase64(md.digest()));     
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }

}
