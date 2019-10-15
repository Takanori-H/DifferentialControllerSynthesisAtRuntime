package multitierControllerModel.GameSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import multitierControllerModel.Model.Model;
import multitierControllerModel.Model.State;
import multitierControllerModel.Model.Transition;

public class DiscreteTransitionParser
{
	Model con;
	List<State> WRETmp;
	List<State> WRCTmp;
	HashMap<String,State> C;
	int count;
	int nowl;
	int flag;
	GameModel nowGame;

	public DiscreteTransitionParser(GameModel Game, int level)
	{
		//now = level;
		//GameList.put(level, Game);
		nowl = level;
		nowGame = Game;
		WRETmp = new ArrayList<State>();
		WRCTmp = new ArrayList<State>();
		C = new HashMap<String,State>();
	}

	public void setCon(Model c)
	{
		this.con = c;
	}

	int DCSUEnv()
	{
		long start2 = System.currentTimeMillis();
		IdentifyUWR(nowGame, nowl);
		if(WRCTmp.size()==0)
		{
			long stop=System.currentTimeMillis();
			System.out.println("Spending time of IdentifyWR: "+(stop-start2)+"ms");
			System.out.println("Synthesis of Controller Level: "+nowl);
			System.out.println("No Controller");
			System.out.println();
			return -1;
		}
		long start = System.currentTimeMillis();
		generateUController(WRCTmp, nowl);
		long stop = System.currentTimeMillis();
		System.out.println("Spending time of IdentifyWR+Synthesis: "+(stop-start2)+"ms");
		System.out.println("Spending time of Synthesis: "+(stop-start)+"ms");
		System.out.println("Synthesis of Controller Level: "+nowl);
		if(checkContSimulate(C, nowl))
		{
//			/*
			System.out.println("Simulate");
			System.out.println();
			System.out.println("level " + nowl + "," + "State数 " + C.size() + "," + "Transition数 " + CountTransitionUNum());
			System.out.println();
//			*/
			return 1;
		}
		else
		{
//			/*
			System.out.println("No Simulate");
			System.out.println();
			return 0;
//			*/
		}
	}

	void IdentifyUWR(GameModel model, int level)
	{
		WRETmp = new ArrayList<State>();
		WRCTmp = new ArrayList<State>();
		count = 0;
		State er = model.getErrorState();
		er.setIsDead();
		count++;
		WRETmp.add(er);
		for(int i=0;i<er.getFromTransitionNum();i++)
		{
			pasteDToTransition(er.getFromTransition(i));
		}
//		/*
		System.out.println("Level: " + level);
		System.out.println("GameState: " + model.getSize());
		System.out.println("WRETmp: " + WRETmp.size());
		System.out.println("count: " + count);
//		*/
		/*State initial = model.getInitialState();
		for(int i=0;i<model.getSize();i++) {
			State tmp = model.getState(i);
			if(tmp==initial && WRCTmp.size()>0) {
				State change = WRCTmp.get(0);
				if(!WRETmp.contains(tmp))WRCTmp.set(0, tmp);
				WRCTmp.add(change);
			}else if(!WRETmp.contains(tmp))WRCTmp.add(tmp);
		}*/
		State initial = model.getInitialState();
		WRCTmp.add(initial);
		for(int i=0;i<initial.getToTransitionNum();i++)
		{
			countWRC(initial.getToTransition(i).getTo());
		}
//		/*
		System.out.println("WRCTmp: " + WRCTmp.size());
		System.out.println();
//		*/
	}

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
				WRETmp.add(m);
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
			WRETmp.add(m);
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

	void countWRC(State tmp)
	{
		if(tmp.isDead() || WRCTmp.contains(tmp))return;
		WRCTmp.add(tmp);
		for(int i=0;i<tmp.getToTransitionNum();i++)
		{
			countWRC(tmp.getToTransition(i).getTo());
		}
	}

	void generateUController(List<State> WRClevel, int level)
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
	}

	boolean checkContSimulate(HashMap<String,State> controller, int level)
	{
		flag = 0;
		State tmpC, tmpC2;
		tmpC=con.getInitialState();
		tmpC2=controller.get(WRCTmp.get(0).getName());
		pasteContSimulate(tmpC, tmpC2, level);
		if(flag==0)
		{
			flag=0;
			return true;
		}
		else
		{
			flag=0;
			return false;
		}
	}

	void pasteContSimulate(State tmpC, State tmpC2, int level)
	{
		tmpC2.setSimulate(level);
		for(int i=0;i<tmpC.getToTransitionNum();i++)
		{
			checkContTransition(tmpC.getToTransition(i), tmpC2, level);
			if(flag==1)return;
		}
	}

	void checkContTransition(Transition ctr, State tmpC2, int level)
	{
		if(flag==1)return;
		if(ctr.isDead())return;
		String ctrName = ctr.getName();
		try
		{
			if(tmpC2.getToTransition(ctrName)!=null)
			{
				Transition ctr2 = tmpC2.getToTransition(ctrName);
				checkContState(ctr.getTo(),ctr2.getTo(), level);
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
			return;
		}
	}

	void checkContState(State tmpC, State tmpC2, int level)
	{
		if(flag==1)return;
		if(tmpC2.isSimulate(level)) return;
		tmpC2.setSimulate(level);//simulate関係をセット
		//System.out.println(jState.getName());
		//System.out.println(iState.getName());
		for(int i=0;i<tmpC.getToTransitionNum();i++)
		{
			checkContTransition(tmpC.getToTransition(i), tmpC2, level);
			if(flag==1)return;
		}
	}

	public int CountTransitionUNum()
	{
		int count=0;
		for(int i=0;i<C.size();i++)
		{
			State cState = C.get(WRCTmp.get(i).getName());
			count += cState.getToTransitionNum();
		}
		return count;
	}

	public int checkDCSUEnv(Model controller)
	{
		this.setCon(controller);
		return checkDCSUEnv();
	}

	public int checkDCSUEnv()
	{
		return DCSUEnv();
	}

}
