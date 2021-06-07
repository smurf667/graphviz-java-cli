# graphviz-java-cli: Graphviz (dot) in Java+JavaScript only

Okay, I know this is just wrong. Please consider it a proof-of-concept.

![That's just wrong](http://www.quickmeme.com/img/d1/d1a0775b7c55b8e5f24195de20181bc7a8602b1577182360177a322866a59382.jpg)

I've started using [Asciidoctor Diagram](https://asciidoctor.org/docs/asciidoctor-diagram/) with the
[Asciidoctor Maven plugin](https://github.com/asciidoctor/asciidoctor-maven-plugin) and was pleasantly surprised that diagrams (except for direct
Graphviz diagrams) worked out of the box. Disappointment settled in when I ran on Linux. It turns out that PlantUML embeds Graphviz
for Windows only (read ["Starting from 1.2020.21"](https://plantuml.com/graphviz-dot)). This leaves other platforms having to install `dot` separately.
At work in a CI scenario this gives me a hard time. I've seen documentation on [smetana](https://plantuml.com/smetana02) and
[VizJs support for PlantUML](https://plantuml.com/vizjs) but had the impression this would be hard in my above Maven setup.

Looking directly at [VizJs](https://github.com/mdaines/viz.js/blob/master/README.md) I see it has no command line interface.
More out of morbid curiosity I tried to give it one and plug it into the Asciidoctor Maven plugin. There are some quirks (for example
only SVG support), but basically it seems to work and may serve as a Linux workaround.

This project can be built and installed, for example with

    mvn install

As a sanity check Graphviz can now be invoked:

```sh
cd target
unzip graphviz-java-cli-*-dist.zip
# show version
dot-lite.sh -V
# read .dot from file and write SVG
dot-lite.sh -odemo.svg ../src/test/resources/simple.dot
# read .dot from file and write SVG to stdout
dot-lite.sh -Tsvg ../src/test/resources/simple.dot
# pipe .dot into stdin and write SVG to file
cat ../src/test/resources/simple.dot | dot-lite.sh -odemo.svg -Tsvg
```

In real life the project building the documentation can depend on `graphviz-java-cli` and unzip the tool which "emulates" Graphviz as shown above:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <version>3.1.2</version>
  <executions>
    <execution>
      <id>unpack</id>
      <phase>initialize</phase>
      <goals>
        <goal>unpack</goal>
      </goals>
      <configuration>
        <artifactItems>
          <artifactItem>
            <groupId>de.engehausen.graphviz</groupId>
            <artifactId>graphviz-java-cli</artifactId>
            <version>${graphviz-java-cli.version}</version>
            <classifier>dist</classifier>
            <type>zip</type>
            <overWrite>false</overWrite>
            <outputDirectory>${project.build.directory}/dot</outputDirectory>
          </artifactItem>
        </artifactItems>
      </configuration>
    </execution>
  </executions>
</plugin>
```

In the configuration of the Asciidoctor Maven plugin the `graphvizdot` attribute can be set to the desired runner script:

```xml
<plugin>
  <groupId>org.asciidoctor</groupId>
  <artifactId>asciidoctor-maven-plugin</artifactId>
  ...
  <configuration>
    <attributes>
      <graphvizdot>${project.build.directory}/dot/dot-lite.sh</graphvizdot>
      ...
    </attributes>
    ...
  </configuration>
</plugin>
```

The approach is demonstrated in the [asciidoctor-diagram-example](asciidoctor-diagram-example/README.md).

## Extended testing

The demonstrated approach seems to only work for simple `.dot` files. I quickly hit a wall with
more complicated ones. Memory and stack sizes can be increased, but runtime seems to grow wildly.
A test for this exists; to run it successfully, do

	export MAVEN_OPTS=-Xss4m -Xms1G -Xmx1G
	mvn test -DlargeDotTest=true
