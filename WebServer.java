package group13;

//
// Multithreaded Java WebServer
// (C) 2001 Anders Gidenstam
// (based on a lab in Computer Networking: ..)
//

import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetAddress.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class WebServer {

	/**
	 * Open a socket for a specific port and wait requests from clients
	 * Everytime a request is received, a new thread is created for each request.
	 */
	public static void main(String argv[]) throws Exception {
		// Set port number
		int port = 8080;

		// Establish the listening socket
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("Port number is: " + serverSocket.getLocalPort());

		// Wait for and process HTTP service requests
		while (!serverSocket.isClosed()) {
			// Wait for TCP connection
			Socket requestSocket = serverSocket.accept();
			requestSocket.setSoLinger(true, 5);

			// Create an object to handle the request
			HttpRequest request = new HttpRequest(requestSocket);

			// request.run()

			// Create a new thread for the request
			Thread thread = new Thread(request);

			// Start the thread
			thread.start();
		}
	}
}

final class HttpRequest implements Runnable {
	// Constants
	// Recognized HTTP methods
	final static class HTTP_METHOD {
		final static String GET = "GET";
		final static String HEAD = "HEAD";
		final static String POST = "POST";
	}

	final static String HTTPVERSION = "HTTP/1.0";
	final static String CRLF = "\r\n";
	Socket socket;

	final private String currentDir = System.getProperty("user.dir");
	private String resourceName = null;
	private String resourceLastModified = null;
	private String httpVersion = "HTTP/1.0";
	private int statusCode = -1;
	private long resourceLength = -1;

	// Constructor
	public HttpRequest(Socket socket) throws Exception {
		this.socket = socket;
	}

	// Implements the run() method of the Runnable interface
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/**
	 * Process a HTTP request
	 * Read the first line of the request and split it with space.
	 * Then check if there are legal values in the method, resource and HTTP version
	 * and respond according to the method.
	 */	
	private void processRequest() throws Exception {
		// Get the input and output streams of the socket.
		InputStream ins = socket.getInputStream();
		DataOutputStream outs = new DataOutputStream(socket.getOutputStream());

		// Set up input stream filters
		BufferedReader br = new BufferedReader(new InputStreamReader(ins));

		// Get the request line of the HTTP request
		String requestLine = br.readLine();

		// Display the request line
		//System.out.println();
		//System.out.println("Request:");
		//System.out.println("  " + requestLine);

		String[] lineArray = requestLine.split(" ");


		if (lineArray.length != 3 || hasEmptyValue(lineArray)) {
			statusCode = 400;
			writeLine(getStatusLine(statusCode), outs);
			writeLine("\n", outs);
			writeLine("Date: " + getDate(), outs);
			writeLine("\n", outs);
		} else {
			String method = lineArray[0];
			String resource = lineArray[1];
			httpVersion = lineArray[2];

			System.out.println("method: " + method);
			System.out.println("resource: " + resource);
			System.out.println("httpVersion: " + httpVersion);

			switch(method) {
				case HTTP_METHOD.HEAD:	
					sendHeader(method, resource, outs);
					break;
				case HTTP_METHOD.GET:
					sendHeader(method, resource, outs);
					if (statusCode == 200){
						sendBytes(new FileInputStream(new File(currentDir, resourceName)), outs);	
					}
					break;
				case HTTP_METHOD.POST:	
					sendHeader(method, resource, outs);
					break;
				default:
					sendHeader(method, resource, outs);
			}
		}

		socket.shutdownOutput();
				
		// Close streams and sockets
		outs.close();
		br.close();
		socket.close();
	}

	/**
	* Check if there is empty value.
	*
	* @param input the array to be checked.
	* @return           true if there are empty values in the array.
	*/
	private boolean hasEmptyValue(String[] input){
		for (int i=0; i < input.length; i++){
			if (input[i] == "" || input[i].matches("") || input[i] == null){
				System.out.println("input[i] is empty or null");
				return true;
			} else if (input[i].matches("\\s")){
				System.out.println("input[i] is space");
				return true;
			}
		}
		return false;
	}


	/**
	* Check if the method is valid.
	* Currently support HEAD, GET and POST methods.
	*
	* @param method a HTTP method
	* @return               true if the method exists
	*/
	private boolean isValidMethod(String method) {
		if (method.equals(HTTP_METHOD.HEAD)
			|| method.equals(HTTP_METHOD.GET)
			|| method.equals(HTTP_METHOD.POST)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	* Check if the resource is valid.
	* If the first character is '/' then it's vaild resource
	*
	* @param resource an relative URL indicates the location of the resource
	* @return                 true if the resouce is valid
	*/
	private boolean isValidResource(String resource) {
		if (resource.charAt(0) == '/') {
			return true;
		} else {
			return false;
		}
	}

	/**
	* Check if the resource exists.
	* If the resource exists, save the length and last modified date.
	*
	* @param resource an relative URL indicates the location of the resource
	* @return true if the resouce exists
	*/
	private boolean resourceExist(String resource){
		resourceName = resource.substring(1);
		File fileResource = new File(currentDir, resourceName);
		if(fileResource.exists() && !fileResource.isDirectory()) {
			resourceLength = fileResource.length();
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy kk:mm:ss ZZZ");
			resourceLastModified = sdf.format(fileResource.lastModified());
			return true;
		} else {
			return false;
		}
	}

	/**
	* Return the status line according to the status code.
	*
	* @param code status code
	* @return           the status line
	*/
	private String getStatusLine(int code){
		switch (code){
			case 200:
				return httpVersion + " " + code + " OK";
			case 301:
				return httpVersion + " " + code +  "Moved Permanently";
			case 400:
				return httpVersion + " " + code + " Bad Request";
			case 404:
				return httpVersion + " " + code + " Not Found";
			case 500:	
				return httpVersion + " " + code + " Internal Server Error";
			case 501:
				return httpVersion + " " + code + " Not Implemented";
			default:
				return "Invalid status code";
		}
	}

	/**
	* Get the current date and return in string
	*
	* @return           the current date
	*/
	private String getDate(){
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d kk:mm:ss yyyy");
		return sdf.format(cal.getTime());
	}

	/**
	* Write a line to the client.
	*
	* @param line  the information to be sent
	* @param outs the output stream to be sent to
	*/
	private void writeLine(String line, DataOutputStream outs){
		try {
			outs.writeBytes(line);
		} catch (IOException e) { 
			e.printStackTrace(); 
		}
	}

	/**
	* Check if the method and resource are legal and send Header.
	*
	* @param method   a HTTP method
	* @param resource an relative URL indicates the location of the resource
	* @param outs        the output stream to be sent to
	*/
	private void sendHeader(String method, String resource, DataOutputStream outs) {
		if (method.equals(HTTP_METHOD.POST)) {
			statusCode = 501;
			System.out.println("method not implemented");
		} else if (!isValidMethod(method) ) {
			statusCode = 400;
			System.out.println("invalid method");
		} else if (!isValidResource(resource) ) {
			statusCode = 400;
			System.out.println("invalid resource");
		} else if (!resourceExist(resource)) {
			statusCode = 404;			
			System.out.println("resource not exist");
		} else {
			statusCode = 200;	
		}

		writeLine(getStatusLine(statusCode), outs);
		writeLine("\n", outs);
		writeLine("Date: " + getDate(), outs);
		writeLine("\n", outs);

		if (statusCode == 200){
			writeLine("Location: " + resource, outs);
			writeLine("\n", outs);
			writeLine("Server: WebServer/1.0", outs);
			writeLine("\n", outs);
			writeLine("Allow: " + HTTP_METHOD.GET + " " + HTTP_METHOD.HEAD, outs);
			writeLine("\n", outs);
			writeLine("Content-Length: " + resourceLength, outs);
			writeLine("\n", outs);
			writeLine("Content-Type: " + contentType(resourceName), outs);
			writeLine("\n", outs);
			writeLine("Last-Modified: on, " + resourceLastModified, outs);
			writeLine("\n\n", outs);
		}
		
	}

	/**
	* Send bytes from the file.
	*
	* @param fins   a file input stream
	* @param outs  the output stream to be sent to
	*/
	private static void sendBytes(FileInputStream fins, DataOutputStream outs) throws Exception {
		// Coopy buffer
		byte[] buffer = new byte[1024];
		int bytes = 0;

		while ((bytes = fins.read(buffer)) != -1) {
			//outs.writeBytes(buffer, 0, bytes);
			outs.write(buffer, 0, bytes);
		}
	}

	/**
	* Set the contentType according to the file name.
	*
	* @param fileName the file name
	* @return                 the content type
	*/
	private static String contentType(String fileName) {
		if (fileName.toLowerCase().endsWith(".htm") || fileName.toLowerCase().endsWith(".html")) {
			return "text/html";
		} else if (fileName.toLowerCase().endsWith(".gif")) {
			return "image/gif";
		} else if (fileName.toLowerCase().endsWith(".jpg")) {
			return "image/jpeg";
		} else {
			return "application/octet-stream";
		}
	}
	
}
