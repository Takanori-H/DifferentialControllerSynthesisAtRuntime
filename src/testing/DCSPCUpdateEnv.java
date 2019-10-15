package testing;

import org.junit.Test;

public class DCSPCUpdateEnv {
	DirectoryTrackerForSingleWinningRegion dt;

	@Test
	public void test()
	{
		String[] cont=
		{
				"case0_controller.txt",
				"case0_controller.txt",
				"case0_controller.txt",
				"case0_controller.txt",
				"case4_controller.txt",
				"case0_controller.txt",
				"case6_controller.txt",
				"case4_controller.txt"/*,
				"Controller8.txt",
				"Controller10.txt",
				"Controller10.txt",
				"Controller10.txt",
				"Controller12.txt",
				"Controller11.txt",
				"Controller14.txt"*/

		};
		doJointTest(cont[0]);
	}

	void doJointTest(String controller)
	{
		dt = new DirectoryTrackerForSingleWinningRegion("ProductionCell");
		long start=System.currentTimeMillis();
		dt.checkDCSUEnv(controller, 0, 16);
		//dt.checkDCSUPCEnv(16);
		long stop=System.currentTimeMillis();
		System.out.print("Spending time: "+(stop-start)+"ms");
		return;
	}
}
