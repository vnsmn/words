package ua.words;

import org.junit.Test;

public class WordBuilderTest {
	@Test
	public void test() throws Exception {
        String s = Thread.currentThread().getContextClassLoader().getResource("words.txt").getFile();
		WordBuilder.main(new String[]{"--size=250", "--sfile=" + s});
	}
}
