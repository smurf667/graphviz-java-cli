package de.engehausen.graphviz;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

public class GraphVizCLITest {

	@Test
	public void verifyVersion() throws IOException {
		final GraphVizCLI cli = new GraphVizCLI(GraphVizCLI.OPTION_VERSION);
		Assertions.assertTrue(captureOutput(cli).startsWith("dot - graphviz-java-cli"));
	}

	@Test
	public void writeFile() throws IOException, URISyntaxException {
		final Path path = simpleDot();
		final Path target = path.getParent().resolve("out.svg");
		new GraphVizCLI(
			GraphVizCLI.OPTION_OUT + target.toString(),
			GraphVizCLI.OPTION_TYPE + "svg",
			path.toString()
		).run();
		final File out = target.toFile();
		Assertions.assertTrue(out.exists());
		Assertions.assertTrue(out.length() > 0);
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
		Assertions.assertTrue(result.contains("<svg"));
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
		Assertions.assertTrue(result.contains("<svg"));
		Assertions.assertTrue(result.contains("pt\" height=\""));
	}

	@Test
	public void streamSvekSVG() throws IOException, URISyntaxException {
		final Path path = svekDot();
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
		Assertions.assertTrue(result.contains("<svg"));
		Assertions.assertTrue(result.contains("pt\" height=\""));
	}

	@Test
	public void tooManyInputFiles() throws IOException {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> new GraphVizCLI("a", "b"));
	}

	@Test
	public void handleError() throws IOException, URISyntaxException {
		final Path path = toPath("/syntax-error.dot");
		final String message = Assertions.assertThrows(IllegalStateException.class, () -> {
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
		}).getMessage();
		Assertions.assertTrue(message.contains("syntax error"));
	}

	@Test
	@EnabledIfSystemProperty(named = "largeDotTest", matches = "true")
	public void processLargeDot() throws IOException, URISyntaxException {
		final Path path = toPath("/large.dot");
		try {
			System.setProperty("TOTAL_MEMORY", Long.valueOf(3 * (2 << 24) / 2).toString());
			System.setProperty(GraphVizCLI.SYSPROP_TOTAL_MEMORY, Long.valueOf(2 << 24).toString());
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
			Assertions.assertTrue(result.contains("<svg"));
		} finally {
			System.clearProperty("TOTAL_MEMORY");
		}
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
		return toPath("/simple.dot");
	}

	protected Path svekDot() throws URISyntaxException {
		return toPath("/svek.dot");
	}

	protected Path toPath(final String resource) throws URISyntaxException {
		final URL url = GraphVizCLITest.class.getResource(resource);
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
