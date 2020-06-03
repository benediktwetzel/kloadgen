package net.coru.kloadgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.coru.kloadgen.exception.KLoadGenException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

public class KLoadGenStandalone {

  private static final Logger log = Logger.getLogger("KLoadGenStandalone");

  public static void main(String... args) {
    Options options = createCLIOptions();

    CommandLineParser parser = new DefaultParser();
    try {

      CommandLine line = parser.parse(options, args);
      Path jMeterPropsFile = Paths.get(line.getOptionValue("h"));
      if (!Files.exists(jMeterPropsFile) || !Files.isReadable(jMeterPropsFile) || !Files.isDirectory(jMeterPropsFile)) {
        throw new KLoadGenException("JMeter properties File not Valid");
      }
      JMeterUtils.setJMeterHome(jMeterPropsFile.toAbsolutePath().toString());
      JMeterUtils.loadJMeterProperties(jMeterPropsFile.toAbsolutePath().toString() + "/bin/jmeter.properties");

      if (line.hasOption("o")) {
        Path optionalPropsFile = Paths.get(line.getOptionValue("o"));
        if (!Files.exists(optionalPropsFile) || !Files.isReadable(optionalPropsFile) || Files.isDirectory(optionalPropsFile)) {
          throw new KLoadGenException("Optionals properties File not Valid");
        }
        JMeterUtils.loadJMeterProperties(optionalPropsFile.toAbsolutePath().toString());
      }

      Path testPlanFile = Paths.get(line.getOptionValue("t"));
      if (!Files.exists(testPlanFile) || !Files.isReadable(testPlanFile) || Files.isDirectory(testPlanFile)) {
        throw new KLoadGenException("Test plan File not Valid");
      }

      StandardJMeterEngine jmeter = new StandardJMeterEngine();

      JMeterUtils.initLocale();

      HashTree testPlanTree = SaveService.loadTree(testPlanFile.toFile());

      jmeter.configure(testPlanTree);
      jmeter.run();

    } catch (ParseException ex) {
      log.log(Level.SEVERE, "Parsing failed.  Reason: ", ex);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("kloadgen", options);
    } catch (KLoadGenException ex) {
      log.log(Level.SEVERE, "Wrong parameters.  Reason: ", ex);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("kloadgen", options);
    } catch (IOException ex) {
      log.log(Level.SEVERE, "Error accessing files.  Reason: ", ex);
    }

  }

  private static Options createCLIOptions() {
    Options options = new Options();
    options.addOption(Option.builder("h").longOpt("jmeterHome").hasArg().desc("JMeter Properties file").required().build());
    options.addOption(Option.builder("o").longOpt("optionalPros").hasArg().desc("Optional properties file").build());
    options.addOption(Option.builder("t").longOpt("testPlan").hasArg().desc("Test plan file").required().build());
    return options;
  }
}
