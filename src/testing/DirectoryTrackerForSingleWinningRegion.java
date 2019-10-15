package testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multitierControllerModel.GameSpace.ConcurrentSystemModelMaker;
import multitierControllerModel.GameSpace.DifferentialTransitionParser;
import multitierControllerModel.GameSpace.DiscreteTransitionParser;
import multitierControllerModel.GameSpace.GameModel;
import multitierControllerModel.Model.Model;

public class DirectoryTrackerForSingleWinningRegion
{
	private File directory;
	private List<String> candidate;
	private List<String> cas;
	private List<Model[]> reqs;
	private List<GameModel> cmList;
	private boolean firstCheck;
	private String sep=File.separator;
	private Model e;
	private Model c;
	private String targetError="";
	private int[] checkerForMRP;
	private GameModel Game;
	private DiscreteTransitionParser distp;
	private DifferentialTransitionParser difftp;

	public DirectoryTrackerForSingleWinningRegion(String directory)
	{
		this.directory=new File(directory);
		if(this.directory.exists())
		{
			candidate=new ArrayList<String>(Arrays.asList(this.directory.list()));
			candidate.remove("Controller");
			cas=new ArrayList<String>();
			cmList=new ArrayList<GameModel>();
		}
		firstCheck=false;
	}

	private File searchEnvInDirectory()
	{
		return new File(directory.getAbsolutePath()+sep+"Controller"+sep+"ENV.txt");
	}

	private void setControllableAction()
	{
		File caFile=new File(directory.getAbsolutePath()+sep+"Controller"+sep+"CA.txt");
		try
		{
			BufferedReader reader =new BufferedReader(new FileReader(caFile));
			String c;
			while((c=reader.readLine())!=null)
			{
				//System.out.println("ControllablAction:"+c);
				cas.add(c);
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		ConcurrentSystemModelMaker.setControllableAction(cas);
	}

	private List<Model[]> getReqs()
	{
		FSP_FileReader reader;
		Map<String,Model> originalReqs=new HashMap<String,Model>();
		File reqsFile=new File(directory.getPath()+sep+"Controller"+sep+"req_levels.txt");
		String c;
		List<Model[]> reqs =new ArrayList<Model[]>();

		for(int i=0;i<candidate.size();i++)
		{
			if (new File(directory.getPath()+sep+candidate.get(i)).isFile())
			{
				reader=new FSP_FileReader(directory.getPath()+sep+candidate.get(i));
//				System.out.println(candidate.get(i));
				originalReqs.put(candidate.get(i),reader.getModel());
			}
		}
		if(reqsFile.isFile())
		{
			try
			{
				BufferedReader bReader=new BufferedReader(new FileReader(reqsFile));
				while((c=bReader.readLine())!=null)
				{
					String[] reqStrings=c.split(",");
//					System.out.println("DirectoryTracker_getReqs:"+c);
					Model[] reqMoni=new Model[reqStrings.length];
					for(int i=0;i<reqStrings.length;i++)
					{
						reqMoni[i]=originalReqs.get(reqStrings[i]+".txt");
					}
					reqs.add(reqMoni);
				}
				bReader.close();

			}
			catch (IOException e)
			{
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			}

		}
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		if(reqs==null||reqs.isEmpty())System.out.println("no reqirement level file");
		return reqs;
	}

	private String setDegradationTargets()
	{
		File caFile=new File(directory.getPath()+sep+"Controller"+sep+"DegradationTargets.txt");
		try
		{
			BufferedReader reader =new BufferedReader(new FileReader(caFile));
			String c;
			checkerForMRP=new int[(candidate.size()/32)+1];
			for(int i=0;i<checkerForMRP.length;i++)
				checkerForMRP[i]=0;
			while((c=reader.readLine())!=null)
			{
				int i=candidate.indexOf(c);
				if(i!=-1)
				{
					if(targetError=="")
						targetError="ERROR"+i;
					else
						targetError=targetError+"&&ERROR"+i;
					checkerForMRP[i/32]+=(1<<(i%32));
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//System.out.println(targetError);
		return targetError;
	}

	public void checkDCSUEnv(String controller, int firstlevel, int lastlevel)
	{
		int check;
		long start=System.currentTimeMillis();
		FSP_FileReader reader=new FSP_FileReader(directory.getPath()+sep+"Controller"+sep+controller);//simulate用のController
		c=reader.getModel();
		long stop = System.currentTimeMillis();
		System.out.println("Spending time of Simulate Controller: " + (stop-start) + "ms");
		long realstart = System.currentTimeMillis();
		for(int i=firstlevel;i<lastlevel;i++)
		{
			start=System.currentTimeMillis();
			getUpdateCModelFromDirectory(i);
			stop = System.currentTimeMillis();
			System.out.println("Spending time of Game Create: " + (stop-start) + "ms");
			distp = new DiscreteTransitionParser(Game, lastlevel-i);
			firstCheck=true;
			check=distp.checkDCSUEnv(c);
			if(check==0 || check==-1)
			{
				continue;
			}
			else
			{
				break;
			}
		}
		long realstop = System.currentTimeMillis();
		System.out.println("Real Spending time: " + (realstop-realstart) + "ms");
		return;
	}

	public GameModel getUpdateCModelFromDirectory(int level)
	{
		File env;
		reqs=new ArrayList<Model[]>();
		if((env=searchEnvInDirectory())==null)
		{
			return null;
		}
		setControllableAction();
		long start = System.currentTimeMillis();
		FSP_FileReader reader;
		reader=new FSP_FileReader(env);
		e=reader.getModel();
		reqs=getReqs();
		targetError=setDegradationTargets();
		long stop = System.currentTimeMillis();
		System.out.println("File Reader Time: " + (stop-start) + "ms");
		long start2 = System.currentTimeMillis();
		Game = ConcurrentSystemModelMaker.makeConcurrentSystem(e, reqs.get(level),cas);
		long stop2 = System.currentTimeMillis();
		System.out.println("Game Create Time: " + (stop2-start2) + "ms");
		return Game;
	}

	public int[][] checkDesignTimeSynthesis()
	{
		if(cmList==null||cmList.isEmpty())getCModelFromDirectory();
		//FSP_FileReader reader=new FSP_FileReader(directory.getPath()+sep+"Controller"+sep+controller);//simulate用のController
		difftp = new DifferentialTransitionParser(cmList);
		firstCheck=true;
		return difftp.checkDesignTimeSynthesis();
	}

	public List<GameModel> getCModelFromDirectory()
	{
		File env;
		reqs=new ArrayList<Model[]>();
		if((env=searchEnvInDirectory())==null)
		{
			return null;
		}
		setControllableAction();
		FSP_FileReader reader;
		reader=new FSP_FileReader(env);
		e=reader.getModel();
		reqs=getReqs();
		targetError=setDegradationTargets();
		for(int i=0;i<reqs.size();i++)
		{//Game 5個作成
			cmList.add(ConcurrentSystemModelMaker.makeConcurrentSystem(e, reqs.get(i),cas));
		}
		return cmList;
	}

	public int checkUpdateControllerFromFile(String fileName)
	{
		//tp=new TransitionParser(cmList);
		BufferedReader reader=null;
		int nowlevel = -1;
		//long start=System.currentTimeMillis();
		try
		{
			reader=new BufferedReader(new FileReader(new File(directory.getPath()+sep+"Controller"+sep+fileName)));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return nowlevel;
		}
		//long stop=System.currentTimeMillis();
		//System.out.println("Spending time of BufferedReader: "+(stop-start)+"ms");
		//long start2=System.currentTimeMillis();
		try
		{
			String temp;
			while((temp=reader.readLine())!=null)
			{
				nowlevel = checkUpdatedController(temp);
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//long stop2=System.currentTimeMillis();
		//System.out.println("Spending time of checkUpdatedController: "+(stop2-start2)+"ms");
		return nowlevel;
	}

	public int checkUpdatedController(String updatedPart)
	{
		if(!modelUpdate(updatedPart))return -1;
		difftp.setCMList(cmList);
		return difftp.checkDifferentialControllerSynthesis();
		//return tp.checkUpdateControllerSynthesis();
	}

	private boolean modelUpdate(String updatedPart)
	{
		if(!firstCheck)
		{
			System.out.println("ERROR: First check have not been done yet");
			return false;
		}
		String[] materials=updatedPart.split(",");
		if(materials.length!=3)
		{
			System.out.println("ERROR:Input type is wong");
			return false;
		}
		for(int i=0;i<cmList.size();i++)
		{
			//System.out.println("level "+i+" model updating with "+materials[1]);
			cmList.set(i, ConcurrentSystemModelMaker.modelUpdate(cmList.get(i), e,  reqs.get(i), materials[0],materials[1], materials[2]));
		}
		return true;
	}

}
