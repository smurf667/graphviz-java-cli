package de.engehausen.graphviz;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.parse.Parser;

/**
 * Simple command line interface for
 * <a href="https://github.com/nidi3/graphviz-java">graphviz-java</a>.
 * This only accepts a minimal subset of options of the
 * <a href="https://www.graphviz.org/">Graphviz</a> {@code dot} tool:
 * <ul>
 * <li>{@code -o<out-filename>} output file name</li>
 * <li>{@code -T<type>} output format, e.g. {@code svg} or {@code png}</li>
 * <li>{@code -V} prints version information</li>
 * </ul>
 * Additionally, <em>one single</em> input file name can be specified.
 */
public class GraphVizCLI implements Runnable {

	public static final String OPTION_OUT = "-o";
	public static final String OPTION_TYPE = "-T";
	public static final String OPTION_VERSION = "-V";

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphVizCLI.class);
	private static final String FILE_MARKER = "input-file";
	private static final String VERSION;
	private static final String GRAPHVIZ_VERSION;
	private static final String SWITCH_PREFIX = "-";
	private static final int SWITCH_LENGTH = SWITCH_PREFIX.length() + 1;
	private static final String PROP_GRAPHVIZ_JAVA_VERSION = "graphviz-java";
	private static final String PROP_MY_VERSION = "graphviz-java-cli";
	private static final Pattern SVG_IN_PX = Pattern.compile("(?m)\\<svg\\s+width=\"(\\d+)px\"\\s+height=\"(\\d+)px\"");

	static {
		String me = "???";
		String graphViz = me;
		try (InputStream stream = GraphVizCLI.class.getResourceAsStream("/versions.properties")) {
			if (stream != null) {
				final Properties props = new Properties();
				props.load(stream);
				me = props.getProperty(PROP_MY_VERSION, me);
				graphViz = props.getProperty(PROP_GRAPHVIZ_JAVA_VERSION, graphViz);
			}
		} catch (IOException e) {
			LOGGER.debug("cannot get version info", e);
		}
		VERSION = me;
		GRAPHVIZ_VERSION = graphViz;
	}

	private final InputStream dot;
	private final OutputStream out;
	private final Format format;
	private final boolean showVersion;

	/**
	 * Processes the command line parameters.
	 * @param args the command line parameters.
	 * @throws IOException in case of error.
	 */
	public GraphVizCLI(final String... args) throws IOException {
		final Map<String, String> options = Stream.of(args)
			.filter(arg -> arg != null)
			.map(arg -> {
				if (arg.startsWith(SWITCH_PREFIX)) {
					return arg.length() > SWITCH_LENGTH ?
						new String[] {
							arg.substring(0, SWITCH_LENGTH),
							arg.substring(SWITCH_LENGTH)
						} :
						new String[] {
						arg,
						arg
					};
				}
				return new String[] { arg, FILE_MARKER };
			})
			.collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
		final List<String> files = options
			.entrySet()
			.stream()
			.filter(entry -> FILE_MARKER.equals(entry.getValue()))
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		if (files.size() > 1) {
			throw new IllegalArgumentException(String.format("only one input file supported, but got %s", files));
		}
		dot = toInStream(files);
		out = toOutStream(options.get(OPTION_OUT));
		format = Format.valueOf(options.getOrDefault(OPTION_TYPE, Format.PNG.name()).toUpperCase(Locale.ENGLISH));
		showVersion = options.containsKey(OPTION_VERSION);
	}

	/**
	 * Runs Graphviz.
	 */
	@Override
	public void run() {
		if (showVersion) {
			System.out.printf("dot - graphviz-java-cli v%s (graphviz-java v%s)%n", VERSION, GRAPHVIZ_VERSION);
			return;
		}
		try {
			Graphviz
				.fromGraph(new Parser().read(dot))
				.postProcessor((result, options, processOptions) -> result
					.mapString(svg -> {
						// quirky PlantUML stdout handling (requires pt instead of px)
						// https://github.com/plantuml/plantuml/blob/f83a207360011e575046becb056f5f84104656b2/src/net/sourceforge/plantuml/svek/DotStringFactory.java#L354
						final Matcher matcher = SVG_IN_PX.matcher(svg);
						if (out == System.out && matcher.find()) {
							return matcher.replaceFirst("<svg width=\"$1pt\" height=\"$2pt\"") + svg.substring(matcher.end());
						}
						return svg;
					}))
				.render(format)
				.toOutputStream(out);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				LOGGER.error("cannot close out stream", e);
			}
		}
	}

	protected InputStream toInStream(final List<String> filenames) throws FileNotFoundException {
		return filenames.isEmpty() ? System.in : new FileInputStream(filenames.get(0));
	}

	protected OutputStream toOutStream(final String filename) throws FileNotFoundException {
		return filename == null ? System.out : new FileOutputStream(filename);
	}

	/**
	 * Runs Graphviz.
	 * @param args the command line parameters.
	 * @throws IOException in case of error
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.printf("usage: dot-lite [-V] [-o<out-filename>] [-T<type>] [<in-filename>]");
			return;
		}
		new GraphVizCLI(args).run();
	}

}