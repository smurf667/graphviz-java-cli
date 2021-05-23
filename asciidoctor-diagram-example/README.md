# Asciidoctor Maven Plugin: Asciidoctor Diagram Example

An example project that demonstrates how to integrate Asciidoctor Diagram with the Asciidoctor Maven plugin.
The purpose of the project is to show how to use the Graphviz "emulator" which runs with Java+JavaScript only, see [below](#graphviz-java-cli).

## Usage

Convert the AsciiDoc to HTML5 by invoking the `process-resources` goal (configured as the default goal):

    mvn

Open the file `target/generated-docs/example-manual.html` in a browser to see the generated HTML file containing the generated diagram images.

## Graphviz configuration

Asciidoctor Diagram bundles both the `ditaa` and PlantUML libraries and will use them to generate diagrams.
In order to generate diagrams using Graphviz, it must be installed separately
*unless* using Windows, in which case please read [Windows: Starting from 1.2020.21](https://plantuml.com/graphviz-dot). This allows to generate
some diagrams, but to create Graphviz diagrams the `dot` tool is still required.

There are two options to reference the installed Graphviz `dot` tool in order to generate diagrams: The system's `PATH` or plug-in attributes configuration.

### Configuration via system's PATH
Visit [Graphviz](https://www.graphviz.org/) for details on how to install the `dot` tool, and to make the `dot` command available on the system's PATH.

### Configuration via plug-in attributes
Once Graphviz binaries from the [Graphviz](https://www.graphviz.org/) are available on the system, the plug-in attributes in the `pom.xml` can be used to reference to the `dot` tool directly.
This type of configuration may be especially useful when working in a CI environment.
Example:

```xml
<plugin>
    <groupId>org.asciidoctor</groupId>
    <artifactId>asciidoctor-maven-plugin</artifactId>
    ...
    <configuration>
        <attributes>
            <graphvizdot>/PATH/TO/Graphviz/bin/dot</graphvizdot>
        </attributes>
        ...
```

### Alternatives

#### graphviz-java-cli

The project of the [parent folder](../README.md) produces a `.zip` with libraries and launcher scripts which can be called from the command line with a *subset* of the actual `dot` commands.
This is sufficient for some primitive tests. Consider this a proof-of-concept only. As shown here it is madness: Java (through Maven) calling Ruby calling Java calling JavaScript (at least). This is very heavy weight, but seems to work at least for the given diagrams.

In the example project here, the `graphvizdot` attribute is set to an _invalid value on Windows_ (causing fallbacks), and for Unix (Linux) a shell script running `graphviz-java-cli` 
is pointed to.

Running on Windows with just `mvn` will generate diagrams, but not the last Graphviz one ("Failed to generate image"). To run with `graphviz-java-cli` on Windows explicitly, run

    mvn -Pwindows

Afterwards, in the generated `target/generated-docs/example-manual.html` all diagrams will show.

#### smetana

Not tested, but around: https://plantuml.com/smetana02

#### VizJs

Not tested, but around: https://plantuml.com/vizjs
