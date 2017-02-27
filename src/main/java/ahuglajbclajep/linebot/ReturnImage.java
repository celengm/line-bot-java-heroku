package ahuglajbclajep.linebot;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/tmp/*")
public class ReturnImage extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		byte[] image;
		try {
			image = Files.readAllBytes(Paths.get(req.getRequestURI()));
		}catch (InvalidPathException | IOException e) {
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		res.setContentType("image/jpeg");
		try (ServletOutputStream stream = res.getOutputStream()) {
			stream.write(image);
		} catch (IOException e) {
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
