package ua.words;

import org.junit.Test;

public class WordBuilderTest {
	@Test
	public void testW() throws Exception {
        String s = Thread.currentThread().getContextClassLoader().getResource("words.txt").getFile();
		WordBuilder.main(new String[]{"--size=250", "--sfile=" + s});
	}
	@Test
	public void testT() throws Exception {
        String s = Thread.currentThread().getContextClassLoader().getResource("words.txt").getFile();
		WordBuilder.main(new String[]{"--size=250",
                "--sfile=" + s,
                "--flagtext=true",
                "--suffix=002E,000A",
                "--filter=[,]"
        });
	}
}
