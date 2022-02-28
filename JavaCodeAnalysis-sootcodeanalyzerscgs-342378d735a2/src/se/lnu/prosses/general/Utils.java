package se.lnu.prosses.general;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitOption;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.util.Collection;
import java.util.stream.Stream;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.apache.commons.io.FileUtils;

/**
 * @author nakhaa
 *
 */
public class Utils {

	public static final Object XMLField = "field";
	public static final Object XMLParameter = "parameter";
	public static final Object XMlReturnValue = "returnvalue";
	public static final Object XMLReference = "reference";
	public static final Object XMlMethod = "method";

	static public <T> boolean areEqual(List<T> list1, List<T> list2) {
		boolean res = true;
		for (T t : list1) {
			if (!list2.contains(t)) {
				res = false;
				break;
			}
		}
		for (T t : list2) {
			if (!list1.contains(t)) {
				res = false;
				break;
			}
		}
		return res;
	}

	public static void deleteDirectory(File file) throws IOException {
		if (file.isDirectory()) {
			for (File file1 : file.listFiles())
				deleteDirectory(file1);
			file.delete();
		} else if (file.isFile()) {
			file.delete();
		}
	}

	public static ArrayList<File> getFilesOfTypes(String directoryPath, final String[] extensions) {
		FilenameFilter fileFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for(String extension:extensions)
					if (name.toLowerCase().endsWith(extension))
						return true;
				return false;
			}
		};
		File source_codes_path = new File(directoryPath);

		File[] files = source_codes_path.listFiles(fileFilter);
		String[] directories = source_codes_path.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		ArrayList<File> results = new ArrayList<File>();
		if(directories!=null)
			for (String directory : directories)
				results.addAll(getFilesOfTypes(source_codes_path + java.io.File.separator + directory, extensions));

		if(files!=null)
			for (File file :  Arrays.asList(files))
				if (!file.isDirectory())
					results.add(file);
		Collections.sort(results);
		return results;

	}

	public static Collection<String> getDirectories(String directoryPath) {
		if(!new File(directoryPath).exists()) {
			Utils.logErr(Utils.class, "Directory " + directoryPath + " does not exist!");
			return null;
		}
		String[] directories = new File(directoryPath).list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		if(directories!=null)
			return Arrays.asList(directories);
		else
			return null;

	}


	public static void log(Class<?> clazz, String message) {
		//	try {
		//	URL propertiesURL = new Utils().getClass().getResource("/log4j.properties");
		//	org.apache.log4j.PropertyConfigurator.configure(propertiesURL);
		//	if (propertiesURL != null)
		//		Logger.getLogger(clazz).info(message);
		//	}
		//	catch(NullPointerException e) {
		//		System.out.println("Could not load the logger");
		System.out.println("[" + //clazz.getSimpleName() + ":" +
				System.currentTimeMillis()/1000000+ "] " + message);
		//	}
	}

	public static void logErr(Class<?> class1, String message) {
		//		try {
		//			URL propertiesURL = new Utils().getClass().getResource("/log4j.properties");
		//			org.apache.log4j.PropertyConfigurator.configure(propertiesURL);
		//			if (propertiesURL != null)
		//				Logger.getLogger(class1).error(message);
		//		}
		//		catch(NullPointerException e) {
		//			System.out.println("Could not load the logger");
		System.err.println("[" + //class1.getSimpleName() + ":" +
				System.currentTimeMillis()/1000000+ "] " + message);
		//		}
	}

	static public String readStringTextFile(String path) {
		String res = "";
		String line = "";
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
			while ((line = bufferedReader.readLine()) != null) {
				res += line + "\n";
			}
		} catch (FileNotFoundException e) {
			Utils.log(Utils.class,"Could not find the file " + path);
		}catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	static public String readTextFile(String path) {
		String res = "";
		String line = "";
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
			while ((line = bufferedReader.readLine()) != null) {
				res += line + "\n";
			}
		} catch (FileNotFoundException e) {
			Utils.log(Utils.class,"Could not find the file " + path);
		} catch (IOException e) {
			Utils.logErr(Utils.class, "Could not read the file" + path);
			//e.printStackTrace();
		}
		return res;
	}

	public static void remakeDirectory(String dirPath) throws IOException {
		deleteDirectory(new File(dirPath));
		new File(dirPath).mkdirs();
	}

	static public <T> ArrayList<T> removeDuplicates(ArrayList<T> list) {
		Set<T> hs = new HashSet<>();
		hs.addAll(list);
		list.clear();
		list.addAll(hs);
		return list;
	}

	public static long folderSize(File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	public static long numberOfFiles(File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length ++;
			else
				length += numberOfFiles(file);
		}
		return length;
	}

	static public void renameFile(String path, String newName) {
		File oldFile = new File(path);
		String newFilePath = path.substring(0, path.lastIndexOf(oldFile.getName())) + newName;
		oldFile.renameTo(new File(newFilePath));
	}

	/**
	 * Source: http://www.devx.com/tips/Tip/22124
	 *
	 * @return
	 */

	public static boolean uncompressZipFile(String zipFile, String destDir, String type) {
		java.util.zip.ZipFile zip;
		java.io.File f = new java.io.File(destDir);
		Utils.log(Utils.class, "Unzipping " + zipFile);
		if (!f.exists() && !f.mkdirs()) {
			System.err.println("Could not create the directory: " + f.getAbsolutePath());
			return false;
		}
		try {
			zip = new java.util.zip.ZipFile(zipFile);
			java.util.Enumeration enumEntries = zip.entries();
			while (enumEntries.hasMoreElements()) {
				java.util.zip.ZipEntry file = (java.util.zip.ZipEntry) enumEntries.nextElement();
				String fname = org.apache.commons.io.FilenameUtils.separatorsToSystem(file.getName ());
				f = new java.io.File (destDir + java.io.File.separator + fname);
				if (file.isDirectory()) { // if its a directory, create it
					f.mkdir();
					continue;
				}
				else
					if(type == null || fname.endsWith(type)) {
						java.io.InputStream is = zip.getInputStream(file); // get the input stream
						if (!new File(f.getParent()).exists())
							new File(f.getParent()).mkdirs();
						FileOutputStream fos = new FileOutputStream(f);
						while (is.available() > 0) { // write contents of 'is' to 'fos'
							fos.write(is.read());
						}
						fos.close();
						is.close();
					}
			}
			zip.close();
		} catch (java.io.FileNotFoundException e) {
			System.err.print("Could not find the file." + e.getMessage());
			return false;
			// e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean uncompressZipFile(String zipFile, String destDir) {
		return uncompressZipFile (zipFile, destDir, null);
	}

	public static boolean writeTextFile(String path, String... content) {
		return writeTextFile(path, Arrays.asList (content));
	}
	public static boolean writeTextFile(String path, Iterable<String> content) {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path))) {
			for (String s: content) {
				bufferedWriter.write(s);
				bufferedWriter.write(System.lineSeparator ());
			}
			bufferedWriter.close();
		} catch (java.io.FileNotFoundException ie) {
			System.err.println("The path or file " + path + " does not exist");
			return false;
		} catch (IOException e) {
			System.err.println("Could not open or write to the file " + path);
			return false;
		}
		return true;
	}


	public static String polishFilePath(String file) {
		while(file.contains("//"))
			file = file.replaceAll("//", "/");
		return file;
	}

	public static void runShellFile(String filePath) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(filePath);
		Process p = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
	}

	public void runSCGSv1(String[] command,String targetPath) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder = builder.directory(new File(targetPath));
		Process p = builder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			Utils.log(this.getClass(), line);
		}
	}

	public static boolean runSCGSv2(String outputPath, String scgsCommand) throws IOException {
		if(!new File(outputPath).exists())
			Utils.writeTextFile(outputPath, "#! /bin/bash\n" + scgsCommand);
		Process process;
		try {
			process = Runtime.getRuntime().exec("sh " + outputPath + "/symmariesCommands.command");
			process.waitFor();
			Utils.log(Utils.class, "Running Symmaries...");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			if((line=reader.readLine())==null) {
				Utils.log(Utils.class, "Could not run Symmaries.");
				return false;
			}
			else
				Utils.log(Utils.class, "Symmaries Ouptut" + "\n" + line);
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		Utils.log(Utils.class, "Symmaries finished processing the application.");
		return true;
	}

	public static boolean createFile(String file) {
		try {
			new File(file).createNewFile();
		} catch (IOException e) {
			Utils.log(Utils.class, "Could not create the file " + file);
			return false;
		}
		return true;

	}

	public static void createDirectory(String dirPath) {
		new File(dirPath).mkdirs();
	}

	public static void updateTextFile(String path, String update) {
		String content = Utils.readStringTextFile(path);
		Utils.writeTextFile(path, content + update);
	}

	public static void copy(String fileName, String dest) {
		try {
			if(new File(fileName).isDirectory())
				FileUtils.copyDirectory(new File(fileName), new File(dest));
			else
				FileUtils.copyFile(new File(fileName), new File(dest));
		} catch (FileNotFoundException e) {
			Utils.logErr(Utils.class, "The file " + fileName+ " does not exist!");
		}
		catch (IOException e) {
			Utils.logErr(Utils.class, "Could not copy the file " + fileName + " to "+ dest);
		}

	}


}
