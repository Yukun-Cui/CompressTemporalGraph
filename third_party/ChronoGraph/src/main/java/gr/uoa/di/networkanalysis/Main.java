package gr.uoa.di.networkanalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Main {

	public static void main(String[] args) throws Exception {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("mytestfile.txt"));
		writer.append("This will be written! Whatever happens, happens!");
		writer.close();
		
		System.out.println(new File("yahoo.graph").length());
	}
}
