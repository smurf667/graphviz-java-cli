package de.engehausen.graphviz;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple command line interface for
 * <a href="https://github.com/mdaines/viz.js/blob/master/README.md">VizJs</a>.
 * This only accepts a minimal subset of options of the
 * <a href="https://www.graphviz.org/">Graphviz</a> {@code dot} tool:
 * <ul>
 * <li>{@code -o<out-filename>} output file name</li>
 * <li>{@code -T<type>} output format, only {@code svg} is supported</li>
 * <li>{@code -V} prints version information</li>
 * </ul>
 * Additionally, <em>one single</em> input file name can be specified.
 */
public class GraphVizCLI implements Runnable {

	public static final String OPTION_OUT = "-o";
	public static final String OPTION_TYPE = "-T";
	public static final String OPTION_VERSION = "-V";

	public static final String FORMAT_SVG = "svg";

	public static final String SYSPROP_TOTAL_STACK = "TOTAL_STACK";
	public static final String SYSPROP_TOTAL_MEMORY = "TOTAL_MEMORY";

	private static final String JS_LANG = "js";

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphVizCLI.class);
	private static final String FILE_MARKER = "input-file";
	private static final String VERSION;
	private static final String GRAPHVIZ_VERSION;
	private static final String SWITCH_PREFIX = "-";
	private static final int SWITCH_LENGTH = SWITCH_PREFIX.length() + 1;
	private static final String PROP_VIZ_JS_VERSION = "viz-js";
	private static final String PROP_MY_VERSION = "graphviz-java-cli";

	static {
		String me = "???";
		String graphViz = me;
		try (InputStream stream = GraphVizCLI.class.getResourceAsStream("/versions.properties")) {
			if (stream != null) {
				final Properties props = new Properties();
				props.load(stream);
				me = props.getProperty(PROP_MY_VERSION, me);
				graphViz = props.getProperty(PROP_VIZ_JS_VERSION, graphViz);
			}
		} catch (IOException e) {
			LOGGER.debug("cannot get version info", e);
		}
		VERSION = me;
		GRAPHVIZ_VERSION = graphViz;
	}

	private final InputStream dot;
	private final OutputStream out;
	private final String format;
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
		format = options.getOrDefault(OPTION_TYPE, FORMAT_SVG);
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
		if (!FORMAT_SVG.equals(format)) {
			throw new IllegalArgumentException("only svg type is supported");
		}
		final String full = configure(readToString(GraphVizCLI.class.getResourceAsStream("/META-INF/resources/webjars/viz.js/2.1.2/full.render.js")));
		final String vizjs = readToString(GraphVizCLI.class.getResourceAsStream("/META-INF/resources/webjars/viz.js/2.1.2/viz.js"));
		final Context context = Context.newBuilder(JS_LANG).build();
		context.eval(JS_LANG, vizjs);
		context.eval(JS_LANG, full);
		final Value render = context.eval(JS_LANG, readToString(GraphVizCLI.class.getResourceAsStream("/render.js")));
		if (render.canExecute()) {
			// this is just so weird, I can't get the promise value, already resolved
			final Value promise = render.execute(readToString(dot));
			final String output = context.getBindings(JS_LANG).getMember("svg").asString();
			if (output != null) {
				try {
					out.write(output.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			} else {
				// this seems to be an error; extracting info from the promise is awkward
				final Matcher matcher = Pattern
					.compile("\\[\\[PromiseValue\\]\\]: (.*)", Pattern.DOTALL)
					.matcher(promise.toString());
				throw new IllegalStateException(sanitizeMessage(matcher.find() ? matcher.group(1) : "unknown error"));
			}
		}

	}

	protected InputStream toInStream(final List<String> filenames) throws FileNotFoundException {
		return filenames.isEmpty() ? System.in : new FileInputStream(filenames.get(0));
	}

	protected OutputStream toOutStream(final String filename) throws FileNotFoundException {
		return filename == null ? System.out : new FileOutputStream(filename);
	}

	protected String readToString(final InputStream is) {
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			return reader
				.lines()
				.collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String configure(final String full) {
		return configure(
			configure(full, SYSPROP_TOTAL_MEMORY)
			, SYSPROP_TOTAL_STACK);
	}

	protected String configure(final String full, final String key) {
		final String value = System.getProperty(key);
		if (value == null) {
			return full;
		}
		final Matcher matcher = Pattern
			.compile(String.format(
				"(Module\\[\"%s\"\\]\\|\\|)(\\d+)",
				key)
			).matcher(full);
		if (matcher.find()) {
			return matcher.replaceFirst(matcher.group(1) + value);
		}
		return full;
	}

	protected String sanitizeMessage(final String message) {
		return message.endsWith("}") ? message.substring(0, message.length() - 1) : message;
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