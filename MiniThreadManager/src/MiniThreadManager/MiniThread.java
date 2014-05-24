package MiniThreadManager;

public class MiniThread extends Thread {
	
	public String threadLabel = null;
	public String statusMessage = null;
	public String endMessage = null;
	public long startTime = 0;
	public long killTime = 0;	
	public long endTime = 0;
	
	public static String toCSVHeader() {
		return "threadLabel,endMessage\n";
	}
	
	public String toCSV() {
		String out = "";

		out += threadLabel;
		out += ",";
		out += endMessage;
		out += ",";
		
		out += "\n";

		return out;
	}
}