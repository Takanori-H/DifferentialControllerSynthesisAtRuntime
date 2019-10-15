package multitierControllerModel.GameSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import multitierControllerModel.Model.State;
import multitierControllerModel.Model.Transition;

public class DifferentialTransitionParser {
	int currentIndex;//現在のレベル
	int MAX_LEVEL;
	int max;//最高レベル
	int now;//今動いているコントローラのレベル
	int count;
	int flag;
	List<GameModel> modelList;
	List<String> transitionRecordsOfModel;//GameのTransitionの記録
	List<State> WRCTmp;
	List<State> WRCnow;
	List<State> deltaWRC;
	List<Integer> nolevel;
	HashMap<Integer,GameModel> GameList;//レベルとゲームモデルを管理
	HashMap<Integer,List<State>> WRC;//コントローラ側のWinning Region
	HashMap<String,State> C;
	HashMap<Integer,HashMap<String,State>> Controllers;

	public DifferentialTransitionParser(List<GameModel> modelList)
	{
		this.modelList = modelList;
		this.transitionRecordsOfModel = new ArrayList<String>();
		MAX_LEVEL = modelList.size();
		currentIndex = 0;
		//new
		now = max = modelList.size();
		GameList = new HashMap<Integer,GameModel>();
		for(int i=0;i<modelList.size();i++)
		{
			GameList.put(modelList.size()-i,modelList.get(i));
		}
		WRCTmp = new ArrayList<State>();
		WRCnow = new ArrayList<State>();
		WRC = new HashMap<Integer,List<State>>();
		C = new HashMap<String,State>();
		Controllers = new HashMap<Integer,HashMap<String,State>>();
		nolevel = new ArrayList<Integer>();
	}

	public int[][] checkDesignTimeSynthesis()
	{
		int level = max;
		int Csize[][] = new int[modelList.size()][2];
		HashMap<String,State> tmpC = new HashMap<String, State>();
		DesignTimeSynthesis();
		while(level>0)
		{
			//GameModel model = GameList.get(level);
			tmpC = Controllers.get(level);
			Csize[level-1][0] = tmpC.size();
			Csize[level-1][1] = CountTransitionNum(level, tmpC);
			level--;
		}
		return Csize;
	}

	void DesignTimeSynthesis()
	{
		int level = max;
		List<State> WRClevel;
		while(level>0)
		{
			GameModel model = GameList.get(level);
			WRClevel = IdentifyWR(model, level);
			generateController(WRClevel, level);
			level--;
		}
		createSimulate(max);
	}

//	/*
	List<State> IdentifyWR(GameModel model, int level)
	{
		WRCTmp = new ArrayList<State>();
		count = 0;
		State er = model.getErrorState();
		er.setIsDead();
		count++;
		for(int i=0;i<er.getFromTransitionNum();i++)
		{
			pasteDToTransition(er.getFromTransition(i));
		}
		System.out.println("Level: " + level);
		System.out.println("GameState: " + model.getSize());
		System.out.println("count: " + count);
		State initial = model.getInitialState();
		WRCTmp.add(initial);
		for(int i=0;i<initial.getToTransitionNum();i++)
		{
			countWRC(initial.getToTransition(i).getTo());
		}
		System.out.println("WRCTmp: " + WRCTmp.size());
		System.out.println();
		WRC.put(level, WRCTmp);
		return WRCTmp;
	}
//	*/

	void pasteDToTransition(Transition m)
	{
		if (m.isDead())
			return;
		// System.out.println("This is dead transition:["+m+"]");//debug
		m.setIsDead();
		this.pasteDToState(m.getFrom());
	}

	void pasteDToState(State m)
	{//バックワードプロパゲーション
		if (m.isDead())
			return;
		boolean dead = true;
		//System.out.println("state "+m);
		for (int i = 0; i < m.getToTransitionNum(); i++)
		{
			Transition t = m.getToTransition(i);
			//System.out.println("  Transition:"+t+" isControllable:"+t.isControllable()+" isDead:"+t.isDead());
			if (!t.isControllable() && t.isDead())
			{//UnControllable Actionで環境側のWinning Regionに繋がるならば
				m.setIsDead();//そのStateはDead
				//WRETmp.add(m);
				count++;
				//System.out.println("Dead state:["+m+"]");//debug
				for (int j = 0; j < m.getFromTransitionNum(); j++)
				{
					pasteDToTransition(m.getFromTransition(j));
				}
				return;
			}
			else if (!t.isDead())
			{//環境側のWinning Regionにつながっていないならば
				dead = false;//環境側のWRにつながっているUnControllableActionがなければok
			}
		}

		if (dead)
		{
			m.setIsDead();
			//WRETmp.add(m);
			count++;
		}
		//System.out.println(" isDead:"+m.isDead());
		if (m.isDead())
		{
			for (int i = 0; i < m.getFromTransitionNum(); i++)
			{
				pasteDToTransition(m.getFromTransition(i));
			}
		}
	}

//	/*
	void countWRC(State tmp)
	{
		if(tmp.isDead() || WRCTmp.contains(tmp))return;
		WRCTmp.add(tmp);
		for(int i=0;i<tmp.getToTransitionNum();i++)
		{
			countWRC(tmp.getToTransition(i).getTo());
		}
	}
//	*/

	void generateController(List<State> WRClevel, int level)
	{
		State tmpState, cState, fromState, toState, fromCState, toCState;
		Transition tmpTransition, cTransition;
		C = new HashMap<String,State>();
		for(int i=0;i<WRClevel.size();i++)
		{//状態だけ用意
			tmpState = WRClevel.get(i);
			cState = new State(tmpState.getName());
			C.put(cState.getName(), cState);
		}
		for(int i=0;i<WRClevel.size();i++)
		{
			tmpState = WRClevel.get(i);
			cState = C.get(tmpState.getName());
			for(int j=0;j<tmpState.getFromTransitionNum();j++)
			{
				tmpTransition = tmpState.getFromTransition(j);
				if(!tmpTransition.isDead())
				{
					fromState = tmpTransition.getFrom();//Winning Region上でのTransitionのfromState
					fromCState = C.get(fromState.getName());//コントローラ上でのTransitionのfromState
					toCState = cState;//cStateのfromTransitionのtoStateはcState
					cTransition = new Transition(tmpTransition.getName(), fromCState, toCState);
					if(tmpTransition.isControllable())cTransition.setIsControllable();
					cState.addFromTransition(cTransition);
				}
			}
			for(int j=0;j<tmpState.getToTransitionNum();j++)
			{
				tmpTransition = tmpState.getToTransition(j);
				if(!tmpTransition.isDead())
				{
					//fromState = tmpTransition.getFrom();//Winning Region上でのTransitionのfromState
					//fromCState = C.get(fromState.getName());//コントローラ上でのTransitionのfromState
					fromCState = cState;//cStateのtoTransitionのfromStateはcState
					toState = tmpTransition.getTo();//Winning Region上でのTransitionのtoState
					toCState = C.get(toState.getName());//コントローラ上でのTransitionのtoState
					cTransition = new Transition(tmpTransition.getName(), fromCState, toCState);
					if(tmpTransition.isControllable())cTransition.setIsControllable();
					cState.addToTransition(cTransition);
				}
			}
		}
		Controllers.put(level,C);
	}

	//コントローラ側のWinning Regionのsimulation関係を取る 設計時
	void createSimulate(int level)
	{
		flag=0;
		//for(int i=1;i<max;i++) {
		for(int i=1;i<=level;i++)
		{
			//GameModel imodel = GameList.get(i);
			List<State> iWRC = WRC.get(i);
			//for(int j=i+1;j<=max;j++) {
			for(int j=i;j<=level;j++)
			{
				//GameModel jmodel = GameList.get(j);
				List<State> jWRC = WRC.get(j);
				pasteSimulate(iWRC, jWRC, j);
				if(flag==0)System.out.println(i + " is simulate " + j);
				if(flag==1)System.out.println(i + " is not simulate " + j);
				flag=0;
			}
		}
	}

	void pasteSimulate(List<State> iWRC, List<State> jWRC, int level)
	{
		State jInitialState = jWRC.get(0);
		State iInitialState = iWRC.get(0);
		iInitialState.setSimulate(level);
		//System.out.println(jInitialState.getName());
		//System.out.println(iInitialState.getName());
		for(int i=0;i<jInitialState.getToTransitionNum();i++)
		{
			pasteSFromTransition(jInitialState.getToTransition(i), iInitialState, level, iWRC);
			if(flag==1)return;
		}
	}

	void pasteSFromTransition(Transition jtr, State iState, int level, List<State> iWRC)
	{
		if(flag==1)return;
		if(jtr.isDead()) return;
		String jName = jtr.getName();
		try
		{
			if(iState.getToTransition(jName)!=null)
			{
				Transition itr = iState.getToTransition(jName);
				//System.out.println(jName);//debug
				//System.out.println(itr.toString());
				//System.out.println();
				pasteSFromState(jtr.getTo(),itr.getTo(), level, iWRC);
				if(flag==1)return;
			}
			else
			{
				throw new NullPointerException();
			}
		}
		catch(NullPointerException e)
		{
			flag=1;
			pasteSNull(iWRC, level);
			return;
		}
		//if(itr==null)System.out.println("true");
		//if(itr==null)return;
	}

	void pasteSFromState(State jState, State iState, int level, List<State> iWRC)
	{
		if(flag==1)return;
		if(iState.isSimulate(level)) return;
		iState.setSimulate(level);//simulate関係をセット
		//System.out.println(jState.getName());
		//System.out.println(iState.getName());
		for(int i=0;i<jState.getToTransitionNum();i++)
		{
			pasteSFromTransition(jState.getToTransition(i), iState, level, iWRC);
			if(flag==1)return;
		}
	}

	void pasteSNull(List<State> iWRC, int level)
	{
		for(int i=0;i<iWRC.size();i++)
		{
			if(iWRC.get(i).isSimulate(level))iWRC.get(i).removeSimulate(level);
			//System.out.println(level);
			/*if(iWRC.get(i).isSimulate(level)) {
				System.out.println("true");
			}else {
				System.out.println("false");
			}*/
		}
	}

	public int CountTransitionNum(int level, HashMap<String,State> tmpC)
	{
		List<State> WR = WRC.get(level);
		int count = 0;
		for(int i=0;i<tmpC.size();i++)
		{
			State cState = tmpC.get(WR.get(i).getName());
			count += cState.getToTransitionNum();
		}
		return count;
	}

	public void setCMList(List<GameModel> modelList)
	{
		this.modelList = modelList;
		for(int i=0;i<modelList.size();i++)
		{
			GameList.put(modelList.size()-i,modelList.get(i));
		}
	}

	public int checkDifferentialControllerSynthesis()
	{
		HashMap<String,State> tmpC = new HashMap<String,State>();
		//DesignTimeSynthesis();
		long start2=System.currentTimeMillis();
		DifferenceSynthesis();
		long stop2=System.currentTimeMillis();
		System.out.println("Spending time of DifferenceSynthesis: "+(stop2-start2)+"ms");
		System.out.println("nowlevel " + now);
		//long start=System.currentTimeMillis();
//		/*
		int level = now;
		while(level>0)
		{
			if(Controllers.containsKey(level))
			{
			tmpC = Controllers.get(level);
			if(tmpC==null)System.out.println("null");
			System.out.println("level " + level + "," + "State数 " + tmpC.size());
			//System.out.println("level " + level + "," + "State数 " + tmpC.size() + "," + "Transition数 " + CountTransitionNum(level, tmpC));
			System.out.println();
			}
			level--;
		}
//		*/
		//long stop=System.currentTimeMillis();
		//System.out.println("Spending time of display: "+(stop-start)+"ms");
		//return Csize;
		return now;
	}

	void DifferenceSynthesis()
	{
		int level = now;//緩和だけならnowからで良い 全部やる必要があるのか？
		int nowClevel = now;
		List<State> deltaWRClevel = new ArrayList<State>();
		//HashMap<String,State> tmpC = new HashMap<String,State>();
		while(level>0)
		{
			System.out.println(level);
			if(nolevel.contains(level))
			{
				level--;
				continue;
			}
			long start2 = System.currentTimeMillis();
			GameModel model = GameList.get(level);
			deltaWRClevel = IdentifyUpdateWR(model, level);//updateGameとidentifyWR
			long start=System.currentTimeMillis();
			UpdateController(model, level);
			if(!checkSynthesis(nowClevel, deltaWRClevel))
			{//今動いているコントローラとsimulateしているかどうか
				//System.out.println("false");
				long stop=System.currentTimeMillis();
				System.out.println("Spending time of IdentifyWR+Synthesis:  "+(stop-start2)+"ms");
				System.out.println("Spending time of Synthesis:  "+(stop-start)+"ms");
				if(WRCTmp.size()==0)
				{
					System.out.println("No Controller");
				}
				else
				{
					System.out.println("No Simulate");
				}
				System.out.println();
				Controllers.remove(level);//simulateしていなかったらコントローラ取り除く
				nolevel.add(level);//コントローラを取り除いたレベルを管理
				level--;
				//now = level;
				continue;
			}
			else if(level==nowClevel)
			{//deltaWRE==0の場合
				long stop=System.currentTimeMillis();
				System.out.println("Spending time of IdentifyWR+Synthesis: "+(stop-start2)+"ms");
				System.out.println("Spending time of Synthesis: "+(stop-start)+"ms");
				System.out.println("Synthesis of Controller Level: "+level);
				System.out.println();
				level--;
				continue;
			}
			//createUpdateSimulate(level);
			generateUpdateController(deltaWRClevel, level);
			//if(Controllers.containsKey(level))System.out.println("true");
			long stop=System.currentTimeMillis();
			System.out.println("Spending time of IdentifyWR+Synthesis: "+(stop-start2)+"ms");
			System.out.println("Spending time of Synthesis: "+(stop-start)+"ms");
			System.out.println("Synthesis of Controller Level: "+level);
			System.out.println();
			level--;
		}
		for(int i=max;i>0;i--)
		{
			if(Controllers.containsKey(i))
			{
				now = i;
				break;
			}
		}
		long start3=System.currentTimeMillis();
		createUpdateSimulate(now);
		long stop3=System.currentTimeMillis();
		System.out.println("Spending time of Simulate: "+(stop3-start3)+"ms");
	}

//	/*
	List<State> IdentifyUpdateWR(GameModel model, int level)
	{//Updateがちゃんとできていない可能性あり
		//WRETmp = new ArrayList<State>();
		WRCTmp = new ArrayList<State>();
		//List<State> oldWRC = new ArrayList<State>();
		deltaWRC = new ArrayList<State>();
		count=0;
		//WRETmp = WRE.get(level);
		//oldWRC = WRC.get(level);
		WRCTmp = WRC.get(level);
		List<State> delta = new ArrayList<State>();
		List<Transition> l = model.getUpdatedPart();//updateはTransitionのみを考えている？
		for(int i=0;i<l.size();i++)
		{
			if(l.get(i).getTo().isDead())
			{
				pasteUpdateDToTransition(l.get(i));
			}
		}
		//WRE.put(level, WRETmp);
		for(int i=0;i<deltaWRC.size();i++)
		{
			State tmp = deltaWRC.get(i);
			if(!WRCTmp.contains(tmp))
			{
				delta.add(tmp);
			}
		}
		deltaWRC.removeAll(delta);
		WRCTmp.removeAll(deltaWRC);
		System.out.println("GameState: "+ model.getSize());
		//System.out.println("WRETmp: " + WRETmp.size());
		//System.out.println("deltaWRE: " + deltaWRE.size());
		System.out.println("count :" + count);
		System.out.println("WRCTmp: " + WRCTmp.size());
		System.out.println("deltaWRC: " + deltaWRC.size());
		WRC.put(level, WRCTmp);
		//UpdateController(l,level);
		return deltaWRC;
		//return WRCTmp;
	}
//	*/

	void pasteUpdateDToTransition(Transition m)
	{
		if (m.isDead())
			return;
		// System.out.println("This is dead transition:["+m+"]");//debug
		m.setIsDead();
		this.pasteUpdateDToState(m.getFrom());
	}

//	/*
	void pasteUpdateDToState(State m)
	{//バックワードプロパゲーション
		if (m.isDead())
			return;
		boolean dead = true;
		//System.out.println("state "+m);
		for (int i = 0; i < m.getToTransitionNum(); i++)
		{
			Transition t = m.getToTransition(i);
			//System.out.println("  Transition:"+t+" isControllable:"+t.isControllable()+" isDead:"+t.isDead());
			if (!t.isControllable() && t.isDead())
			{//UnControllable Actionで環境側のWinning Regionに繋がるならば
				m.setIsDead();//そのStateはDead
				//System.out.println(m.getName());
//				WRETmp.add(m);
//				deltaWRE.add(m);
				deltaWRC.add(m);
				count++;
				//System.out.println("Dead state:["+m+"]");//debug
				for (int j = 0; j < m.getFromTransitionNum(); j++)
				{
					pasteUpdateDToTransition(m.getFromTransition(j));
				}
				return;
			}
			else if (!t.isDead())
			{//環境側のWinning Regionにつながっていないならば
				dead = false;//環境側のWRにつながっているUnControllableActionがなければok
			}
		}

		if (dead)
		{
			m.setIsDead();
			count++;
			deltaWRC.add(m);
//			WRETmp.add(m);
//			deltaWRE.add(m);
		}
		//System.out.println(" isDead:"+m.isDead());
		if (m.isDead())
		{
			for (int i = 0; i < m.getFromTransitionNum(); i++)
			{
				pasteUpdateDToTransition(m.getFromTransition(i));
			}
		}
	}
//	*/

	void UpdateController(/*List<Transition> l,*/GameModel model, int level)
	{//コントローラ内にtransitionが増えていた場合にコントローラをアップデート
		State toState, fromState, toCState, fromCState;
		Transition newTransition;
		C = Controllers.get(level);
		if(C==null)System.out.println("true");
		List<Transition> l = model.getUpdatedPart();
		for(int i=0;i<l.size();i++)
		{
			Transition tr = l.get(i);
			if(!tr.isDead())
			{
				toState = tr.getTo();
				fromState = tr.getFrom();
				if(toState.getName()==null)System.out.println("true");
				toCState = C.get(toState.getName());
				fromCState = C.get(fromState.getName());
//				/*
				if(toCState==null && fromCState == null)
				{
					if(toState.isDead())continue;
					if(fromState.isDead())continue;
					toCState=new State(toState.getName());
					fromCState=new State(fromState.getName());
					newTransition = new Transition(tr.getName(), fromCState, toCState);
					if(tr.isControllable())newTransition.setIsControllable();
					toCState.addFromTransition(newTransition);
					fromCState.addToTransition(newTransition);
					C.put(toCState.getName(),toCState);
					C.put(fromCState.getName(), fromCState);
				}
				else if(toCState==null)
				{
					if(toState.isDead())continue;
					toCState=new State(toState.getName());
					newTransition = new Transition(tr.getName(), fromCState, toCState);
					if(tr.isControllable())newTransition.setIsControllable();
					toCState.addFromTransition(newTransition);
					fromCState.addToTransition(newTransition);
					C.put(toCState.getName(), toCState);
				}
				else if(fromCState==null)
				{
					if(fromState.isDead())continue;
					fromCState=new State(fromState.getName());
					newTransition = new Transition(tr.getName(), fromCState, toCState);
					if(tr.isControllable())newTransition.setIsControllable();
					toCState.addFromTransition(newTransition);
					fromCState.addToTransition(newTransition);
					C.put(fromCState.getName(), fromCState);
				}
				else
				{
					newTransition = new Transition(tr.getName(), fromCState, toCState);
					if(tr.isControllable())newTransition.setIsControllable();
					toCState.addFromTransition(newTransition);
					fromCState.addToTransition(newTransition);
				}
//				*/
			}
		}
		Controllers.put(level, C);
	}

	boolean checkSynthesis(int nowClevel, List<State> deltaWRElevel)
	{
		for(int i=0;i<deltaWRElevel.size();i++)
		{
			State tmp = deltaWRElevel.get(i);
			//if(tmp.isSimulate(nowClevel))System.out.println("false");
			if(tmp.isSimulate(nowClevel))return false;
		}
		return true;
	}

	void generateUpdateController(List<State> deltaWRElevel, int level)
	{//差分更新
		State tmpState, cState, from, to;
		Transition cTransition;
		String cTransitionName;
		C = Controllers.get(level);
		for(int i=0;i<deltaWRElevel.size();i++)
		{
			tmpState = deltaWRElevel.get(i);
			cState = C.get(tmpState.getName());
			for(int j=0;j<cState.getFromTransitionNum();j++)
			{
				cTransition = cState.getFromTransition(j);
				cTransitionName = cTransition.getName();
				from = cTransition.getFrom();
				from.eraseToTransition(cTransitionName);
				from.eraseToTransitionNameList(cTransitionName);
			}
			for(int j=0;j<cState.getToTransitionNum();j++)
			{
				cTransition = cState.getToTransition(j);
				cTransitionName=cTransition.getName();
				to = cTransition.getTo();
				to.eraseFromTransition(cTransition);
				//to.erasefromTransitionNameList(cTransitionName);
			}
			C.remove(cState.getName());
		}
		Controllers.put(level, C);
	}

	void createUpdateSimulate(int level)
	{
		flag=0;
		//for(int i=1;i<max;i++) {
		for(int i=1;i<=level;i++)
		{
			//GameModel imodel = GameList.get(i);
			List<State> iWRC = WRC.get(i);
			for(int j=0;j<iWRC.size();j++)
			{
				iWRC.get(i).clearSimulate();
			}
			if(!Controllers.containsKey(i))
			{
				continue;
			}
			for(int j=i;j<=level;j++)
			{
				//GameModel jmodel = GameList.get(j);
				if(!Controllers.containsKey(j))
				{
					//System.out.println(i + " is not simulate " + j);
					continue;
				}
				List<State> jWRC = WRC.get(j);
				pasteSimulate(iWRC, jWRC, j);
				if(flag==0)System.out.println(i + " is simulate " + j);
				if(flag==1)System.out.println(i + " is not simulate " + j);
				flag=0;
			}
		}
	}

}
