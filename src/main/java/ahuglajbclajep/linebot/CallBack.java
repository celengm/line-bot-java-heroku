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
	
	private String createReply(JsonNode message){
		StringBuffer replyMessages = new StringBuffer("\"messages\":[");
		String type = message.path("type").asText();

		replyMessages.append("！");
		//replyMessages=GetData(message.path("text").asText());				

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
