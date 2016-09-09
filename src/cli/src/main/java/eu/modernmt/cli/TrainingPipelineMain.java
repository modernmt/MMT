package eu.modernmt.cli;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.TrainingFacade;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TrainingPipelineMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option vocabularyPath = Option.builder("v").hasArgs().required(false).build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
            Option devPath = Option.builder().longOpt("dev").hasArg().required(false).build();
            Option testPath = Option.builder().longOpt("test").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(vocabularyPath);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
            cliOptions.addOption(devPath);
            cliOptions.addOption(testPath);
        }

        public final Locale sourceLanguage;
        public final Locale targetLanguage;
        public final File vocabulary;
        public final File[] inputRoots;
        public final File outputRoot;
        public final File devRoot;
        public final File testRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Locale.forLanguageTag(cli.getOptionValue('s'));
            targetLanguage = Locale.forLanguageTag(cli.getOptionValue('t'));
            vocabulary = cli.hasOption('v') ? new File(cli.getOptionValue('v')) : null;

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));

            devRoot = cli.hasOption("dev") ? new File(cli.getOptionValue("dev")) : null;
            testRoot = cli.hasOption("test") ? new File(cli.getOptionValue("test")) : null;
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        ArrayList<Corpus> monolingualCorpora = new ArrayList<>();
        ArrayList<BilingualCorpus> bilingualCorpora = new ArrayList<>();

        Corpora.list(monolingualCorpora, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoots);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        TrainingFacade.TrainingOptions options = new TrainingFacade.TrainingOptions();

        if (args.devRoot != null)
            options.developmentPartition = args.devRoot;

        if (args.testRoot != null)
            options.testPartition = args.testRoot;

        if (args.vocabulary != null)
            options.vocabulary = args.vocabulary;

        ModernMT.training.preprocess(bilingualCorpora, monolingualCorpora, args.sourceLanguage, args.targetLanguage, args.outputRoot, options);
    }

}
