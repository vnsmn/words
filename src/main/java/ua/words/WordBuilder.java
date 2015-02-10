package ua.words;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class WordBuilder {

    static Logger log = Logger.getLogger(WordBuilder.class);
    static private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmssSSS");
    static private NumberFormat numberFormat = NumberFormat.getIntegerInstance();

    static {
        numberFormat.setMaximumIntegerDigits(5);
        numberFormat.setMinimumIntegerDigits(5);
        numberFormat.setGroupingUsed(false);
    }

    @SuppressWarnings("static-access")
    static public void main(String... args) throws IOException, ParseException {
        log.info("");
        log.info("***** start executing the main method *****");

        CommandLine line = new PosixParser().parse(new Options()
                .addOption(OptionAdapter.CONFIG_FILE.createOption())
                .addOption(OptionAdapter.SOURCE_FILE.createOption())
                .addOption(OptionAdapter.TARGET_FILE.createOption())
                .addOption(OptionAdapter.PREFIX.createOption())
                .addOption(OptionAdapter.SUFFIX.createOption())
                .addOption(OptionAdapter.FILTER.createOption())
                .addOption(OptionAdapter.TEXT.createOption())
                .addOption(OptionAdapter.SORT.createOption())
                .addOption(OptionAdapter.UNIQUE.createOption())
                .addOption(OptionAdapter.SIZE.createOption()), args);
        DataConfig dataConfig = new DataConfig();
        dataConfig.cnfFile = OptionAdapter.CONFIG_FILE.getOptionValue(line);
        dataConfig.srcFile = OptionAdapter.SOURCE_FILE.getOptionValue(line);
        dataConfig.trgFile = OptionAdapter.TARGET_FILE.getOptionValue(line);
        dataConfig.prefix = OptionAdapter.PREFIX.getOptionValue(line);
        dataConfig.suffix = OptionAdapter.SUFFIX.getOptionValue(line);
        dataConfig.filter = OptionAdapter.FILTER.getOptionValue(line);
        dataConfig.isText = OptionAdapter.TEXT.getBooleanOptionValue(line);
        dataConfig.isSort = OptionAdapter.SORT.getBooleanOptionValue(line);
        dataConfig.isUnique = OptionAdapter.UNIQUE.getBooleanOptionValue(line);
        dataConfig.size = OptionAdapter.SIZE.getLongOptionValue(line);
        if (StringUtils.isBlank(dataConfig.cnfFile)) {
            throw new WordExeption("The cfile is empty.");
        }
        Properties properties = new Properties();
        if (new File(dataConfig.cnfFile).exists()) {
            try (FileInputStream fis = new FileInputStream(dataConfig.cnfFile)) {
                properties.load(fis);
            }
        } else {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(dataConfig.cnfFile);
            properties.load(is);
        }
        if (StringUtils.isBlank(dataConfig.srcFile)) {
            dataConfig.srcFile = properties.getProperty(OptionAdapter.SOURCE_FILE.getOptionName());
        }
        if (StringUtils.isBlank(dataConfig.trgFile)) {
            dataConfig.trgFile = properties.getProperty(OptionAdapter.TARGET_FILE.getOptionName());
        }
        if (StringUtils.isBlank(dataConfig.prefix)) {
            dataConfig.prefix = properties.getProperty(OptionAdapter.PREFIX.getOptionName());
        }
        if (StringUtils.isBlank(dataConfig.suffix)) {
            dataConfig.suffix = properties.getProperty(OptionAdapter.SUFFIX.getOptionName());
        }
        if (StringUtils.isBlank(dataConfig.filter)) {
            dataConfig.filter = properties.getProperty(OptionAdapter.FILTER.name);
        }
        if (dataConfig.isText == null) {
            dataConfig.isText = Boolean.valueOf(properties.getProperty(OptionAdapter.TEXT.name, "false"));
        }
        if (dataConfig.isSort == null) {
            dataConfig.isSort = Boolean.valueOf(properties.getProperty(OptionAdapter.SORT.name, "false"));
        }
        if (dataConfig.isUnique == null) {
            dataConfig.isUnique = Boolean.valueOf(properties.getProperty(OptionAdapter.UNIQUE.name, "false"));
        }
        if (dataConfig.size == null) {
            dataConfig.size = Long.valueOf(properties.getProperty(OptionAdapter.SIZE.name, "" + Long.MAX_VALUE));
        }
        log.info("***** CONFIG *****");
        log.info("sfile=" + dataConfig.srcFile);
        log.info("tfile=" + dataConfig.trgFile);
        log.info("prefix=" + dataConfig.prefix);
        log.info("suffix=" + dataConfig.suffix);
        log.info("filter=" + dataConfig.filter);
        log.info("mode.text=" + dataConfig.isText);
        log.info("mode.isSort=" + dataConfig.isSort);
        log.info("mode.isUnique=" + dataConfig.isUnique);
        log.info("size=" + dataConfig.size);
        //log.info("character=" + Integer.toHexString(Character.codePointAt("\n", 0)));
        new WordBuilder().execute(dataConfig);

        log.info("***** finish executing the main method *****");
    }

    private void execute(DataConfig dataConfig) throws IOException {
        try (FileInputStream fis = new FileInputStream(dataConfig.srcFile)) {
            List<String> sortWords;
            if (!dataConfig.isText) {
                Collection<String> ws = WordParser.parseWords(fis, dataConfig.filter);
                log.info("all words=" + ws.size());
                if (dataConfig.isUnique) {
                    LinkedHashSet<String> uniqWords = new LinkedHashSet<>();
                    uniqWords.addAll(ws);
                    sortWords = new ArrayList<>(uniqWords);
                } else {
                    sortWords = new ArrayList<>(ws);
                }
                if (dataConfig.isSort) {
                    sortWords.sort(new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            return o1.compareTo(o2);
                        }
                    });
                }
            } else {
                List<String> ws = WordParser.parseText(fis, dataConfig.filter);
                sortWords = new ArrayList<>(ws);
            }
            String ret = "";
            String groupFileMarker = dateFormat.format(new Date());
            String resFile = (StringUtils.isBlank(dataConfig.trgFile) ? dataConfig.srcFile : dataConfig.trgFile) +
                "." + groupFileMarker;
            long maxCharset = dataConfig.size;
            CycleState cycleState = new CycleState();
            for (String wd : sortWords) {
                String suffix2 = toViewString(dataConfig.suffix, cycleState);
                String prefix2 = toViewString(dataConfig.prefix, cycleState);
                String newRow = ret + prefix2 + wd + suffix2;
                if (newRow.length() > maxCharset) {
                    flush(ret, resFile + "." + numberFormat.format(cycleState.flushNumber) + ".txt");
                    cycleState.flushNumber++;
                    ret = prefix2 + wd + suffix2;
                } else {
                    ret = newRow;
                }
                cycleState.cycle++;
            }
            flush(ret, resFile + "." + numberFormat.format(cycleState.flushNumber) + ".txt");
        }
    }

    private void flush(String ws, String file) throws IOException {
        if (StringUtils.isBlank(ws)) {
            return;
        }
        try (FileOutputStream fs = new FileOutputStream(file)) {
            fs.write(ws.getBytes());
        }
    }

    /**
     * Convert special symbols to string. splitter is ','
     * @param s is array of number of hex radix
     * @return view String
     */
    private String toViewString(String s, CycleState state) {
        String ret = "";
        if (!StringUtils.isBlank(s)) {
            for (String nb : s.split(",")) {
                nb = nb.trim().toLowerCase();
                if (nb.startsWith("$cycle")) {
                    ret += state.cycle;
                } else if (nb.startsWith("0x")) {
                    ret += String.valueOf(Character.toChars(Integer.valueOf(nb.substring(2), 16)));
                } else {
                    ret += Integer.valueOf(nb.trim());
                }
            }
        }
        return ret;
    }

    private class CycleState {
        int cycle = 1;
        int flushNumber;
    }

    static private class DataConfig {
        String cnfFile;
        String srcFile;
        String trgFile;
        String prefix;
        String suffix;
        String filter;
        Boolean isText;
        Boolean isSort;
        Boolean isUnique;
        Long size;
    }

    private enum OptionAdapter {
        CONFIG_FILE("file.cnf", "config.properties"),
        SOURCE_FILE("file.src"),
        TARGET_FILE("file.target"),
        PREFIX("prefix"),
        SUFFIX("suffix"),
        FILTER("filter"),
        TEXT("mode.text"),
        SORT("mode.sort"),
        UNIQUE("mode.unique"),
        SIZE("size");

        OptionAdapter(String name) {
            this.name = name;
        }

        OptionAdapter(String name, String defValue) {
            this(name);
            this.defValue = defValue;
        }

        public String getOptionValue(CommandLine commandLine) {
            return commandLine.getOptionValue(name, defValue);
        }

        public Boolean getBooleanOptionValue(CommandLine commandLine) {
            String s = getOptionValue(commandLine);
            return StringUtils.isBlank(s) ? null : Boolean.valueOf(s.trim());
        }

        public Long getLongOptionValue(CommandLine commandLine) {
            String s = getOptionValue(commandLine);
            return StringUtils.isBlank(s) ? null : Long.valueOf(s.trim());
        }

        public String getOptionName() {
            return name;
        }

        @SuppressWarnings("static-access")
        public Option createOption() {
            return OptionBuilder.withLongOpt(name)
                    .hasArg()
                    .withValueSeparator('=')
                    .create();
        }

        private String name;
        private String defValue;
    }
}
