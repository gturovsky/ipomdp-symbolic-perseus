package masg.catchProblem.visualization;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TraceViewer {

	static int a1Pos=1;
	static int a2Pos=2;
	static int wPos=2;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
		
		String line = reader.readLine();
		
		int step = 1;
		int colocations=0;
		
		while(line!=null)
		{
			String tokens[] = line.split(" ");
			a1Pos=Integer.parseInt(tokens[0]);
			a2Pos=Integer.parseInt(tokens[1]);
			wPos=Integer.parseInt(tokens[2]);
			
			if(a1Pos==wPos || a2Pos==wPos)
				colocations++;
			
			System.out.println("State #" + step);
			System.out.println("Colocations:" + colocations);
			System.out.println();
			drawTextGrid(5,5);
			System.out.println();
			
			line = reader.readLine();
			step++;
		}
		
	}
	
	public static void textViewer()
	{
		
	}
	
	public static void drawTextGrid(int height, int width)
	{
		int pos=1;
		
		for(int j=0;j<width;j++)
		{
			System.out.print("-----");
		}
		
		System.out.println();
		
		for(int i=0;i<height;i++)
		{	
			int tempPos=pos;
			
			for(int j=0;j<width;j++)
			{
				System.out.print("|");
				if(a1Pos==tempPos)
					System.out.print("1");
				else
					System.out.print(" ");
				
				if(wPos==tempPos)
					System.out.print("W");
				else
					System.out.print(" ");
				
				System.out.print("  ");
				tempPos++;
				
			}
			
			System.out.println("|");
			tempPos = pos;
			
			for(int j=0;j<width;j++)
			{
				System.out.print("|");
				if(a2Pos==tempPos)
					System.out.print("2");
				else
					System.out.print(" ");
				
				System.out.print(" ");
				
				System.out.print("  ");
				tempPos++;
			}
			System.out.println("|");
			
			for(int j=0;j<width;j++)
			{
				System.out.print("-----");
			}
			
			System.out.println();
			pos = tempPos;
		}
	}

}
