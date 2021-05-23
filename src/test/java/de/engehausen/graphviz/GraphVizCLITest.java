package de.engehausen.graphviz;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GraphVizCLITest {

	@Test
	public void verifyVersion() throws IOException {
		final GraphVizCLI cli = new GraphVizCLI(GraphVizCLI.OPTION_VERSION);
		Assertions.assertTrue(captureOutput(cli).startsWith("dot - graphviz-java-cli"));
	}

	@Test
	public void writePNG() throws IOException, URISyntaxException {
		final Path path = simpleDot();
		final Path png = path.getParent().resolve("out.png");
		new GraphVizCLI(
			GraphVizCLI.OPTION_OUT + png.toString(),
			GraphVizCLI.OPTION_TYPE + "png",
			path.toString()
		).run();
		final BufferedImage result = ImageIO.read(png.toFile());
		Assertions.assertNotNull(result);
	}

	@Test
	public void produceSVG() throws IOException, URISyntaxException {
		final Path path = simpleDot();
		final String result = captureOutput(() -> {
			try {
				new GraphVizCLI(
					GraphVizCLI.OPTION_TYPE + "svg",
					path.toString()
				).run();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});
		Assertions.assertTrue(result.startsWith("<svg"));
	}

	@Test
	public void streamSVG() throws IOException, URISyntaxException {
		final Path path = simpleDot();
		final String result = captureOutput(() -> {
			final InputStream old = System.in;
			try {
				System.setIn(new FileInputStream(path.toFile()));
				new GraphVizCLI(
					GraphVizCLI.OPTION_TYPE + "svg"
				).run();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				System.setIn(old);
			}
		});
		Assertions.assertTrue(result.startsWith("<svg"));
		Assertions.assertTrue(result.contains("pt\" height=\""));
	}

	@Test
	public void tooManyInputFiles() throws IOException {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> new GraphVizCLI("a", "b"));
	}

	@Test
	public void invokeMain() throws IOException {
		final String info = captureOutput(() -> {
			try {
				GraphVizCLI.main(new String[0]);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});
		Assertions.assertNotNull(info);
		Assertions.assertTrue(info.startsWith("usage"));
	}

	protected Path simpleDot() throws URISyntaxException {
		final URL url = GraphVizCLITest.class.getResource("/simple.dot");
		Assertions.assertNotNull(url);
		return Paths.get(url.toURI());
	}

	protected String captureOutput(final Runnable code) throws IOException {
		final PrintStream old = System.out;
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			final PrintStream capture = new PrintStream(baos);
			System.setOut(capture);
			code.run();
			capture.flush();
			return new String(baos.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			System.setOut(old);
		}
	}

}
