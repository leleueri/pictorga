package org.github.leleueri.services;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.MultiEmitterProcessor;
import org.apache.thrift.Option;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.github.leleueri.PictorgaConfiguration;
import org.github.leleueri.utils.NoopContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Singleton
public class OrganizerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizerService.class);

    @Inject
    private PictorgaConfiguration config;

    @Inject
    ManagedExecutor exec;
    
    private volatile ProcessState processingState = ProcessState.NONE;

    private static final Pattern PATTERN_1 = Pattern.compile(".*([0-9]{8}[-_][0-9]{6}).*");
    private static final DateTimeFormatter FORMATTER_1 = new DateTimeFormatterBuilder().appendPattern("yyyyMMdd")
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .toFormatter();
    
    private static final Pattern PATTERN_2 = Pattern.compile(".*([0-9]{4}[-_][0-9]{2}[-_][0-9]{2}).*");
    private static final DateTimeFormatter FORMATTER_2 = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();
    
    private static final LocalDateTime EPOCH = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());

    public OrganizerService() throws Exception{
        TikaConfig cfg = new TikaConfig(OrganizerService.class.getClassLoader().getResourceAsStream("tika-config.xml"));
        this.parser = new AutoDetectParser(cfg);
    }

    private final AutoDetectParser parser;
    
    private static final ContentHandler handler = new NoopContentHandler();
    
    public static class ProcessingContext {
        public final Path item;
        public String destSegment;
        public LocalDateTime creationDate;
        
        public ProcessingContext(Path item) {
            this.item = item;
        }
        
    }
    
    private void markAsProcessed() {
        this.processingState = ProcessState.DONE;
    }

    private void markAsProcessingFailed() {
        this.processingState = ProcessState.ERROR;
    }

    private void markAsProcessing() {
        this.processingState = ProcessState.ONGOING;
    }
    
    public ProcessState getProcessingState() {
        return processingState;
    }
    
    public OrganizerState startOrganize() {
        LOGGER.debug("Start organize...");

        OrganizerState state = OrganizerState.EMPTY_DIR;
        Path inputDir = Paths.get(config.getInputDir());
        File entry = inputDir.toFile();

        if (entry.exists() && entry.isDirectory()) {
            if (processingState == ProcessState.ONGOING) {
                LOGGER.debug("Start organize already running...");
                state = OrganizerState.ONGOING;
            } else {
                File[] files = entry.listFiles();
                if (files.length != 0) {
                    LOGGER.debug("Launch organize processing...");
                    state = OrganizerState.STARTED; 
                    
                    markAsProcessing();
                    
                    Multi.createFrom().items(listFiles(inputDir)
                            .filter(file -> !file.toFile().getName().equalsIgnoreCase("Thumbs.db"))
                            .map(item -> {
                                LOGGER.debug("Prepare Context for '{}'", item);
                                return new ProcessingContext(item);
                            }))                            
                            .onItem().produceUni( this::process ).merge(4)
                            .subscribe().with(
                                    (context) -> LOGGER.debug("'{}' Processed !", context.item), 
                                    (e) -> {
                                        LOGGER.warn("Processing error", e);
                                        markAsProcessingFailed();
                                    }, 
                                    () -> {
                                        LOGGER.info("Input Directory '{}' processed", inputDir);
                                        markAsProcessed();
                                    });
                    
                }
            }
        } else {
            LOGGER.debug("'{}' doesn't exist or is not a directory", config.getInputDir());
        }
        return state;
    }

    private static LocalDateTime extractDate(String name) {
        LOGGER.debug("extract date from name '{}'", name);
        try {
            Matcher m1 = PATTERN_1.matcher(name);
            if (m1.matches()) {
                return LocalDateTime.parse(m1.group(1).substring(0, 8), FORMATTER_1);
            }

            Matcher m2 = PATTERN_2.matcher(name);
            if (m2.matches()) {
                return LocalDateTime.parse(m2.group(1).substring(0, 10).replaceAll("_", "-"), FORMATTER_2);
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to extract date from file name '{}'", name, e);

        }
        return EPOCH;
    }

    private Uni<ProcessingContext> process(final ProcessingContext context) {
        return Uni.createFrom().completionStage(exec.supplyAsync(() -> {
            try {
                Metadata metadata = parseFile(context.item);
                Date date = Option.fromNullable(metadata.getDate(TikaCoreProperties.METADATA_DATE)).or(metadata.getDate(TikaCoreProperties.CREATED));
                date = Option.fromNullable(date).or(Date.from(Instant.EPOCH));
                context.creationDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            } catch (Throwable e) {
                LOGGER.error("Unable to process METADATA for the file '{}'", context.item, e);
            }

            if (context.creationDate == null || context.creationDate.getYear() <= 1970 ) {
                LOGGER.debug("Tika parsing failed, try to extract date from filename");
                context.creationDate = extractDate(context.item.toFile().getName());
            }

            try {
                String destSubPath = context.creationDate.getYear() > 1970 ? context.creationDate.format(DateTimeFormatter.ofPattern("yyyy" +  File.separator + "MM")) : Paths.get(config.getInputDir()).relativize(context.item).getName(0).toFile().getName();
                if (destSubPath.equals("")) destSubPath = "unclassified";
                LOGGER.debug("{} CREATED {}", context.item, destSubPath);
                Path destDirectory = Files.createDirectories(Paths.get(config.getRepository(), destSubPath));
                Files.move(context.item, destDirectory.resolve(context.item.toFile().getName()), StandardCopyOption.ATOMIC_MOVE);
            } catch (Throwable e) {
                LOGGER.error("Unable to move the file '{}'", context.item, e);
            }
            
            return context;
        }));
    }
    
    private Stream<Path> listFiles(Path input)  {
        if (input.toFile().isDirectory()) {
            try {
                return Files.list(input).map(f -> f.toAbsolutePath()).flatMap(this::listFiles);
            } catch (IOException e) {
                LOGGER.info("Unable to list content of directory '{}'", input.toString(), e);
                return Stream.empty();
            }
        } else {

            LOGGER.debug("Found File '{}'", input.toString());
            return Stream.of(input);
        }
    }

    private final Metadata parseFile(Path file) throws IOException, SAXException, TikaException {
        Metadata metadata = new Metadata();
        try (InputStream stream = Files.newInputStream(file, StandardOpenOption.READ)) {
            parser.parse(stream, handler, metadata);
            return metadata;
        }
    }
}