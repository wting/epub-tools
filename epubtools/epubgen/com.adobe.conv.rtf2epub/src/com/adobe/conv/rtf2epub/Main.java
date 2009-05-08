/*******************************************************************************
* Copyright (c) 2009, Adobe Systems Incorporated
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are met:
*
* ·        Redistributions of source code must retain the above copyright 
*          notice, this list of conditions and the following disclaimer. 
*
* ·        Redistributions in binary form must reproduce the above copyright 
*		   notice, this list of conditions and the following disclaimer in the
*		   documentation and/or other materials provided with the distribution. 
*
* ·        Neither the name of Adobe Systems Incorporated nor the names of its 
*		   contributors may be used to endorse or promote products derived from
*		   this software without specific prior written permission. 
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************/

package com.adobe.conv.rtf2epub;

import java.io.File;
import java.io.FileOutputStream;

import com.adobe.epub.io.ContainerWriter;
import com.adobe.epub.io.FolderContainerWriter;
import com.adobe.epub.io.OCFContainerWriter;
import com.adobe.epub.io.ZipContainerSource;
import com.adobe.epub.opf.Publication;
import com.adobe.office.rtf.RTFDocument;
import com.adobe.office.word.WordDocument;

public class Main {
	public static final String VERSION = "0.2.0";
	public static boolean deprecatedFontMangling = false;
	public static String source = null;
	public static String dest = null;

	public static void main(String[] args) {		
		try {
			Main.processArguments(args);
			if((Main.source == null) || (Main.dest == null)) {
				Main.displayHelp();
			}
			File wordFile = new File(Main.source);
			File epubFileOrFolder = new File(Main.dest);
			RTFDocument doc = new RTFDocument(wordFile);
			ContainerWriter container;
			if( epubFileOrFolder.isDirectory() )
				container = new FolderContainerWriter(epubFileOrFolder);
			else
				container = new OCFContainerWriter(new FileOutputStream(epubFileOrFolder));
			Publication epub = new Publication();
			if (Main.deprecatedFontMangling == true)
				epub.useAdobeFontMangling();
			else
				epub.useIDPFFontMangling();
			Converter conv = new Converter(doc, epub);
			conv.convert();
			epub.serialize(container);
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method iterates through all of the arguments passed to
	 * main to find accepted flags and the name of the file to check.
	 * Here are the currently accepted flags:
	 * <br>
	 * <br>-h or --help = display usage instructions, and exits.
	 * <br>-v or --version = display tool version number
	 * <br>-a or --adobeFontMangling - use the deprecated font mangling
	 * algorithm (default is to use IDPF recommended algorithm.)
	 * 
	 * @param args String[] containing arguments passed to main
	 * @return the name of the file to check
	 */
	public static void processArguments(String[] args) {
		// Exit if there are too few arguments passed to main
		if(args.length < 1) {
			Main.displayHelp();
		}
		
		
		// For each element of args[], check to see if it is an accepted flag
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--version") || args[i].equals("-v")){
				displayVersion();
			}
			else if(args[i].equals("--help") || args[i].equals("-h")){
				displayHelp(); // display help message
			}
			else if(args[i].equals("--adobeFontVersion") || args[i].equals("-a")) {
				Main.deprecatedFontMangling = true;
			}
			else if (Main.source==null) {
				Main.source = args[i];
			}
			else if (Main.dest==null){
				Main.dest = args[i];
			}
			else {
				System.err.println("Unknown argument: " + args[i]);
				System.exit(1);
			}
		}
	}
	
	/**
	 * This method displays a short help message that describes the
	 * command-line usage of this tool
	 */
	public static void displayHelp() {
		Main.displayVersion();
		System.out.println("Usage: \n" + "java -jar rtf2epub-"+Main.VERSION+".jar [options] source_file target_file");
		System.out.println("java -jar rtf2epub"+Main.VERSION+".jar [options] source_file target_folder");
		System.out.println();
		System.out.println("This tool accepts the following options:");
		System.out.println("-h or --help 			= Displays this help message, and exits");
		System.out.println("-v or --version 		= Displays the tool's version number");
		System.out.println("-a or --adobeFontMangling 	= (Not recommmended.) Uses deprecated font mangling algorithm. \n\t\t\t\tWithout this flag the IDPF algorithm is used.\n");
		System.exit(0);
	}
	
	public static void displayVersion() {
		System.out.println("rtf2epub Version " + Main.VERSION+"\n");
	}


}
