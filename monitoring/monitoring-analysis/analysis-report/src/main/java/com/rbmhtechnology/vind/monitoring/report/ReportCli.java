package com.rbmhtechnology.vind.monitoring.report;

import com.rbmhtechnology.vind.monitoring.report.writer.HtmlReportWriter;
import com.rbmhtechnology.vind.monitoring.report.writer.ReportWriter;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ReportCli {

    private static final Logger logger = LoggerFactory.getLogger(ReportCli.class);
    private static HelpFormatter formatter = new HelpFormatter();

    public static void main( String[] args ) {

        // parse the command line arguments
        final Options options = configCliOptions();
        CommandLine line = null;
        try {
            line = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            logger.error("CLI parsing error: {}", e.getMessage());
            formatter.printHelp("Analysis Report Writer", options);
            System.exit(-1);
        }
        try {
            exec(options, line);
        } catch ( Exception e) {
            logger.error("CLI parsing error: {}", e.getMessage());
            formatter.printHelp("Analysis Report Writer", options);
            System.exit(-1);
        }
    }

    public static void exec(final Options options, final CommandLine line) throws Exception {
        //TODO some checks
        final String esHost = line.getOptionValue("eh");
        final String esPort = line.getOptionValue("ep");
        final String esIndex = line.getOptionValue("ei");

        final String messageWrapper = line.getOptionValue("mw");
        final String resultFormat = line.getOptionValue("f");

        final String applicationId = line.getOptionValue("aid");
        final ZonedDateTime from = ZonedDateTime.parse(line.getOptionValue("from"));
        final ZonedDateTime to = ZonedDateTime.parse(line.getOptionValue("to"));

        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, from, to, applicationId);
        esRepsortService.setMessageWrapper(messageWrapper);

        final LinkedHashMap<String, Long> topFaceFields = esRepsortService.getTopFaceFields();
        final ArrayList<String> facetFields = new ArrayList<>(topFaceFields.keySet());

        final LinkedHashMap<String, Long> topSuggestionFields = esRepsortService.getTopSuggestionFields();
        final ArrayList<String> suggestFields = new ArrayList<>(topSuggestionFields.keySet());

        final ReportWriter reportWriter = new HtmlReportWriter();//TODO make format aware

        final Report report = new Report()
                .setApplicationName(applicationId)
                .setFrom(from)
                .setTo(to)
                .setTopDays(esRepsortService.getTopDays())
                .setRequests(esRepsortService.getTotalRequests())
                .setTopSuggestionFields(topSuggestionFields)
                .setSuggestionFieldsValues(esRepsortService.getSuggestionFieldsValues(suggestFields))
                .setFacetFieldsValues(esRepsortService.getFacetFieldsValues(facetFields))
                .setTopFacetFields(topFaceFields)
                .setTopQueries(esRepsortService.getTopQueries())
                .setTopUsers(esRepsortService.getTopUsers());


        Path filePath = Files.createFile(Paths.get("./testout/" +  getCleanFilename(applicationId, resultFormat)));
        reportWriter.write(report, filePath.toAbsolutePath().toString());

        esRepsortService.close();
    }

    private static String getCleanFilename(String applicationId, String resultFormat) {
        return applicationId.replaceAll(" ", "_").replaceAll("/", "_") + "-" + System.currentTimeMillis() + "." + resultFormat;
    }

    public static Options configCliOptions() {

        final Options options = new Options();

        /**
         *
         private String esHost = "172.20.30.95";
         private String esPort = "19200";
         private String esIndex = "logstash-searchanalysis-*";
         private String applicationName = "mediamanager-Media Manager / Editing - Internal / RBMH-Assets";
         private final String messageWrapper = "message_json";
         */
        options.addOption(Option.builder("eh")
                .numberOfArgs(1)
                .argName("ELASTIC HOST")
                .longOpt("host")
                .desc("eleasticsearch host.")
                .required(true)
                .build());
        options.addOption(Option.builder("ep")
                .numberOfArgs(1)
                .argName("ELASTIC PORT")
                .longOpt("port")
                .desc("eleasticsearch port.")
                .required(true)
                .build());
        options.addOption(Option.builder("ei")
                .numberOfArgs(1)
                .argName("ELASTIC INDEX")
                .longOpt("index")
                .desc("eleasticsearch index.")
                .required(true)
                .build());
        options.addOption(Option.builder("aid")
                .numberOfArgs(1)
                .argName("APPLICATION ID")
                .longOpt("appid")
                .desc("application id.")
                .required(true)
                .build());

        options.addOption(Option.builder("mw")
                .numberOfArgs(1)
                .argName("MESSAGE WRAPPER")
                .longOpt("message_wrapper")
                .desc("message wrapper.")
                .required(true)
                .build());

        options.addOption(Option.builder("f")
                .numberOfArgs(1)
                .argName("RESULT FORMAT")
                .longOpt("result_format")
                .desc("result format.")
                .required(true)
                .build());

        options.addOption(Option.builder("from")
                .numberOfArgs(1)
                .argName("FROM")
                .longOpt("from")
                .desc("from")
                .required(true)
                .build());
        options.addOption(Option.builder("to")
                .numberOfArgs(1)
                .argName("TO")
                .longOpt("to")
                .desc("to")
                .required(true)
                .build());

        return options;
    }
}
