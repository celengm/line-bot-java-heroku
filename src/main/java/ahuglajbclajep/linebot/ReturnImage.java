package ahuglajbclajep.linebot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * GETリクエストに画像を返すクラス
 * heroku上のTomcat8系サーバーでの実行を想定
 */
public class ReturnImage extends HttpServlet {

	// メソッド //
	/*
	 * GETリクエストに合わせてjpg画像を送り返す
	 *
	 * サーバーにGETリクエストがあったとき呼ばれる
	 * @param request リクエストの内容
	 * @param response レスポンスを送るためのオブジェクト
	 * @throws IOException 内部でcatchしているので実際にはthrowsしない
	 * @throws ServletException この例外を含む処理を実装していないためthrowsしない
	 * @since Java1.8
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException{
		/* 指定された画像を byte[] に変換する //
		 * @WebServlet(name="receive", urlPatterns={"*.jpg"})
		 * などとしておくと request.getRequestURI() が
		 * /tmp/hogehoge.jpg となりそのままファイルパスとして使える
		 *
		 * 画像の byte[] 変換は下のnioを使う方法のほか
		 * ByteArrayOutputStream に ImageIO.write で書き込み toByteArray() したり
		 * apache.commons の IOUtils.toByteArray(InputStream input) を使ってもできる
		 */
		byte[] image;
		try {
			image = Files.readAllBytes(Paths.get(request.getRequestURI()));
		}catch (InvalidPathException | IOException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 でリソースが利用できないことを示す
			return;
		}

		/* 画像を送る //
		 * 下の方法以外にも
		 * Sendクラスで行ったように HttpPost と HttpClients を使ってもいいし
		 * se の HttpURLConnection を使ってもいい
		 * ただしこれらの方法は URL を作るか取得する必要がある
		 */
		response.setContentType("image/jpeg"); // MIME に従ってデータタイプを指定, jpg も jpeg と書く
		try (ServletOutputStream stream = response.getOutputStream();) {
			stream.write(image);
		}catch (IOException e){
			response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 を返す
		}
	}
}