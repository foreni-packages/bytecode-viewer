package the.bytecode.club.bytecodeviewer;

import java.io.File;

import me.konloch.kontainer.io.DiskWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import the.bytecode.club.bytecodeviewer.decompilers.Decompiler;

/**
 * 
 * Used to allow BCV to be integrated as CLI instead of GUI.
 * 
 * @author Konloch
 *
 */

public class CommandLineInput {


	private static final Options options = new Options();
	private static final CommandLineParser parser = new DefaultParser();
	
	static {
		options.addOption("help", false, "prints the help menu.");
		options.addOption("list", false, "lists all the available decompilers for BCV " + BytecodeViewer.version+".");
		options.addOption("decompiler", true, "sets the decompiler, procyon by default.");
		options.addOption("i", true, "sets the input.");
		options.addOption("o", true, "sets the output.");
		options.addOption("t", true, "sets the target class to decompile, append all to decomp all as zip.");
		options.addOption("nowait", true, "won't wait the 5 seconds to allow the user to read the CLI.");
	}
	
	public static boolean containsCommand(String[] args) {
		if(args == null || args.length == 0)
			return false;
		
		try {
			CommandLine cmd = parser.parse(options, args);
			if(
				cmd.hasOption("help") ||
				cmd.hasOption("list") ||
				cmd.hasOption("decompiler") ||
				cmd.hasOption("i") ||
				cmd.hasOption("o") ||
				cmd.hasOption("t") ||
				cmd.hasOption("nowait")
			) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static int parseCommandLine(String[] args) {
		if(!containsCommand(args))
			return 1;
		try {
			CommandLine cmd = parser.parse(options, args);
			if(cmd.hasOption("list")) {
			    System.out.println("Procyon");
			    System.out.println("CFR");
			    System.out.println("FernFlower");
			    System.out.println("Krakatau");
			    System.out.println("Krakatau-Bytecode");
			    System.out.println("JD-GUI");
			    System.out.println("Smali");
				return -1;
			} else if(cmd.hasOption("help")) {
				for(String s : new String[] {
						"-help                         Displays the help menu",
						"-list                         Displays the available decompilers",
						"-decompiler <decompiler>      Selects the decompiler, procyon by default",
						"-i <input file>               Selects the input file",
						"-o <output file>              Selects the output file",
						"-nowait                       Doesn't wait for the user to read the CLI messages"
				})
					System.out.println(s);
				return -1;
			} else {
				if(cmd.getOptionValue("i") == null) {
					System.err.println("Set the input with -i");
					return -1;
				} if(cmd.getOptionValue("o") == null) {
					System.err.println("Set the output with -o");
					return -1;
				} if(cmd.getOptionValue("t") == null) {
					System.err.println("Set the target with -t");
					return -1;
				}
				
				File input = new File(cmd.getOptionValue("i"));
				File output = new File(cmd.getOptionValue("o"));
				String decompiler = cmd.getOptionValue("decompiler");

				if(!input.exists()) {
					System.err.println(input.getAbsolutePath() + " does not exist.");
					return -1;
				}
				
				if(output.exists()) {
					System.err.println("WARNING: Deleted old " + output.getAbsolutePath() + ".");
					output.delete();
				}
				
				//check if zip, jar, apk, dex, or class
				//if its zip/jar/apk/dex attempt unzip as whole zip
				//if its just class allow any
				
				if(
					decompiler != null &&
					!decompiler.equalsIgnoreCase("procyon") &&
					!decompiler.equalsIgnoreCase("cfr") &&
					!decompiler.equalsIgnoreCase("fernflower") &&
					!decompiler.equalsIgnoreCase("krakatau") &&
					!decompiler.equalsIgnoreCase("krakatau-bytecode") &&
					!decompiler.equalsIgnoreCase("jd-gui") &&
					!decompiler.equalsIgnoreCase("smali")
				) {
					System.out.println("Error, no decompiler called '" + decompiler + "' found. Type -decompiler-list for the list");
				}
				

				if(!cmd.hasOption("nowait"))
					Thread.sleep(5 * 1000);
					
				return 0;
			}
		} catch(Exception e) {
			new the.bytecode.club.bytecodeviewer.api.ExceptionUI(e);
		}
		
		return 1;
	}
	
	public static void executeCommandLine(String[] args) {
		try {
			CommandLine cmd = parser.parse(options, args);
			String decompiler = cmd.getOptionValue("decompiler");
			File input = new File(cmd.getOptionValue("i"));
			File output = new File(cmd.getOptionValue("o"));
			String target = cmd.getOptionValue("t");
			
			if(cmd.getOptionValue("decompiler") == null) {
				System.out.println("You can define another decompiler by appending -decompiler \"name\", by default procyon has been set.");
				decompiler = "procyon";
			}
			
			//check if zip, jar, apk, dex, or class
			//if its zip/jar/apk/dex attempt unzip as whole zip
			//if its just class allow any
			
			if(decompiler.equalsIgnoreCase("procyon")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with Procyon");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					Decompiler.procyon.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.procyon.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			} else if(decompiler.equalsIgnoreCase("cfr")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with CFR");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					Decompiler.cfr.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.cfr.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			} else if(decompiler.equalsIgnoreCase("fernflower")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with FernFlower");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					Decompiler.fernflower.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.fernflower.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			} else if(decompiler.equalsIgnoreCase("krakatau")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with Krakatau");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					Decompiler.krakatau.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.krakatau.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			} else if(decompiler.equalsIgnoreCase("krakatau-bytecode")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with Krakatau-Bytecode");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					System.out.println("Coming soon.");
					//Decompiler.krakatauDA.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.krakatauDA.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			} else if(decompiler.equalsIgnoreCase("jd-gui")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with JD-GUI");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					Decompiler.jdgui.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.jdgui.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			} else if(decompiler.equalsIgnoreCase("smali")) {
				System.out.println("Decompiling " + input.getAbsolutePath() + " with Smali");
				BytecodeViewer.openFiles(new File[]{input}, false);
				
				Thread.sleep(5 * 1000);
				
				if(target.equalsIgnoreCase("all")) {
					System.out.println("Coming soon.");
					//Decompiler.smali.decompileToZip(output.getAbsolutePath());
				} else {
					try {
						ClassNode cn = BytecodeViewer.getClassNode(target);
						final ClassWriter cw = new ClassWriter(0);
						try {
							cn.accept(cw);
						} catch(Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(200);
								cn.accept(cw);
							} catch (InterruptedException e1) { }
						}
						String contents = Decompiler.smali.decompileClassNode(cn, cw.toByteArray());
						DiskWriter.replaceFile(output.getAbsolutePath(), contents, false);
					} catch (Exception e) {
						new the.bytecode.club.bytecodeviewer.api.ExceptionUI( e);
					}
				}
			}
			
			System.out.println("Finished.");
			System.out.println("Bytecode Viewer CLI v" + BytecodeViewer.version + " by @Konloch - http://bytecodeviewer.com");
			System.exit(0);
		} catch(Exception e) {
			new the.bytecode.club.bytecodeviewer.api.ExceptionUI(e);
		}
	}
	
}