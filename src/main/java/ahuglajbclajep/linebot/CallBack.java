package ahuglajbclajep.linebot;


import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


@WebServlet("/callback")
public class CallBack extends HttpServlet {
	private static final String APP_NAME = System.getenv("APP_NAME");
	private static final String SECRET_KEY = System.getenv("LINE_BOT_CHANNEL_SECRET");
	private static final String TOKEN = System.getenv("LINE_BOT_CHANNEL_TOKEN");

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) {


		// 署名検証 //
		String sig = req.getHeader("X-Line-Signature");
		byte[] reqAll;
		try (InputStream in = req.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			while (true) {
				int i = in.read();
				if (i == -1) {
					reqAll = out.toByteArray();
					break;
				}
				out.write(i);
			}

			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"));
			String csig = Base64.getEncoder().encodeToString(mac.doFinal(reqAll));

			if (!csig.equals(sig)) throw(new IOException());

		} catch (IOException | NullPointerException | NoSuchAlgorithmException | InvalidKeyException e) {
			res.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		// 内容の解析 //
		ObjectMapper mapper = new ObjectMapper();
		JsonNode events;
		try {
			events = mapper.readTree(reqAll).path("events");
		} catch (IOException e) {
			res.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		String replyMess;
		if ("message".equals(events.path(0).path("type").asText())) {  // メッセージを受けたとき
			replyMess = createReply(events.path(0).path("message"));

		} else if ("join".equals(events.path(0).path("type").asText())){  // トークの参加を受けたとき
			replyMess = "\"messages\":[{\"type\":\"text\", \"text\":\"睦月、砲雷撃戦始めるよ♪\"}]";

		} else {
			res.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		// 返信する //
		HttpPost httpPost = new HttpPost("https://api.line.me/v2/bot/message/reply");
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("Authorization", "Bearer " + TOKEN);

		StringBuffer replyBody = new StringBuffer("{\"replyToken\":\"")
				.append(events.path(0).path("replyToken").asText())
				.append("\",")
				.append(replyMess)
				.append("}");

		httpPost.setEntity(new StringEntity(replyBody.toString(), StandardCharsets.UTF_8));

		try (CloseableHttpResponse resp = HttpClients.createDefault().execute(httpPost)) {
		} catch (IOException e) {}

		res.setStatus(HttpServletResponse.SC_OK);
	}
	
	private String GetData(String sVal){
		String sTmp;
		String sX;
		String sY;
		int iPos;

		//https://pkget.com/?lat=25.0754447989281&lng=121.522592776661&g=2
		iPos=sVal.indexOf("lat=");
		if(iPos>0) {
		    sTmp = sVal.substring(iPos);
		    sTmp=sTmp.replace("lat=","");
		    sTmp=sTmp.replace("&lng=",",");
		    sTmp=sTmp.replace("&g=1","");
		    sTmp=sTmp.replace("&g=2","");

		    iPos=sTmp.indexOf(",");
		    if(iPos>0) {
		        sX = sTmp.substring(0, iPos);
		        sY = sTmp.substring(iPos + 1);
		        if(sX.length()>10) {
	 	    	    sX = sX.substring(0, 10);
		        }
			if(sY.length()>10) {
		            sY = sY.substring(0, 10);
			    }
			    sTmp = sX + ',' + sY;
			}			
		}
		else
		{
			sTmp="magnet:?xt=urn:btih:" + sVal;
		    //sTmp=sVal;
		    //sTmp="!!!f";
		}
		return sTmp;
	}	
	
	private String createReply(JsonNode message){
		StringBuffer replyMessages = new StringBuffer("\"messages\":[");
		String type = message.path("type").asText();
		String sTmp;

		if ("text".equals(type)) {
			String[] args;
			//args = message.path("text").asText().split("#", 2);
			sTmp=message.path("text").asText().replace(" ","");
			args = sTmp.split(" ", 2);
			//args = message.path("text").asText().split(" ", 2);
			

			if ("@qr".equals(args[0])) {
				replyMessages.append("{\"type\":\"text\",\"text\":\"")
						.append("よぉーし、頑張るにゃ！")
						.append("\"},");
				try {
					String url = createQR(args[1], message.path("id").asText());  // /tmp/hoge.jpgなど
					replyMessages.append("{\"type\":\"image\",\"originalContentUrl\":\"")
							.append("https://").append(APP_NAME).append(".herokuapp.com")
							.append(url)
							.append("\",\"previewImageUrl\":\"")
							.append("https://").append(APP_NAME).append(".herokuapp.com")
							.append(url);

				} catch (ArrayIndexOutOfBoundsException | IOException | WriterException e) {
					replyMessages.append("{\"type\":\"text\",\"text\":\"")
							.append("およ？およよ？");
				}

			} else if ("@time".equals(args[0])) {
				replyMessages.append("{\"type\":\"text\",\"text\":\"")
						.append("えへへ、どうぞです♪")
						.append("\"},")
						.append("{\"type\":\"text\",\"text\":\"");
				try {
					ZonedDateTime now = ZonedDateTime.now(ZoneId.of(args[1]));
					replyMessages.append(now.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));

				} catch (ArrayIndexOutOfBoundsException | DateTimeException e) {
					replyMessages.append("利用可能なタイムゾーンの一覧です！")
							.append(System.getProperty("line.separator"))
							.append("https://git.io/vyqDP");
				}

			} else if ("@wol".equals(args[0])) {
				replyMessages.append("{\"type\":\"text\",\"text\":\"")
						.append("はい、睦月が用意するね！")
						.append("\"},").append("{\"type\":\"text\",\"text\":\"")
						.append("http://www.wolframalpha.com");
				try {
					String url = URLEncoder.encode(args[1], "UTF-8");
					replyMessages.append("/input/?i=").append(url);
				} catch (ArrayIndexOutOfBoundsException | UnsupportedEncodingException e) {}

			} else if("@twt".equals(args[0])) {
				replyMessages.append("{\"type\":\"text\",\"text\":\"")
						.append("はい、睦月が用意するね！")
						.append("\"},").append("{\"type\":\"text\",\"text\":\"")
						.append("https://twitter.com/search");

				try {
					String url = URLEncoder.encode(args[1], "UTF-8");
					replyMessages.append("?q=").append(url);

				} catch (ArrayIndexOutOfBoundsException | UnsupportedEncodingException e) {
					replyMessages.append("-advanced");
				}

			} else {
				replyMessages.append("{\"type\":\"text\",\"text\":\"")
						.append(GetData(args[0]));
			}

		} else if ("sticker".equals(type)) {  // スタンプが送られてきたとき
			replyMessages.append("{\"type\":\"text\",\"text\":\"")
					.append("なんですかなんですかぁー？");

		} else if ("image".equals(type)) {  // 画像が送られてきたとき
			replyMessages.append("{\"type\":\"text\",\"text\":\"")
					.append("睦月、負ける気がしないのね！");
		}
		replyMessages.append("\"}]");

		return replyMessages.toString();
	}

	private String createQR(String arg, String id) throws WriterException, IOException {
		EnumMap<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

		BitMatrix bitMatrix = new QRCodeWriter().encode(arg, BarcodeFormat.QR_CODE, 185, 185, hints);
		// サイズはバージョン毎に4セル刻みで大きくなり,バージョン40で171x171, デフォルトのMARGINは上下左右4セル

		BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
		StringBuffer fileName = new StringBuffer("/tmp/").append(id).append(".jpg");
		ImageIO.write(image, "JPEG", new File(fileName.toString()));  // tmpフォルダ以下に出力

		return fileName.toString();
	}
}
