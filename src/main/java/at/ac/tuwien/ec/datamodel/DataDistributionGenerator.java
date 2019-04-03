package at.ac.tuwien.ec.datamodel;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import at.ac.tuwien.ec.model.Hardware;
import at.ac.tuwien.ec.sleipnir.SimulationSetup;

public class DataDistributionGenerator implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1230409659206834914L;
	private ArrayList<DataEntry> generatedData;
	private int entryNum;
	
	private ExponentialDistribution miDistr,inData,outData,coreD;
	
	public DataDistributionGenerator(int entryNum)
	{
		generatedData = new ArrayList<DataEntry>();
		this.entryNum = entryNum;
		if(SimulationSetup.workloadType.startsWith("CPU"))
		{
			miDistr = new ExponentialDistribution(2e3); //cpu-bound
			inData = new ExponentialDistribution(1e3); //cpu-bound
			outData = new ExponentialDistribution(1e3); //cpu-bound
			coreD = new ExponentialDistribution(4); //cpu-bound
		}
		else 
		{
			miDistr = new ExponentialDistribution(2e2); //data-bound
			inData = new ExponentialDistribution(1e3); //data-bound
			outData = new ExponentialDistribution(1e3); //data-bound
			coreD = new ExponentialDistribution(2); //data-bound
		}
	}
	
	public ArrayList<DataEntry> getGeneratedData()
	{
		if(generatedData.isEmpty())
			generateData();
		return generatedData;
	}

	private void generateData() {
		double mi, inD, outD;
		int coreNum = 1;
		for(int i = 0; i < entryNum; i++) 
		{
			do
				if(SimulationSetup.workloadType.startsWith("DATA"))
					coreNum = (int) ((int) 1 + coreD.sample()); // data-bound
				else
					coreNum = (int) ((int) 4 + coreD.sample()); //cpu-bound
			while(coreNum > 16 || coreNum < 1);
			if(SimulationSetup.workloadType.equals("CPU0"))
			{
				mi = 5e3 + miDistr.sample(); // cpu-bound
				inD = 1e3 + inData.sample(); //cpu-bound
				outD = 1e3 + outData.sample(); //cpu-bound
			}
			else if(SimulationSetup.workloadType.equals("CPU1"))
			{
				mi = 1e4 + miDistr.sample(); // cpu-bound
				inD = 1e3 + inData.sample(); //cpu-bound
				outD = 1e3 + outData.sample(); //cpu-bound
			}
			else if(SimulationSetup.workloadType.equals("CPU2"))
			{
				mi = 5e4 + miDistr.sample(); // cpu-bound
				inD = 1e3 + inData.sample(); //cpu-bound
				outD = 1e3 + outData.sample(); //cpu-bound
			}
			else if(SimulationSetup.workloadType.equals("CPU3"))
			{
				mi = 1e5 + miDistr.sample(); // cpu-bound
				inD = 1e3 + inData.sample(); //cpu-bound
				outD = 1e3 + outData.sample(); //cpu-bound
			}
			else if(SimulationSetup.workloadType.equals("DATA0"))
			{
				mi = 1e3 + miDistr.sample(); // data-bound
				inD = 5e3 + inData.sample(); //data-bound
				outD = 5e3 + outData.sample(); //data-bound
			}
			else if(SimulationSetup.workloadType.equals("DATA1"))
			{
				mi = 1e3 + miDistr.sample(); // data-bound
				inD = 1e4 + inData.sample(); //data-bound
				outD = 1e4 + outData.sample(); //data-bound
			}
			else if(SimulationSetup.workloadType.equals("DATA2"))
			{
				mi = 1e3 + miDistr.sample(); // data-bound
				inD = 5e4 + inData.sample(); //data-bound
				outD = 5e4 + outData.sample(); //data-bound
			}
			else //(SimulationSetup.workloadType.equals("DATA3"))
			{
				mi = 1e3 + miDistr.sample(); // data-bound
				inD = 1e5 + inData.sample(); //data-bound
				outD = 1e5 + outData.sample(); //data-bound
			}
			generatedData.add(
					new DataEntry("entry"+i,
							new Hardware(coreNum, 1, inD + outD),
							mi,
							"iot"+(i%SimulationSetup.iotDevicesNum),
							inD,
							outD,
							"iot"+(i%SimulationSetup.iotDevicesNum))
					);
		}
	}
	
	
}
