package ua.words;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class WordParser {

    static Logger log = Logger.getLogger(WordParser.class);

	/*
	 * This method doesn't close stream of "in" (see argument "in" of
	 * InputStream class)
	 */
	static public Collection<String> parseWords(InputStream in, String filter) throws IOException {
		Reader r = new BufferedReader(new InputStreamReader(in));
		StreamTokenizer st = new StreamTokenizer(r);
        st.wordChars(Character.codePointAt("'", 0), Character.codePointAt("'", 0));
        st.ordinaryChar('.');
        st.ordinaryChar(',');
		boolean eof = false;
		List<String> words = new ArrayList<String>();
		do {
			int token = st.nextToken();
			switch (token) {
			case StreamTokenizer.TT_EOF:
				eof = true;
				break;
			case StreamTokenizer.TT_WORD:
                if (!StringUtils.isBlank(st.sval)) {
                    String s = st.sval.replaceAll("  ", " ");
                    words.add(filter(filter, s).toLowerCase());
                }
				break;
			case StreamTokenizer.TT_NUMBER:
				words.add(Double.toString(st.nval));
				break;
			default:
			}
		} while (!eof);
		return words;
	}

    static public List<String> parseText(InputStream in, String filter) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        for (String s = br.readLine(); s != null; s = br.readLine()) {
            s = s.replaceAll("  ", " ").replaceAll("[.] ", ".").replaceAll(" [.]", ".");
            sb.append(s.trim());
        }
        String[] ret = filter(filter, sb.toString()).split("[.]");
        return Arrays.asList(ret);
    }

    static private String filter(String pattern, String s) {
        return s.replaceAll(pattern, "");
    }
}
