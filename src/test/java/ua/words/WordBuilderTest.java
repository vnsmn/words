package ua.words;

import org.junit.Test;

public class WordBuilderTest {
	@Test
	public void testW() throws Exception {
        String s = Thread.currentThread().getContextClassLoader().getResource("words.txt").getFile();
		WordBuilder.main(new String[]{"--size=250", "--file.src=" + s});
	}
	@Test
	public void testT() throws Exception {
        String s = Thread.currentThread().getContextClassLoader().getResource("words.txt").getFile();
		WordBuilder.main(new String[]{"--size=250",
                "--file.src=" + s,
                "--mode.text=true",
                "--prefix=$cycle,0x002E,0x0020",
                "--suffix=0x002E,0x000A",
                "--filter=[,]"
        });
	}
}
