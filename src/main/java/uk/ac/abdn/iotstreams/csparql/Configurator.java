package uk.ac.abdn.iotstreams.csparql;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.hp.hpl.jena.rdf.model.Model;

import eu.larkc.csparql.core.engine.CsparqlEngine;
import uk.ac.abdn.iotstreams.util.IotStreamsException;
import uk.ac.abdn.iotstreams.util.Logging;

/**
 * 
 * @author nhc
 *
 * A Configurator loads configuration from a directory
 * and configures a given C-SPARQL Engine accordingly.
 */
final class Configurator {
    /** Charset used for decoding all meat probe files */
    private static final Charset ISO88591 = Charset.forName("ISO-8859-1");

    private static final String QUERY_FILE = "csparql-query.rq";
    private static final String OWL_FILE = "init.ttl";
    
    /** Root of all configuration files */
    private static final Path CONFIG_ROOT = Paths.get("config/iotstreams/");
    
    /** The engine to configure */
    private final CsparqlEngine engine;

    /** Observer for each query */
    private Map<String, IotStreamsFormatter> observers = new HashMap<>();

    /** The persistent model to add inferred provenance to */
    private final Consumer<Model> persistentModel;
    
    /**
     * Registers the engine to configure
     * @param engine The engine to configure
     * @param persistentModel The object to pass inferred provenance to
     */
    public Configurator(final CsparqlEngine engine, final Consumer<Model> persistentModel) {
        this.engine = engine;
        this.persistentModel = persistentModel;
        try {
            Files.walk(CONFIG_ROOT)
                .filter(Files::isRegularFile)
                .forEach(this::addFile);
        } catch (final IOException e) {
            throw IotStreamsException.configurationError(e);
        }
    }
    
    /**
     * Reads file content and interprets the file path in order to add this content
     * to the right place.
     * @param file A file in the configuration dir.
     */
    private void addFile(final Path file) {
        Logging.info(file.toString());
        final String content = read(file);
        final Path rel = CONFIG_ROOT.relativize(file);
        if (rel.getNameCount() == 2 && rel.getFileName().toString().equals(QUERY_FILE)) {
            //<CONFIG_ROOT>/<name>/csparql-query.rq: A C-SPARQL query
            this.registerQuery(rel.getName(0), content);
        } else if (rel.getNameCount() == 2 && rel.getFileName().toString().equals(OWL_FILE)) {
            //<CONFIG_ROOT>/<name>/init.ttl: A TTL ontology for initializing our Jena model
            formatter(rel.getName(0)).setOntology(content);
        } else if (rel.getNameCount() == 3 && rel.getFileName().toString().endsWith(".rq")) {
            //<CONFIG_ROOT>/<name>/[coldstart/warm]/<othername>: A SPARQL update to be executed on the output of a query
            formatter(rel.getName(0)).addSparql(
                    rel.getName(1).toString(), 
                    rel.getName(2).toString(), 
                    content);
        } else {
            throw IotStreamsException.configurationError(String.format("Unexpected file %s at depth %d, filename=%s", rel.toString(), rel.getNameCount(), rel.getFileName().toString()));
        }
    }
    
    /**
     * Registers a new C-SPARQL query.
     * @param name Derived from the file path
     * @param content The text of the C-SPARQL query
     */
    private void registerQuery(final Path name, final String content) {
        try {
            this.engine.registerQuery(
                    content, 
                    false)
                .addObserver(formatter(name));
        } catch (final ParseException e) {
            throw IotStreamsException.configurationError(e);
        }
    }
    
    private IotStreamsFormatter formatter(final Path nameAsPath) {
        final String name = nameAsPath.toString();
        if (!this.observers.containsKey(name)){
            this.observers.put(name, new IotStreamsFormatter(name, this.persistentModel));
        }
        return this.observers.get(name.toString());
    }
    /**
     * Utility for reading a file entirely, as ISO88591
     * @param file Any regular file
     * @return The file's content
     */
    private static String read(final Path file) {
        try {
            return String.join("\n", Files.readAllLines(file, ISO88591));
        } catch (final IOException e) {
            throw IotStreamsException.configurationError(e);
        }
    }
}
