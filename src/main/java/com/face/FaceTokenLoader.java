/***************************************************************************
 * Copyright 2014 by HomeDirect - All rights reserved.                *    
 **************************************************************************/
package com.face;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.vietspider.net.cookie.VietSpiderCookieSpec;

import com.chamgroup.system.SystemEnvironment;


/**
 *  Author : Nhu Dinh Thuan
 *          Email:thuan.nhu@homedirect.com.vn
 * May 19, 2015
 */
public class FaceTokenLoader {
	private final static Logger LOGGER = Logger.getLogger(FaceTokenLoader.class);

	private  BasicCookieStore cookieStore;
	private  CloseableHttpClient httpclient;

	public FaceTokenLoader() {
		cookieStore = new BasicCookieStore();
		CookieSpecProvider easySpecProvider = new CookieSpecProvider() {
			@SuppressWarnings("unused")
			public CookieSpec create(HttpContext context) {
				return new VietSpiderCookieSpec();
			}
		};

		Registry<CookieSpecProvider> r = RegistryBuilder.<CookieSpecProvider>create()
				.register(CookieSpecs.STANDARD, new org.vietspider.net.cookie.DefaultCookieSpecProvider())
				.register("easy", easySpecProvider)
				.build();


		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec("easy")
				.setSocketTimeout(30*1000).setConnectTimeout(180*1000)
				.build();


		HttpClientBuilder clientBuilder = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultCookieSpecRegistry(r)
				.setDefaultRequestConfig(globalConfig)
				.setRetryHandler(new DefaultHttpRequestRetryHandler(5, true))
				;
		clientBuilder.setDefaultCookieStore(cookieStore);
		httpclient = clientBuilder.build();
	}

	private final static String GROUP_NUMBER_OF_MEMBER_CODE = "<span id=\"count_text\">";
	private final static String GROUP_NAME_CODE = "<title id=\"pageTitle\">";

	public String[] readGroup(String groupId) throws URISyntaxException {
		String code = getGroups("https://www.facebook.com/groups/" + groupId);
		String [] values = new String[2];
		if(code == null) return values;
		int idx = code.indexOf(GROUP_NUMBER_OF_MEMBER_CODE);
		if(idx > -1) {
			int start = idx + GROUP_NUMBER_OF_MEMBER_CODE.length();
			int end =  code.indexOf(' ', start + 2);
			if(idx > -1) values[1] = code.substring(start, end);
		}

		idx = code.indexOf(GROUP_NAME_CODE);
		if(idx > -1) {
			int start = idx + GROUP_NAME_CODE.length();
			int end =  code.indexOf('<', start + 2);
			if(idx > -1) values[0] = code.substring(start, end);
		}

		File file = SystemEnvironment.getDataDir();// new File(url.toURI());
		if(!file.exists()) file.mkdirs();
		file = new File(file, groupId + ".group.html");
		try {
			RWData.getInstance().save(file, code.getBytes("utf8"));
		} catch (Exception e) {
			LOGGER.error(e, e);
		}
		return values;
	}

	public String getGroups(String url){
		try {
			HttpGet httpget = new HttpGet(url);
			CloseableHttpResponse response = httpclient.execute(httpget);

			String html = readContent(response.getEntity());
			return html;
		} catch (Exception e) {
			LOGGER.info(e.getMessage());
			return null;
		}
	}

	public String getShortLivedAccessToken(String email, String password) {
		try {
			login(email, password);

			HttpGet httpget = new HttpGet("https://developers.facebook.com/tools/accesstoken/");
			CloseableHttpResponse response = httpclient.execute(httpget);
			String html = readContent(response.getEntity());
			int start = html.indexOf("nikbds");
			if(start < 0) start = 0;
			int idx = html.indexOf("User Token", start);
			if(idx < 0) {
			/*	File file = SystemEnvironment.getInstance().getDataFolder();// new File(url.toURI());
				file = new File(file, "track/facebook.login.html");
				if(!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				if(!file.exists()) file.createNewFile();

				RWData.getInstance().save(file, html.getBytes("utf8"));
				LOGGER.error("save facebook page to "+ file.getAbsolutePath());
				       }*/
				LOGGER.error(email + " : "+ password + " - Not found User Token data from server ");
				return null;
			}
			html = html.substring(idx);
			start = html.indexOf("<code>");
			int end = html.indexOf("</code>", start);
			if(start < 0 || end < 0) {
				LOGGER.error(email + " : "+ password + " - Not found code token from server");
				return null;
			}

			String code = html.substring(start + 6, end);
			LOGGER.info(email + " : "+ password + " - Get User Token from server: "+ code);
			return code;

		} catch (Exception exp) {
			LOGGER.error(exp, exp);
		}
		return null;
	}

	private static String readContent(HttpEntity entity) throws UnsupportedOperationException, IOException {
		return readContent(entity.getContent());
	}

	private static String readContent(InputStream stream) throws UnsupportedOperationException, IOException {
		int read = -1;
		byte[] buff = new byte[4*1024];
		StringBuilder builder = new StringBuilder();
		while((read = stream.read(buff)) != -1) {
			builder.append(new String(buff, 0, read));
		}
		return builder.toString();
	}


	private void login(String email, String password) throws IOException, URISyntaxException {
		HttpGet httpget = new HttpGet("https://www.facebook.com/");
		httpget.addHeader(new BasicHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:39.0) Gecko/20100101 Firefox/39.0")); 
		httpget.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
		httpget.addHeader(new BasicHeader("Accept-Language", "en-US,vi-VN;q=0.8,vi;q=0.5,en;q=0.3"));
		httpget.addHeader(new BasicHeader("Cookie", "_js_datr=RJacVdrWks5AzPmBL_jSImxW; _js_reg_fb_ref=https%3A%2F%2Fwww.facebook.com%2F; _js_reg_fb_gate=https%3A%2F%2Fwww.facebook.com%2F; wd=1440x175; dpr=2"));
		httpget.addHeader(new BasicHeader("Accept-Encoding", "gzip, deflate"));
		httpget.addHeader(new BasicHeader("Connection", "keep-alive"));
		httpget.addHeader(new BasicHeader("Cache-Control", "max-age=0"));

		CloseableHttpResponse response1 = httpclient.execute(httpget);
		try {
			HttpEntity entity = response1.getEntity();

			LOGGER.info("Login form get: " + response1.getStatusLine());
			EntityUtils.consume(entity);

			LOGGER.info("Initial set of cookies:");
			List<Cookie> cookies = cookieStore.getCookies();
			if (cookies.isEmpty()) {
				LOGGER.info("None");
			} else {
				for (int i = 0; i < cookies.size(); i++) {
					LOGGER.info("- " + cookies.get(i).toString());
				}
			}
		} finally {
			response1.close();
		}

		HttpUriRequest login = RequestBuilder.post()
				.setUri(new URI("https://www.facebook.com/login"))
				.addParameter("email", email)
				.addParameter("pass", password)
				.build();
		login.addHeader(new BasicHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:39.0) Gecko/20100101 Firefox/39.0")); 
		login.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
		login.addHeader(new BasicHeader("Accept-Language", "en-US,vi-VN;q=0.8,vi;q=0.5,en;q=0.3"));
		login.addHeader(new BasicHeader("Accept-Encoding", "gzip, deflate"));
		login.addHeader(new BasicHeader("Connection", "keep-alive"));
		login.addHeader(new BasicHeader("Cache-Control", "max-age=0"));
		CloseableHttpResponse response2 = httpclient.execute(login);
		try {
			HttpEntity entity = response2.getEntity();

			LOGGER.info("Login form get: " + response2.getStatusLine());

			LOGGER.info("Post logon cookies:");
			List<Cookie> cookies = cookieStore.getCookies();
			if (cookies.isEmpty()) {
				LOGGER.info(email + " : " + password + " - None");
			} else {
				for (int i = 0; i < cookies.size(); i++) {
					LOGGER.info("- " + cookies.get(i).toString());
				}
			}

			EntityUtils.consume(entity);
		} finally {
			response2.close();
		}
	}
/*
	public static void main(String[] args) {
		FaceTokenLoader login = new FaceTokenLoader();
		LOGGER.info(login.getShortLivedAccessToken("", ""));
	}*/
	
}
