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
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
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
                .addOption(OptionAdapter.LINE_NUMBERING.createOption())
                .addOption(OptionAdapter.TEXT.createOption())
                .addOption(OptionAdapter.SIZE.createOption()), args);
        String cfile = OptionAdapter.CONFIG_FILE.getOptionValue(line);
        String sfile = OptionAdapter.SOURCE_FILE.getOptionValue(line);
        String tfile = OptionAdapter.TARGET_FILE.getOptionValue(line);
        String preffix = OptionAdapter.PREFIX.getOptionValue(line);
        String suffix = OptionAdapter.SUFFIX.getOptionValue(line);
        String filter = OptionAdapter.FILTER.getOptionValue(line);
        String flnumb = OptionAdapter.LINE_NUMBERING.getOptionValue(line);
        String ftext = OptionAdapter.TEXT.getOptionValue(line);
        String size = OptionAdapter.SIZE.getOptionValue(line);
        if (StringUtils.isBlank(cfile)) {
            throw new WordExeption("The cfile is empty.");
        }
        Properties properties = new Properties();
        if (new File(cfile).exists()) {
            try (FileInputStream fis = new FileInputStream(cfile)) {
                properties.load(fis);
            }
        } else {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cfile);
            properties.load(is);
        }
        if (StringUtils.isBlank(sfile)) {
            sfile = properties.getProperty(OptionAdapter.SOURCE_FILE.getOptionName());
        }
        if (StringUtils.isBlank(tfile)) {
            tfile = properties.getProperty(OptionAdapter.TARGET_FILE.getOptionName());
        }
        if (StringUtils.isBlank(preffix)) {
            preffix = properties.getProperty(OptionAdapter.PREFIX.getOptionName());
        }
        if (StringUtils.isBlank(suffix)) {
            suffix = properties.getProperty(OptionAdapter.SUFFIX.getOptionName());
        }
        if (StringUtils.isBlank(filter)) {
            filter = properties.getProperty(OptionAdapter.FILTER.name);
        }
        if (StringUtils.isBlank(flnumb)) {
            flnumb = properties.getProperty(OptionAdapter.LINE_NUMBERING.name);
        }
        if (StringUtils.isBlank(ftext)) {
            ftext = properties.getProperty(OptionAdapter.TEXT.name);
        }
        if (StringUtils.isBlank(size)) {
            size = properties.getProperty(OptionAdapter.SIZE.name);
        }
        log.info("***** CONFIG *****");
        log.info("sfile=" + sfile);
        log.info("tfile=" + tfile);
        log.info("prefix=" + preffix);
        log.info("suffix=" + suffix);
        log.info("filter=" + filter);
        log.info("flaglnumb=" + flnumb);
        log.info("flagtext=" + ftext);
        log.info("size=" + size);
        //log.info("character=" + Integer.toHexString(Character.codePointAt("\n", 0)));
        new WordBuilder().execute(sfile, tfile, preffix, suffix, filter,
                Boolean.valueOf(flnumb), Boolean.valueOf(ftext), size);

        log.info("***** finish executing the main method *****");
    }

    private void execute(String sfile, String tfile, String prefix, String suffix, String filter,
                         boolean isLineNumber, boolean isText, String size) throws IOException {
        try (FileInputStream fis = new FileInputStream(sfile)) {
            List<String> sortWords;
            if (!isText) {
                Collection<String> ws = WordParser.parseWords(fis, filter);
                log.info("all words=" + ws.size());
                TreeSet<String> sortUniqWords = new TreeSet<>(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareTo(o2);
                    }
                });
                sortUniqWords.addAll(ws);
                sortWords = new ArrayList<>(sortUniqWords);
            } else {
                List<String> ws = WordParser.parseText(fis, filter);
                sortWords = new ArrayList<>(ws);
            }
            String ret = "";
            String groupFileMarker = dateFormat.format(new Date());
            String resFile = (StringUtils.isBlank(tfile) ? sfile : tfile) +
                "." + groupFileMarker;
            long maxCharset = StringUtils.isEmpty(size) ? Long.MAX_VALUE : Long.parseLong(size);
            int pos = 0;
            String suffix2 = convertHexToString(suffix);
            String prefix2 = convertHexToString(prefix);
            int num = 1;
            for (String wd : sortWords) {
                String newRow = ret + (isLineNumber ? num : "") + prefix2 + wd + suffix2;
                if (newRow.length() > maxCharset) {
                    flush(ret, resFile + "." + numberFormat.format(pos) + ".txt");
                    pos++;
                    ret = (isLineNumber ? num : "") + prefix2 + wd + suffix2;
                } else {
                    ret = newRow;
                }
                num++;
            }
            flush(ret, resFile + "." + numberFormat.format(pos) + ".txt");
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
     * Convert hex array to string. splitter is ','
     * @param s is array of number of hex radix
     * @return
     */
    private String convertHexToString(String s) {
        String ret = "";
        if (!StringUtils.isBlank(s)) {
            for (String nb : s.split(",")) {
                ret += String.valueOf(Character.toChars(Integer.valueOf(nb.trim(), 16)));
            }
        }
        return ret;
    }

    private enum OptionAdapter {
        CONFIG_FILE("cfile", "config.properties"),
        SOURCE_FILE("sfile"),
        TARGET_FILE("tfile"),
        PREFIX("prefix"),
        SUFFIX("suffix"),
        FILTER("filter"),
        LINE_NUMBERING("flaglnumb"),
        TEXT("flagtext"),
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
