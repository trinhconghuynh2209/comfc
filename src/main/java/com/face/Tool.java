/***************************************************************************
 * 							tungtt							               *    
 **************************************************************************/
package com.face;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

/**
 * Author : tungtt Apr 7, 2016
 */
public class Tool {
	private static Logger logger = Logger.getLogger(Tool.class);

	private static String USER_AGENT =  "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0";
	private static File folder = UtilFile.getFolder("content");
	private String APP_ID = "1699912630277195";
	private String APP_SECRET = "24d8d99341591a8bba2d334d5d68d765";
	private static String PATH = folder.getAbsolutePath();

	public String connect(String link, String method) {
		URL url;
		try {
			url = new URL(link);

			/*List<String> list = DataUtil.getInstance().readFile(folder.getAbsolutePath()
				+ "/user-agent.txt");

		Random random = new Random();*/

			HttpURLConnection httpConnection = (HttpURLConnection) url
					.openConnection();
			httpConnection.setRequestMethod(method);
			httpConnection.setRequestProperty("User-Agent", USER_AGENT);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					httpConnection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return response.toString();
		} catch (MalformedURLException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		return null;
	}

	public void like(String token, List<String> postIdList, Tool tool) {
		for(int i = 0; i < postIdList.size(); i++) {
			String url = "https://graph.facebook.com/v2.5/" + postIdList.get(i)
					+ "/likes?access_token=";
			tool.connect(url + token, "POST");
		}
		System.out.println();
	}

	public void postStatus(String token, Tool tool) {
		List<String> messageList = DataUtil.getInstance().readFile(PATH	+ "/post-text.txt");
		List<String> pageIdList = DataUtil.getInstance().readFile(PATH + "/post-id.txt");
		for (int i = 0; i < pageIdList.size(); i++) {
			for (int j = 0; j < messageList.size(); j++) {
				String url = "https://graph.facebook.com/v2.5/"
						+ pageIdList.get(i) + "/feed?message="
						+ messageList.get(i) + "&access_token=";
				String response = tool.connect(url + token, "POST");
				try {
					DataUtil.getInstance().writeText(DataUtil.getInstance().readJsonObject(response, "id"),
							new File(PATH + "/posted-id.txt"), true);
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
	}

	public void comment(String token, List<String> postIdList, Tool tool) {
		String comment = DataUtil.getInstance().readByte(PATH + "/comment.txt");
		for(int i = 0; i < postIdList.size(); i++) {
			String url = "https://graph.facebook.com/v2.5/" + postIdList.get(i)
					+ "/comments?access_token=" + token + "&message=" + comment;
			tool.connect(url, "POST");
		}
	}

	public List<String> getGroupPostId(String token, Tool tool) throws ParseException, IOException, InterruptedException {
		List<String> list = DataUtil.getInstance().readFile(PATH + "/group-id.txt");
		List<String> groupId = new ArrayList<>();
		for(int i = 0; i < list.size(); i++) {
			String url = "https://graph.facebook.com/v2.5/" + urlToId(list.get(i), token, tool) + "/feed?fields=id&limit=100&access_token=" + token;
			String data = tool.connect(url, "GET");
			try {
				groupId.addAll(DataUtil.getInstance().readJsonArray(data, "id"));
			} catch(Exception e) {
				logger.info("khong get duoc post id");
			}
		}
		return groupId;
	}

	public String urlToId(String groupUrl, String token, Tool tool) {
		String url = "https://graph.facebook.com/v2.5/" + groupUrl + "/?access_token=" + token;
		String data = tool.connect(url, "GET");
		
		String id = null;
		try {
			String og_object = DataUtil.getInstance().readJsonObject(data, "og_object");
			id = DataUtil.getInstance().readJsonObject(og_object, "id");
		} catch (ParseException e) {
			logger.error(e);
		}
		
		return id;
	}

	public void share(String token, Tool tool){
		List<String> shareLinkList = DataUtil.getInstance().readFile(PATH + "/share-link.txt");
		List<String> pageIdList = DataUtil.getInstance().readFile(PATH + "/page-id.txt");
		for (int i = 0; i < pageIdList.size(); i++) {
			for (int j = 0; j < shareLinkList.size(); j++) {
				String url = "https://graph.beta.facebook.com/"
						+ pageIdList.get(i) + "/feed?link="
						+ shareLinkList.get(j) + "&access_token=";
				String response = tool.connect(url + token, "POST");
				try {
					DataUtil.getInstance().writeText(DataUtil.getInstance().readJsonObject(response, "id"),new File(folder.getAbsolutePath() + 
							"shared-id.txt"), true);
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
	}

	public String getLongLivedAccessToken(Tool tool, String token) {
		String url = "https://graph.facebook.com/oauth/access_token?grant_type=fb_exchange_token&client_id="
				+ APP_ID
				+ "&client_secret="
				+ APP_SECRET
				+ "&fb_exchange_token="
				+ token
				+ "&redirect_uri=https://www.facebook.com/connect/login_success.html";
		String longLivedToken = tool.connect(url, "GET");
		if(longLivedToken.length() > 0) return longLivedToken;
		return null;
	}

	public void generatedToken(Tool tool) {
		List<String> token = DataUtil.getInstance().readFile(PATH + "/short-lived-token.txt");
		for(int i = 0; i < token.size(); i++) {
			try {
				String longLivedToken = getLongLivedAccessToken(tool, token.get(i));
				if(longLivedToken != null) DataUtil.getInstance().writeText(token.get(i), new File(PATH + "/long-lived-token.txt"), true);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}
	
	public static void launch() throws Exception {
		Tool tool = new Tool();
		FaceTokenLoader tokenLoader = new FaceTokenLoader();
		List<String> accounts = DataUtil.getInstance().readFile(PATH + "/accounts.txt");
//		List<String> tokenList = new ArrayList<>();
		for (int i = 0; i < accounts.size(); i++) {
			int idx = accounts.get(i).indexOf("/");
			String email = accounts.get(i).substring(0, idx);
			String password = accounts.get(i).substring(idx + 1);
			//tokenList.add(tokenLoader.getShortLivedAccessToken(email, password));
			DataUtil.getInstance().writeText(tokenLoader.getShortLivedAccessToken(email, password), new File(PATH + "/short-lived-token.txt"), true);
			logger.info(tokenLoader.getShortLivedAccessToken(email,
					password));
		}
		
		List<String> tokenList = DataUtil.getInstance().readFile(PATH + "/long-lived-token.txt");
		for (int i = 0; i < tokenList.size(); i++) {
			/*List<String> postIdList;
			if (DataUtil.getInstance()
					.readFile(folder.getAbsolutePath() + "/id.txt").size() > 0) {
				postIdList = DataUtil.getInstance().readFile(
						folder.getAbsolutePath() + "/id.txt");
				//postIdList = tool.getGroupPostId(tokenList.get(i), tool);
				for (int j = 0; j < postIdList.size(); j++) {
					tool.like(tokenList.get(i), postIdList, tool);
					DataUtil.getInstance().writeText(postIdList.get(j), new File(folder.getAbsolutePath() + 
							"post_id.txt"), true);
				}
			}*/
			List<String> listId = tool.getGroupPostId(tokenList.get(i), tool);
			System.out.println(listId);
		}

		//		for (int i = 0; i < tokenList.size(); i++) {
		//			// tool.postStatus(tokenList.get(i), tool);
		//			// tool.share(tokenList.get(i), tool);
		//			//tool.comment("abc", tool);
		//			List<String> listId = tool.getGroupPostId(tokenList.get(i), tool);
		//			tool.comment(tokenList.get(i), listId,tool);
		//		}

		logger.info("success");
	}

	public static void main(String[] args) throws Exception {
		launch();
	}
}
