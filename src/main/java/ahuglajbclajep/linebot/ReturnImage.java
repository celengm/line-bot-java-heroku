package ahuglajbclajep.linebot;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/tmp/*")
public class ReturnImage extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException{
		byte[] image;
		try {
			image = Files.readAllBytes(Paths.get(request.getRequestURI()));
		}catch (InvalidPathException | IOException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		response.setContentType("image/jpeg");
		try (ServletOutputStream stream = response.getOutputStream()) {
			stream.write(image);
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
