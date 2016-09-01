package org.hps.users.spaul;

import java.io.IOException;

import hep.aida.IHistogram1D;

import org.lcsim.util.aida.AIDA;

public class TestAIDA2ROOT {
	public static void main(String arg[]) throws IOException{
		AIDA aida = AIDA.defaultInstance();
		IHistogram1D h = aida.histogram1D("/folder/title;xaxis;yaxis", 100, 0, 10);
		aida.saveAs("/Users/sebouhpaul/test.root");
	}
}
