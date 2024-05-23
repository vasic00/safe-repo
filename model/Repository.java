package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class Repository {

	private final String opensslConfigPath;
	private final String CAPath;
	private final String certsPath;
	private final String databasePath;
	private final String privatePath;
	private final String CRLPath;
	private final int minRandomNumber;
	private final int maxRandomNumber;
	
	private final HashMap<String, User> users = new HashMap<String, User>();
	
	public Repository() throws Exception
	{
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("config.properties");
		properties.load(input);
		opensslConfigPath = properties.getProperty("opensslConfigPath");
		List<String> opensslConfigLines = Files.readAllLines(Paths.get(opensslConfigPath));
		
		String tempCAPath = null;
		String tempCertsPath = null;
		String tempDirPath = null;
		String tempDatabasePath = null;
		String tempPrivatePath = null;
		String tempCRLPath = null;
		
		int counter = 5;
		for (String line : opensslConfigLines)
		{
			if (!line.contains("$dir") && line.contains("dir") && line.contains("="))
				tempDirPath = line.split("=")[1].split("#")[0].trim();
			
			if (line.contains("certificate") && line.contains("$dir"))
			{
				String[] tempArr = line.split("=");
				tempCAPath = tempArr[1].trim();
				tempCAPath = tempCAPath.split("\\s")[0];
				tempCAPath = tempCAPath.replace("$dir", tempDirPath);
				counter--;
			}
			
			if (!line.contains("new") && line.contains("certs") && line.contains("$dir"))
			{
				String[] tempArr = line.split("=");
				tempCertsPath = tempArr[1].trim();
				tempCertsPath = tempCertsPath.split("\\s")[0];
				tempCertsPath = tempCertsPath.replace("$dir", tempDirPath);
				counter--;
			}
			
			if (line.contains("database") && line.contains("$dir"))
			{
				String[] tempArr = line.split("=");
				tempDatabasePath = tempArr[1].trim();
				tempDatabasePath = tempDatabasePath.split("\\s")[0];
				tempDatabasePath = tempDatabasePath.replace("$dir", tempDirPath);
				counter--;
			}
			
			if (line.contains("private") && line.contains("$dir"))
			{
				String[] tempArr = line.split("=");
				tempPrivatePath = tempArr[1].trim();
				tempPrivatePath = tempPrivatePath.split("\\s")[0];
				tempPrivatePath = tempPrivatePath.replace("$dir", tempDirPath);
				tempArr = tempPrivatePath.split("/");
				tempPrivatePath = tempPrivatePath.replace("/" + tempArr[tempArr.length - 1], "");
				counter--;
			}
			
			if (line.contains("crl_dir") && line.contains("$dir"))
			{
				String[] tempArr = line.split("=");
				tempCRLPath = tempArr[1].trim();
				tempCRLPath = tempCRLPath.split("\\s")[0];
				tempCRLPath = tempCRLPath.replace("$dir", tempDirPath);
				counter--;
			}
			if (counter == 0)
				break;
		}
		CAPath = tempCAPath;
		certsPath = tempCertsPath;
		databasePath = tempDatabasePath;
		privatePath = tempPrivatePath;
		CRLPath = tempCRLPath;
		
		minRandomNumber = Integer.parseInt(properties.getProperty("minRandomNumber"));
		maxRandomNumber = Integer.parseInt(properties.getProperty("maxRandomNumber"));
		input.close();
		
		File previousUsersDir = new File("users");
		String[] previousUsers = previousUsersDir.list();
		
		if (previousUsers.length > 0)
		{
			for (String user : previousUsers)
			{
				user = "users/" + user;
				ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(user));
				User previousUser = (User)objectInputStream.readObject();
				users.put(previousUser.getUserName(), previousUser);
				objectInputStream.close();
			}
		}
	}
	
	public void start() throws Exception
	{
		Scanner scanner = new Scanner(System.in);
		String nextLine = null;
		
		Runtime runtime = Runtime.getRuntime();
		Process nextProcess = null;
		BufferedReader processResult = null;
		
		Set<String> allUserNames = users.keySet();
		
		boolean over = false;
		
		mainLoop:
		while (!over)
		{
			System.out.println("Log in or Sign Up?");
			System.out.println("Type l for log in, s for sign up");
			nextLine = scanner.nextLine();
			
			if (nextLine.equals("s"))
			{
				String newUserName = null;
				String newPassword = null;
				
				System.out.println("Username:");
				while (true)
				{
					nextLine = scanner.nextLine();
					if (allUserNames.contains(nextLine))
					{
						System.out.println("Username is already taken. Please specify a different username.");
						continue;
					}
					newUserName = nextLine;
					break;
				}
				
				System.out.println("Password:");
				nextLine = scanner.nextLine();
				String tempPass = nextLine;
				nextProcess = runtime.exec(shell("openssl", "passwd", nextLine));
				nextProcess.waitFor();
				processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
				newPassword = processResult.readLine();
				processResult.close();
				
				System.out.println("Where do you want your private key to be placed? Type d for default path (directory private from openssl.cnf)");
				nextLine = scanner.nextLine();
				if (nextLine.equals("d"))
					nextLine = privatePath + "/" + newUserName + ".key";
				
				nextProcess = runtime.exec(shell("openssl", "genrsa", "-aes128", "-passout", "pass:" + tempPass, "-out", nextLine, "2048"));
				nextProcess.waitFor();
				nextProcess = runtime.exec(shell("openssl", "req", "-new", "-key", nextLine, 
						"-passin", "pass:" + tempPass, "-out", newUserName + ".req", "-config", opensslConfigPath, "-subj", 
						"/C=BA/ST=RS/L=Banja Luka/O=Elektrotehnicki fakultet/OU=ETF/CN=" + newUserName + "/emailAddress=" + newUserName + "@mail.com"));
				nextProcess.waitFor();
				
				System.out.println("Where do you want your certificate to be placed? Type d for default path (directory certs from openssl.cnf)");
				nextLine = scanner.nextLine();
				if (nextLine.equals("d"))
					nextLine = certsPath + "/" + newUserName + ".crt";
				
				nextProcess = runtime.exec(shell("openssl", "ca", "-in", newUserName + ".req", "-out", nextLine, "-config", opensslConfigPath, "-batch"));
				nextProcess.waitFor();
				nextProcess = runtime.exec(shell("openssl", "x509", "-in", nextLine, "-noout", "-text"));
				nextProcess.waitFor();
				processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
				
				String s = null;
				while ((s = processResult.readLine()) != null)
				{
					if (s.contains("Subject:") || s.contains("subject="))
					{
						String[] tempStringArray = s.split(",");
						User newUser = new User(newUserName, newPassword, tempStringArray[0].split(" = ")[1], tempStringArray[1].split(" = ")[1],
								tempStringArray[2].split(" = ")[1], tempStringArray[3].split(" = ")[1], tempStringArray[4].split(" = ")[1],
								tempStringArray[5].split(" = ")[1], tempStringArray[6].split(" = ")[1]);
						
						users.put(newUserName, newUser);
						
						ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("users/" + newUserName));
						objectOutputStream.writeObject(newUser);
						objectOutputStream.close();
						break;
					}
				}
				processResult.close();
				
				Files.deleteIfExists(Paths.get(newUserName + ".req"));
				Files.createDirectory(Paths.get("docs/" + newUserName));
				
			}
			else if (nextLine.equals("l"))
			{
				System.out.println("Path to your certificate file.");
				String certPath = null;
				boolean certSuspended = false;
				String tempSerialNumber = null;
				
				certCheck:
				while (true)
				{
					certPath = scanner.nextLine();
					nextProcess = runtime.exec(shell("openssl", "verify", "-CAfile", CAPath, certPath));
					nextProcess.waitFor();
					processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
					nextLine = processResult.readLine();
					processResult.close();
					if (nextLine == null || !nextLine.contains("OK"))
					{
						System.out.println("Invalid certificate. Please try again.");
						continue;
					}
					nextProcess = runtime.exec(shell("openssl", "x509", "-in", certPath, "-noout", "-text"));
					nextProcess.waitFor();
					processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
					
					while ((tempSerialNumber = processResult.readLine()) != null)
					{
						if (tempSerialNumber.contains("Serial Number"))
						{
							String serialNumber = tempSerialNumber.split("\\(")[1].split("\\)")[0].strip().split("x")[1].toUpperCase();
							tempSerialNumber = serialNumber;
							long tempLong = Files.lines(Paths.get(databasePath)).filter(line -> line.startsWith("R") && (line.contains("	" + serialNumber + "	")
									|| line.contains("	" + "0" + serialNumber + "	"))).count();
							if (tempLong > 0)
							{
								certSuspended = true;
								System.out.println("Your certificate is suspended.");
								System.out.println("Upon entering your credentials successfully, the suspension will be removed.");
								System.out.println("You can choose to continue with login process, or restart the process. Type anything to continue, or r to restart.");
								processResult.close();
								nextLine = scanner.nextLine();
								if (nextLine.equals("r"))
									continue mainLoop;
							}
							processResult.close();
							break certCheck;
						}
					}
					
				}
				
				int counter = 3;
				System.out.println("Username:");
				usernameLoop:
				while (true)
				{
					if (counter == 0)
					{
						System.out.println("Login process failed too many times. Entire process has been restarted.");
						if (!certSuspended)
						{
							nextProcess = runtime.exec(shell("openssl", "ca", "-revoke", certPath, "-crl_reason", "certificateHold", "-config", opensslConfigPath));
							nextProcess.waitFor();
							nextProcess = runtime.exec(shell("openssl", "ca", "-gencrl", "-out", CRLPath + "/list" + Files.list(Paths.get(CRLPath)).count(), 
									"-config", opensslConfigPath));
							nextProcess.waitFor();
						}
						continue mainLoop;
					}
					nextLine = scanner.nextLine();
					if (!allUserNames.contains(nextLine))
					{
						System.out.println("No such username exists. Please try again.");
						counter--;
						continue;
					}
					else
					{
						nextProcess = runtime.exec(shell("openssl", "x509", "-in", certPath, "-noout", "-text"));
						nextProcess.waitFor();
						processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
						String s = null;
						while ((s = processResult.readLine()) != null)
						{
							if (s.contains("Subject:") || s.contains("subject="))
							{
								processResult.close();
								String[] tempStringArray = s.split(",");
								if (users.get(nextLine).equals(new User(nextLine, "", tempStringArray[0].split(" = ")[1], tempStringArray[1].split(" = ")[1],
								tempStringArray[2].split(" = ")[1], tempStringArray[3].split(" = ")[1], tempStringArray[4].split(" = ")[1],
								tempStringArray[5].split(" = ")[1], tempStringArray[6].split(" = ")[1])))
									break usernameLoop;
								else
								{
									counter--;
									
									if (counter == 0)
									{
										System.out.println("Login process failed too many times. Entire process has been restarted.");
										if (!certSuspended)
										{
											nextProcess = runtime.exec(shell("openssl", "ca", "-revoke", certPath, "-crl_reason", "certificateHold", "-config", opensslConfigPath));
											nextProcess.waitFor();
											nextProcess = runtime.exec(shell("openssl", "ca", "-gencrl", "-out", CRLPath + "/list" + Files.list(Paths.get(CRLPath)).count(), 
													"-config", opensslConfigPath));
											nextProcess.waitFor();
										}
										continue mainLoop;
									}
									
									System.out.println("The certificate does not belong to the user.");
									System.out.println("Do you wish to enter a different username? If yes, type y.");
									System.out.println("Type n if you wish to restart the login/signup process.");
									nextLine = scanner.nextLine();
									if (nextLine.equals("y"))
										continue usernameLoop;
									if (nextLine.equals("n"))
										continue mainLoop;
								}
							}
						}
					}
					
				}
			
				User user = users.get(nextLine);
				String salt = user.getPassword().substring(0,2);
				System.out.println("Password:");
				String rawPassword = null;
				while (true)
				{
					rawPassword = scanner.nextLine();
					nextProcess = runtime.exec(shell("openssl", "passwd", "-salt", salt, rawPassword));
					nextProcess.waitFor();
					processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
					
					if (!user.getPassword().equals(processResult.readLine()))
					{
						counter--;
						
						if (counter == 0)
						{
							System.out.println("Login process failed too many times. Entire process has been restarted.");
							if (!certSuspended)
							{
								nextProcess = runtime.exec(shell("openssl", "ca", "-revoke", certPath, "-crl_reason", "certificateHold", "-config", opensslConfigPath));
								nextProcess.waitFor();
								nextProcess = runtime.exec(shell("openssl", "ca", "-gencrl", "-out", CRLPath + "/list" + Files.list(Paths.get(CRLPath)).count(), 
										"-config", opensslConfigPath));
								nextProcess.waitFor();
							}
							continue mainLoop;
						}
						
						System.out.println("Invalid password for given username. Please try again.");
						continue;
					}
					processResult.close();
					break;
				}
				
				if (certSuspended)
				{
					List<String> lines = Files.readAllLines(Paths.get(databasePath));
					String newLine = "";
					int index = -1;
					for (int i = 0; i < lines.size(); i++)
					{
						if (lines.get(i).startsWith("R") && (lines.get(i).contains("	" + tempSerialNumber + "	")
								|| lines.get(i).contains("	" + "0" + tempSerialNumber + "	")))
						{
							String[] tempArr = lines.get(i).split("	");
							tempArr[2] = "";
							tempArr[0] = "V";
							for (String s : tempArr)
							{
								newLine = newLine + "	" + s;
							}
							newLine = newLine.strip();
							index  = i;
							break;
						}
					}
					BufferedWriter bw = new BufferedWriter(new FileWriter(databasePath, false));
					for (int i = 0; i < lines.size(); i++)
					{
						if (i == index)
							bw.write(newLine);
						else
							bw.write(lines.get(i));
						if (i != lines.size() - 1)
							bw.newLine();
					}
					bw.close();
					
					nextProcess = runtime.exec(shell("openssl", "ca", "-gencrl", "-out", CRLPath + "/list" + Files.list(Paths.get(CRLPath)).count(), 
							"-config", opensslConfigPath));
					nextProcess.waitFor();
				}
				
				work(user, rawPassword, certPath, runtime, processResult, scanner);
			}
			
		}
		
	}
	
	private void work(User user, String rawPassword, String certPath, Runtime runtime, BufferedReader processResult, Scanner scanner) throws Exception
	{
		Process nextProcess = null;
		String nextLine = null;
		String keyPath = null;
		
		String docs = "docs/" + user.getUserName();
		System.out.println("Your documents:");
		List<Path> listOfDocs = Files.list(Paths.get(docs)).collect(Collectors.toList());
		
		for (Path p : listOfDocs)
		{
			System.out.println(p.getFileName());
		}
		
		System.out.println("Enter the path of your private key.");
		
		while (true)
		{
			keyPath = scanner.nextLine();
			nextProcess = runtime.exec(shell("openssl", "rsa", "-in", keyPath, "-noout", "-text", "-passin", "pass:" + rawPassword));
			nextProcess.waitFor();
			processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
			nextLine = processResult.readLine();
			processResult.close();
			
			if (nextLine == null || nextLine.contains("unable") || nextLine.contains("Unable"))
				System.out.println("Unable to load the key. Please specify a path again.");
			else break;
		}
		
		mainLoop:
		while (true)
		{
			System.out.println("Do you want to download or upload files? Type d for download, or u for upload.");
			nextLine = scanner.nextLine();
			
			if (nextLine.equals("u"))
			{
				System.out.println("Enter the path of your file.");
				nextLine = scanner.nextLine();
				String[] s = nextLine.split("/");
				
				for (Path p : listOfDocs)
				{
					if (p.getFileName().toString().equals(s[s.length - 1]))
					{
						System.out.println("File with the same name already exists.");
						continue mainLoop;
					}
				}

				Files.createFile(Paths.get(docs + "/" + s[s.length - 1] + ".txt"));
				
				byte[] bytes = Files.readAllBytes(Paths.get(nextLine));
				int numberOfSegments = new Random().nextInt(minRandomNumber, maxRandomNumber + 1);
				int numberOfBytesInSegment = bytes.length / numberOfSegments;
				int leftOverBytes = bytes.length % numberOfSegments;
				
				int byteCounter = 0;
				
				Set<Path> setOfExistingSegments = Files.list(Paths.get("storage")).collect(Collectors.toSet());
				
				for (int i = 0; i < numberOfSegments; i++)
				{
					String segmentName = null;
					while(true)
					{
						nextProcess = runtime.exec(shell("openssl", "rand", "-hex", "4"));
						nextProcess.waitFor();
						processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
						segmentName = processResult.readLine();
						processResult.close();
						if (!setOfExistingSegments.contains(Paths.get("storage/" + segmentName)))
							break;
					}
					
					String temp = "storage/" + segmentName + ".txt";
					
					Files.createFile(Paths.get(temp));
					
					FileOutputStream fos = new FileOutputStream(Paths.get(temp).toFile(), true);
					if (i == numberOfSegments - 1 && leftOverBytes > 0)
						numberOfBytesInSegment += leftOverBytes;
					fos.write(bytes, byteCounter, numberOfBytesInSegment);
					fos.close();
					
					nextProcess = runtime.exec(shell("openssl", "dgst", "-sha1", "-sign", keyPath, "-passin", "pass:" + rawPassword, "-out", "signatures/" + segmentName, temp));
					nextProcess.waitFor();
					nextProcess = runtime.exec(shell("openssl", "rand", "-hex", "16"));
					nextProcess.waitFor();
					processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
					String tempKey = processResult.readLine();
					processResult.close();
					nextProcess = runtime.exec(shell("openssl", "enc", "-in", temp, "-out", "storage/" + segmentName, "-aes-128-ecb", "-K", tempKey));
					nextProcess.waitFor();
					
					String tempAESKeyPath = "keys/" + segmentName + ".txt";
					Files.createFile(Paths.get(tempAESKeyPath));
					Files.writeString(Paths.get(tempAESKeyPath), tempKey);
					nextProcess = runtime.exec(shell("openssl", "rsautl", "-encrypt", "-in", tempAESKeyPath, "-inkey", keyPath,
							"-out", "keys/" + segmentName, "-passin", "pass:" + rawPassword));
					nextProcess.waitFor();
					
					Files.deleteIfExists(Paths.get(tempAESKeyPath));
					
					Files.writeString(Paths.get(docs + "/" + s[s.length - 1] + ".txt"), segmentName + System.lineSeparator(), StandardOpenOption.APPEND);
					
					Files.deleteIfExists(Paths.get(temp));
					
					byteCounter = byteCounter + numberOfBytesInSegment;
					
				}
				
				nextProcess = runtime.exec(shell("openssl", "rsautl", "-encrypt", "-in", docs + "/" + s[s.length - 1] + ".txt", "-out",
						docs + "/" + s[s.length - 1], "-inkey", keyPath, "-passin", "pass:" + rawPassword));
				nextProcess.waitFor();
				
				Files.deleteIfExists(Paths.get(docs + "/" + s[s.length - 1] + ".txt"));
				
			}
			else if (nextLine.equals("d"))
			{
				System.out.println("Enter file name.");
				nextLine = scanner.nextLine();
				
				boolean fileDoesntExist = true;
				for (Path p : listOfDocs)
				{
					if (p.getFileName().toString().equals(nextLine))
					{
						fileDoesntExist = false;
						break;
					}
				}
				if (fileDoesntExist)
				{
					System.out.println("No file by that name.");
					continue mainLoop;
				}
				
				nextProcess = runtime.exec(shell("openssl", "rsautl", "-decrypt", "-in", docs + "/" + nextLine, "-inkey", keyPath, 
						"-out", docs + "/" + nextLine + ".txt","-passin", "pass:" + rawPassword));
				nextProcess.waitFor();	
				List<String> segmentsOfFile = Files.readAllLines(Paths.get(docs + "/" + nextLine + ".txt"));
				Files.deleteIfExists(Paths.get(docs + "/" + nextLine + ".txt"));
				
				byte[] finalBytes = null;
				byte[] tempBytes = null;
				
				for (String s : segmentsOfFile)
				{
					nextProcess = runtime.exec(shell("openssl", "rsautl", "-decrypt", "-in", "keys/" + s,
							"-inkey", keyPath, "-passin", "pass:" + rawPassword));
					nextProcess.waitFor();
					processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
					String segmentKey = processResult.readLine();
					processResult.close();
					nextProcess = runtime.exec(shell("openssl", "enc", "-d", "-aes-128-ecb", "-in", "storage/" + s, "-K", segmentKey,
							"-out", "storage/" + s + ".txt"));
					nextProcess.waitFor();
					
					tempBytes = Files.readAllBytes(Paths.get("storage/" + s + ".txt"));
					
					if (finalBytes == null)
						finalBytes = tempBytes;
					else finalBytes = concat(finalBytes, tempBytes);
					
					nextProcess = runtime.exec(shell("openssl", "dgst", "-sha1", "-prverify", keyPath, "-passin", "pass:" + rawPassword,
							"-signature", "signatures/" + s, "storage/" + s + ".txt"));
					nextProcess.waitFor();
					
					processResult = new BufferedReader(new InputStreamReader(nextProcess.getInputStream()));
					
					if (!processResult.readLine().contains("OK"))
						System.out.println("Your file has been compromised. Segment: " + s);
					
					processResult.close();
					Files.deleteIfExists(Paths.get("storage/" + s + ".txt"));
					
				}
				
				System.out.println("Where do you want your downloaded file to be placed?");
				nextLine = scanner.nextLine();
				
				Files.write(Paths.get(nextLine), finalBytes);
			}
		}
		
	}
	
	private String[] shell(String... strings)
	{
		String[] commands = new String[strings.length];
		System.arraycopy(strings, 0, commands, 0, strings.length);
		return commands;
	}
	
	private byte[] concat(byte[] first, byte[] second)
	{
		  byte[] result = Arrays.copyOf(first, first.length + second.length);
		  System.arraycopy(second, 0, result, first.length, second.length);
		  return result;
	}
}
