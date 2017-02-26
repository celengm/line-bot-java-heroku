package ahuglajbclajep.linebot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.URLCodec;
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

/*
 * lineサーバーからのPOSTリクエストにPOSTリクエストを返すクラス
 * heroku上のTomcat8系サーバーでの実行を想定
 */
@WebServlet("/callback")
public class CallBack extends HttpServlet {

	// フィールド変数 //
	private static final String SECRET_KEY = "Channel Secret";
	private static final String TOKEN = "Channel Access Token";
	private static final String APP_NAME = "Webhook URL";
	private boolean inst = true; // インスタンス直後を示すフラグ

	// メソッド //
	/*
	 * LINEサーバーにPOSTリクエストを送り返す
	 *
	 * サーバーにPOSTリクエストがあったとき呼ばれる
	 * jsonを処理するためjacksonライブラリを使用
	 * @param request リクエストの内容
	 * @param response レスポンスを送るためのオブジェクト
	 * @throws IOException 内部でcatchしているので実際にはthrowsしない
	 * @since Java1.8
	 * @see https://devdocs.line.me/ja/?java#webhook
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		/*
		 * Line API Reference (以下リファレンス)より request は
		 * Request Headers と Request Body を持つ
		 * Request Headers には署名検証に使用するX-Line-Signatureヘッダーが含まれる
		 */

		/* 署名検証 //
		 * リファレンスには"リクエストごとに必ず検証してください"とある
		 * X-Line-Signature の値と request 全体から生成されるハッシュを比べ送信元がLINEサーバーか確認
		 * 不正なリクエストならHTTPステータスコード200を response に送る
		 */
		String reqAll = ""; // 例外処理の外でも使うのでここでインスタンス
		try (Stream<String> stream = request.getReader().lines()) {
			/*
			 * getReader() は HttpServletRequest の親 ServletRequest のメソッド @return java.io.BufferedReader
			 * Stream<t> は Stream API , java.io とは無関係 @since Java1.8
			 * nullチェックはtry-with-resources文より不要 @since Java1.7
			 * 結局,1行ずつ BufferedReader から読み込まれたデータのアドレスを格納した stream という list を得ている
			 * つまり リクエスト,ヘッダー(複数行),空白の行,ボディ のリストが得られる
			 */
			String signature = request.getHeader("X-Line-Signature"); // X-Line-Signature ヘッダーの値を取得, 該当するヘッダーがないならnull
			reqAll = stream.reduce((hoge, fuga) -> hoge + "\n" + fuga).orElse("");
			/*
			 * (s1, s2) -> ... はラムダ式 @since Java1.8
			 * hoge, fuga で先頭から順に2行を取り出し, json の改行文字である \n で結合
			 * 改行で切る前の request の内容をリストからString型で再生成している
			 * reduce の戻り値は Optional<T> @since Java 1.8
			 * @see https://docs.oracle.com/javase/jp/8/docs/api/java/util/Optional.html
			 * これは戻り値が null "かもしれない"ことを示すクラス
			 * orElse() には Optional<String> を String に変換する意味もある, 今回はこれ
			 */

			// 以下返信の生成までリファレンスの "署名検証の例" を参考
			SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"); // 秘密鍵を生成
			Mac mac = Mac.getInstance("HmacSHA256"); // HmacSHA256 アルゴリズムを利用するため Mac クラスをインスタンス
			mac.init(key); // 生成した秘密鍵を HmacSHA256 アルゴリズムにわたす
			byte[] source = reqAll.getBytes(StandardCharsets.UTF_8); // reqAll をbyte配列に変換
			String createdSignature = Base64.encodeBase64String(mac.doFinal(source)); // doFinal() を実行することで初期化を行い, source からハッシュ値を生成, エンコード
			// Request Headerに付与されたSignatureと一致するか検証
			if (!createdSignature.equals(signature)) { // createdSignature はここでは非null値, equals() は signature が null でも false を返す, 不正なリクエストなら
				response.setStatus(HttpServletResponse.SC_OK); // HTTPステータスコード200を返す
				/*
				 * ステータスコードが200である理由はリファレンスに
				 * "WebhookのHTTPS POSTリクエストに対しては、常にStatus Code 200を返してください"とあるため
				 * ステータスコード200は処理の成功を意味する
				 */
				return; // doPostメソッドを抜け次のリクエストに備える
			}
		} catch (IOException | NullPointerException | NoSuchAlgorithmException | InvalidKeyException e) { // 堅牢性を重視, マルチキャッチ @since Java1.7
			/*
			 * | は or の意味, 左から
			 * doPost()をオーバーライドする必要があるため throw と書いているがここで catch することで throw しなくなるので安全, UnsupportedEncodingException も含む
			 * reduce() の例外, ヘッダー以下が空だったとき, 最後の equals でも(ほぼ起こり得ないが) createdSignature が null だったら踏む
			 * Mac.getInstance() の例外, 設計上起こりえないがコンパイルするために catch する
			 * Mac.init の例外, 設計上起こりえないがコンパイルするために catch する
			 *
			 * IllegalArgumentException(SecretKeySpecのコンストラクタの例外) は設計上起こりえないので catch しない
			 * IllegalStateException(Mac.doFinalの例外) は設計上起こりえないので catch しない
			 */
			response.setStatus(HttpServletResponse.SC_OK); // 以下は不正なリクエストの時と同様に処理
			return;
		}

		// 署名検証により,以下の処理はリクエストの正規性を前提とする

		/* 内容の解析 //
		 * 送られてきた json を jackson ライブラリでjavaオブジェクトに変換, 解析を行う
		 *
			{
				"replyToken": "nHuyWiB7yP5Zw52FIkcQobQuGDXCTA",
				"type": "message",
				"timestamp": 1462629479859,
				"source": {
					"type": "user",
					"userId": "U206d25c2ea6bd87c17655609a1c37cb8"
				},
				"message": {
					"id": "325708",
					"type": "text",
					"text": "Hello, world"
				}
			}
		 *
		 * 上が下でいう events
		 */
		ObjectMapper mapper = new ObjectMapper(); // json と javaオブジェクトの変換を行うための関数郡を提供するクラス
		// @see https://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/ObjectMapper.html
		JsonNode events;
		try {
			events = mapper.readTree(reqAll).path("events");
			/*
			 * readTree() で json を木構造としたときの根となるノードを JsonNode型(@return)で定義, @throws IOException
			 * path() は get() と同様に木構造上の指定したキーに対応する値(木構造で考えるとノード)を取得するが
			 * get() と違いnullを返 さない("missing node"を返す, これはisMissingNode()でも判別可)
			 * 結局,リファレンスでいう Webhook Event Object を events に格納している
			 */
		} catch (IOException e) { // JsonParseException も含まれる
			response.setStatus(HttpServletResponse.SC_OK); // doPostメソッドを抜ける
			return;
		}

		String replyMess; // if文の外で使うためここで宣言, インスタンスはしない
		if ("message".equals(events.path(0).path("type").asText())) { // なんらかのメッセージが送られてきたとき
			/*
			 * path(0)で events の0番目の配列の要素を見つけ値を取得
			 * path("type") で最初の値の中の type キーを見つけ値を取得
			 * asText(String) は asText() に類似のメソッドで値をString型に変換するほか
			 * 変換元が null だったら引数を返す(asText() は空文字""を返す)
			 */
			replyMess = createReply(events.path(0).path("message")); // createReply関数で内容に合わせた返信を作る
		} else if ("join".equals(events.path(0).path("type").asText())){ // トークに参加したとき
			replyMess = "\"messages\":[{\"type\":\"text\", \"text\":\"睦月、砲雷撃戦始めるよ♪\"}]"; // 定型文なのでcreateReply関数は使わない
		} else {
			response.setStatus(HttpServletResponse.SC_OK); // それ以外のイベントならdoPostメソッドを抜ける
			return;
		}

		/* 返信を作る //
		 * 返信内容はリファレンスよりボディに replyToken, messages を付ければよい
		 * replaytokenは Webhook で受信した replyToken, messages は Send Message Object の配列(最大要素数5)
		 *
		 * HttpPost でクエストを投げたい URL を持った HttpPost オブジェクトを作り
		 * 必要なヘッダーとその値を指定, 返信用のボディを作る
		 */
		HttpPost httpPost = new HttpPost("https://api.line.me/v2/bot/message/reply"); // サーバーに対する応答を生成するためのクラス, URL はリファレンスより
		// @see http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/methods/HttpPost.html

		httpPost.setHeader("Content-Type", "application/json"); // Contents-Type はデータが json であることを示す
		httpPost.setHeader("Authorization", "Bearer " + TOKEN); // Authorization は認証情報を示す, Authorization: Bearer TOKEN とするのが一般的

		StringBuffer strBuf = new StringBuffer("{\"replyToken\":\""); // (1), 高速化のため StringBuffer で文字連結
		strBuf.append(events.path(0).path("replyToken").asText()); // (2), replyTokenを取得, 返信時の宛先になる
		strBuf.append("\","); // (3)
		strBuf.append(replyMess); // (4)
		strBuf.append("}"); // (5)
		/*
			{
				"replyToken":"0123456789...", // (1), (2), (3)
				"messages":[ // (4)
					// ここの内容は replyMess による
				]
			} // (5)
		 *
		 */

		// 返信を送る //
		StringEntity params = new StringEntity(strBuf.toString(), StandardCharsets.UTF_8); // Entity とは実体を持ったデータのまとまりのこと
		httpPost.setEntity(params); // StringEntity は HttpEntity を実装してる

		try (CloseableHttpResponse resp = HttpClients.createDefault().execute(httpPost);) {
			/*
			 * HttpClients.createDefault() は CloseableHttpClient を返す
			 * CloseableHttpClient とは閉じることのできる(閉じなければいけない)Http通信を行えるクラス
			 * 今回はtry-with-resources文で閉じている
			 * httpPost は HttpUriRequest を実装, リクエストの反応を CloseableHttpResponse に格納する
			 *
			 * 成功時はStatus Code 200と共に空のJSONオブジェクト {} が返る
			 *
			 * ステータスコードの取得は
			 * int status = resp.getStatusLine().getStatusCode();
			 * getStatusLine() の戻り値は StatusLine (もしくはnull)
			 * StatusLineの中身は HTTP-Version SP Status-Code SP Reason-Phrase CRLF
			 * @see https://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/StatusLine.html
			 *
			 * オブジェクトの取得は
			 * resp.getEntity().getContent();
			 * そのまま read(); したり InputStreamReader → BufferedReader.readLine(); などよしなに
			 */
		} catch (IOException e) {
			// なにもしない
		}
		response.setStatus(HttpServletResponse.SC_OK); // 200を返し,次のリクエストに備える
	}

	/*
	 * 返信を生成する
	 *
	 * URLの書式に変換する部分はapacheのライブラリを使用
	 * @param "message": {"id": "325708","type": "text","text": "Hello, world"} など
	 * @param リクエストのURL, QRコードの生成に使用
	 * @return "messages":[{"type":"text","text":"にゃしぃ"}] など, 最大で5つのオブジェクトが送れる
	 * @see https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/net/URLCodec.html
	 */
	private String createReply(JsonNode message){

		StringBuffer replyMess = new StringBuffer("\"messages\":[");

		if (inst) { // インスタンス直後は追加でメッセージを返す
			replyMess.append("{\"type\":\"text\",\"text\":\"");
			replyMess.append("睦月、呼んだ？");
			replyMess.append("\"},");
			inst = false;
		}

		String type = message.path("type").asText(); // type の値を取得
		if ("text".equals(type)) { // 処理の重いもの, もっとも呼ばれやすいものから順に
			String[] spl;
			spl = message.path("text").asText().split(" ", 2);
			/*
			 * '" "' で区切り,戻り値(区切りの文字は含まれない)を要素数2の配列に指定
			 * "@wol hoge fuga" のとき
			 * spl[0] = "@wol"; spl[1] = "hoge fuga";
			 * "@wol" のとき
			 * spl[0] = "@wol"; spl[1] は存在しない
			 */

			if ("@qr".equals(spl[0])) {
				replyMess.append("{\"type\":\"text\",\"text\":\"");
				replyMess.append("えへへ、どうぞです♪"); // まずテキストを返す
				replyMess.append("\"},");
				try { // QRコード(JPEG画像)を返す
					String url = createQR(spl[1], message.path("id").asText()); // QRコードを生成
					replyMess.append("{\"type\":\"image\",\"originalContentUrl\":\""); // 画像を返す
					replyMess.append(APP_NAME); // ドメイン
					replyMess.append(url); // /tmp/hoge.jpg など
					replyMess.append("\",\"previewImageUrl\":\"");  // プレビュー画像
					replyMess.append(APP_NAME);
					replyMess.append(url);
				} catch (ArrayIndexOutOfBoundsException | IOException | WriterException e) {
					/*
					 * spl[1] が存在しないか,QRコードが生成できなかったとき
					 * IOException は UnsupportedEncodingException を含む
					 */
					replyMess.append("{\"type\":\"text\",\"text\":\"");
					replyMess.append("およ？およよ？"); // テキストを返す
				}

			} else if ("@wol".equals(spl[0])) {
				replyMess.append("{\"type\":\"text\",\"text\":\"");
				replyMess.append("はい、睦月が用意するね！"); // まずテキストを返す
				replyMess.append("\"},");

				replyMess.append("{\"type\":\"text\",\"text\":\""); // URL を返す
				replyMess.append("http://www.wolframalpha.com");
				try {
					String url = new URLCodec().encode(spl[1], "UTF-8");
					/*
					 * URLの書式に変換するには, 上のように URLCodec
					 * @see https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/net/URLCodec.html
					 * や, 標準ライブラリに含まれる java.net.URLEncoder を利用する
					 * ただし URLEncoder には Shift_JIS の文字列が上手く処理できない問題がある
					 */
					replyMess.append("/input/?i="); // GETリクエストを作る
					replyMess.append(url);
				} catch (ArrayIndexOutOfBoundsException | UnsupportedEncodingException e) {
					// なにもしない
				}

			} else if("@twt".equals(spl[0])) {
				replyMess.append("{\"type\":\"text\",\"text\":\"");
				replyMess.append("はい、睦月が用意するね！"); // まずテキストを返す
				replyMess.append("\"},");

				replyMess.append("{\"type\":\"text\",\"text\":\""); // URL を返す
				replyMess.append("https://twitter.com/search"); // ここまでは共通
				try {
					String url = new URLCodec().encode(spl[1], "UTF-8");
					replyMess.append("?q="); // GETリクエストを作る
					replyMess.append(url);
				} catch (ArrayIndexOutOfBoundsException | UnsupportedEncodingException e) {
					replyMess.append("-advanced"); // 別の URL を返す
				}

			} else {
				replyMess.append("{\"type\":\"text\",\"text\":\"");
				replyMess.append("にゃしぃ"); // テキストを返す
			}

		} else if ("sticker".equals(type)) { // スタンプが送られてきたとき
			replyMess.append("{\"type\":\"text\",\"text\":\"");
			replyMess.append("なんですかなんですかぁー？"); // テキストを返す

		} else if ("image".equals(type)) { // 画像が送られてきたとき
			replyMess.append("{\"type\":\"text\",\"text\":\"");
			replyMess.append("睦月、負ける気がしないのね！"); // テキストを返す
		}
		replyMess.append("\"}]");
		return replyMess.toString();
	}

	/*
	 * QRコードを生成する
	 *
	 * 渡された文字列をJPEG画像としてQRコードを生成しURLの続きを返す
	 * QRコードの生成にはZXingを使用
	 * @param QRコードにする文字列
	 * @param ファイル名
	 * @return /hoge.jpg など
	 * @throws WriterException QRコードが生成できなかった場合
	 * @throws IOException QRコードが一時ファイルに書き出せなかった場合
	 * @see https://github.com/zxing/zxing
	 */
	private String createQR(String src, String name) throws WriterException, IOException {
		/* QRコードの生成 //
		 * heroku では自由にファイルを保存することができない
		 * データベースも用意されているが,これもファイルの実体は保存できない
		 * そのためファイルの保存には s3 などのサービスを利用するのが一般的
		 * ただし heroku でもカレントディレクトリの /tmp フォルダ以下には一時ファイルを保存しておくことができる
		 * 一時ファイルはdynosの休眠と共に消滅する
		 */
		EnumMap<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
		/*
		 * EnumMap は列挙型のキーを扱うための Map
		 * Map でQRコード生成時のオプションを指示する
		 * @see https://zxing.github.io/zxing/apidocs/com/google/zxing/EncodeHintType.html
		 */
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // UTF-8 でエンコード
		BitMatrix bitMatrix = new QRCodeWriter().encode(src, BarcodeFormat.QR_CODE, 185, 185, hints);
		/*
		 * サイズはバージョン毎に4セル刻みで大きくなり,バージョン40で171x171, デフォルトのMARGIN(余白)は上下左右4セル
		 * @see https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/qrcode/QRCodeWriter.java
		 * encode() の WriteException は createReply の中で catch したいので throws
		 */

		// 一時ファイルに出力 //
		BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
		StringBuffer fileName = new StringBuffer("/tmp/"); // tmpフォルダ以下に出力
		fileName.append(name);
		fileName.append(".jpg");
		ImageIO.write(image, "JPEG", new File(fileName.toString()));
		/*
		 * write() の IOException は createReply の中で catch したいので throws
		 * ファイル名は .jpg だが JPEG として作るのはリファレンスでそうなっていたため
		 */

		return fileName.toString();
	}
}