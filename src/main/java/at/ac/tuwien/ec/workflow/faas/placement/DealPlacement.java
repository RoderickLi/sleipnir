package at.ac.tuwien.ec.workflow.faas.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.alg.shortestpath.JohnsonShortestPaths;
import org.jgrapht.traverse.TopologicalOrderIterator;

import at.ac.tuwien.ec.model.infrastructure.MobileDataDistributionInfrastructure;
import at.ac.tuwien.ec.model.infrastructure.computationalnodes.ComputationalNode;
import at.ac.tuwien.ec.model.infrastructure.computationalnodes.IoTDevice;
import at.ac.tuwien.ec.model.infrastructure.computationalnodes.MobileDevice;
import at.ac.tuwien.ec.model.infrastructure.computationalnodes.NetworkedNode;
import at.ac.tuwien.ec.model.infrastructure.network.ConnectionMap;
import at.ac.tuwien.ec.model.infrastructure.network.NetworkConnection;
import at.ac.tuwien.ec.model.software.ComponentLink;
import at.ac.tuwien.ec.model.software.MobileSoftwareComponent;
import at.ac.tuwien.ec.provisioning.MobilityBasedNetworkPlanner;
import at.ac.tuwien.ec.provisioning.mobile.MobileDevicePlannerWithMobility;
import at.ac.tuwien.ec.scheduling.Scheduling;
import at.ac.tuwien.ec.scheduling.offloading.OffloadScheduling;
import at.ac.tuwien.ec.sleipnir.SimulationSetup;
import at.ac.tuwien.ec.workflow.faas.FaaSWorkflow;
import at.ac.tuwien.ec.workflow.faas.FaaSWorkflowPlacement;
import scala.Tuple2;

public class DealPlacement extends FaaSPlacementAlgorithm {
	
	private ArrayList<IoTDevice> publisherDevices;
	private ArrayList<MobileDevice> subscriberDevices;
	private ArrayList<ComputationalNode> candidateCenters;
	TopologicalOrderIterator<MobileSoftwareComponent, ComponentLink> workflowIterator;
		
	public DealPlacement(Tuple2<FaaSWorkflow,MobileDataDistributionInfrastructure> arg)
	{
		super();
		setCurrentWorkflow(arg._1());
		setInfrastructure(arg._2());
		for(ComputationalNode c : getInfrastructure().getAllNodes()) 
		{
			if(c.getMaxDistance() < 0)
			{
				double maxDistance = Double.MIN_VALUE;
				for(IoTDevice i : getInfrastructure().getIotDevices().values())
				{
					double dist = getInfrastructure().getConnectionMap().computeDistance(c,i);
					if(dist > maxDistance)
						maxDistance = dist;
				}
				c.setMaxDistance(maxDistance);
			}
		}
		MobileDataDistributionInfrastructure currInf = this.getInfrastructure();
		ArrayList<String> activeTopics = new ArrayList<String>();
		for(String topic : currInf.getRegistry().keySet())
		{
			if(!currInf.getRegistry().get(topic).isEmpty())
				activeTopics.add(topic);
		}
		String[] sourceTopics = activeTopics.toArray(new String[0]);
		
		publisherDevices = new ArrayList<IoTDevice>();
		subscriberDevices = new ArrayList<MobileDevice>();
		
		Set<String> srcTopicSet = new HashSet<String>(Arrays.asList(sourceTopics));
		this.workflowIterator = new TopologicalOrderIterator<MobileSoftwareComponent,ComponentLink>(getCurrentWorkflow().getTaskDependencies());
		
		for(IoTDevice iot : currInf.getIotDevices().values())
		{
			Set<String> iotTopics = new HashSet<String>(Arrays.asList(iot.getTopics()));
			iotTopics.retainAll(srcTopicSet);
			if(!iotTopics.isEmpty())
				publisherDevices.add(iot);
		}
		
		for(String t : activeTopics)
		{
			ArrayList<MobileDevice> subscribers = currInf.getSubscribedDevices(t);
			if(subscribers != null)
				subscriberDevices.addAll(subscribers);
		}
		
		//ConnectionMap infrastructureMap = (ConnectionMap) getInfrastructure().getConnectionMap().clone();
		ConnectionMap infrastructureMap = (ConnectionMap) getInfrastructure().getConnectionMap();
		//infrastructureMap = extractSubgraph(infrastructureMap,publisherDevices,subscriberDevices);
		candidateCenters = findCenters(infrastructureMap, SimulationSetup.nCenters);
	}
	
	
	class MaxDistanceComparator implements Comparator<ComputationalNode>
	{

		@Override
		public int compare(ComputationalNode o1, ComputationalNode o2) {
			// TODO Auto-generated method stub
			return Double.compare(o1.getMaxDistance(), o2.getMaxDistance());
		}
	}

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1302954506550171766L;

		
	@Override
	public ArrayList<? extends Scheduling> findScheduling() {
		double startTime = System.currentTimeMillis();
		int currentTimestamp = 0;
		ArrayList<FaaSWorkflowPlacement> schedulings = new ArrayList<FaaSWorkflowPlacement>();
		
		FaaSWorkflowPlacement scheduling = new FaaSWorkflowPlacement(this.getCurrentWorkflow(),this.getInfrastructure());
		
		this.workflowIterator = new TopologicalOrderIterator<MobileSoftwareComponent,ComponentLink>(getCurrentWorkflow().getTaskDependencies());
				
		while(workflowIterator.hasNext())
		{
			//Extract subgraph
			if(updateCondition()) 
			{
				MobileDataDistributionInfrastructure currInf = this.getInfrastructure();
				ArrayList<String> activeTopics = new ArrayList<String>();
				for(String topic : currInf.getRegistry().keySet())
				{
					if(!currInf.getRegistry().get(topic).isEmpty())
						activeTopics.add(topic);
				}
				String[] sourceTopics = activeTopics.toArray(new String[0]);
				
				publisherDevices = new ArrayList<IoTDevice>();
				subscriberDevices = new ArrayList<MobileDevice>();
				
				Set<String> srcTopicSet = new HashSet<String>(Arrays.asList(sourceTopics));
				
				
				for(IoTDevice iot : currInf.getIotDevices().values())
				{
					Set<String> iotTopics = new HashSet<String>(Arrays.asList(iot.getTopics()));
					iotTopics.retainAll(srcTopicSet);
					if(!iotTopics.isEmpty())
						publisherDevices.add(iot);
					
				}
				
				for(String t : activeTopics)
				{
					ArrayList<MobileDevice> subscribers = currInf.getSubscribedDevices(t);
					if(subscribers != null)
						subscriberDevices.addAll(subscribers);
				}
				
				//ConnectionMap infrastructureMap = (ConnectionMap) getInfrastructure().getConnectionMap().clone();
				ConnectionMap infrastructureMap = (ConnectionMap) getInfrastructure().getConnectionMap().clone();
				//infrastructureMap = extractSubgraph(infrastructureMap,publisherDevices,subscriberDevices);
				candidateCenters = findCenters(infrastructureMap, SimulationSetup.nCenters);
			}
			//all of this before should be moved in the constructor
			MobileSoftwareComponent msc = workflowIterator.next();
			double minAvgCost = Double.MAX_VALUE;
			ComputationalNode trg = null;
			if(msc.isOffloadable()) 
			{
				for(ComputationalNode cn : candidateCenters)
				{
					double avgCost = computeAverageCost(msc, cn, subscriberDevices);
					if(avgCost < minAvgCost && cn.isCompatible(msc)	)
					{
						minAvgCost = avgCost;
						trg = cn;
					}
				}
				if(trg == null)
				{
					for(ComputationalNode cn : this.getInfrastructure().getAllNodes())
						if(cn.isCompatible(msc)) 
						{
							trg = cn;
							break;
						}
				}
			}
			else
			{
				for(ComputationalNode cn : this.getInfrastructure().getCloudNodes().values())
				{
					double avgCost = computeAverageCost(msc, cn, subscriberDevices);
					if(avgCost < minAvgCost && cn.isCompatible(msc) )
					//if(avgCost < minAvgCost)
					{
						minAvgCost = avgCost;
						trg = cn;
					}
				}
			}
			if(trg==null)
				return schedulings;
			deploy(scheduling,msc,trg, publisherDevices, subscriberDevices);
			
			currentTimestamp = (int) Math.round(getCurrentTime());
			//System.out.println("TIMESTAMP: "+currentTimestamp);
			for(MobileDevice d : this.getInfrastructure().getMobileDevices().values()) 
				d.updateCoordsWithMobility((double)currentTimestamp);
			//System.out.println("ID: " + d.getId() + "COORDS: " + d.getCoords());
			
			MobilityBasedNetworkPlanner.setupMobileConnections(getInfrastructure());
			MobileDevicePlannerWithMobility.updateDeviceSubscriptions(getInfrastructure(),
					SimulationSetup.selectedWorkflow);			
		}
		double endTime = System.currentTimeMillis();
		double time = endTime - startTime;
		scheduling.setExecutionTime(time);
		schedulings.add(scheduling);
		System.out.println("Sim Time: " + getCurrentTime() + " Update intervals: " + updateIntervals);
		return schedulings;
	}
	
	private boolean connectionExists(ComputationalNode cn, ArrayList<MobileDevice> subscriberDevices) {
		boolean exists = false;
		for(MobileDevice dev : subscriberDevices)
			if(Double.isFinite(computeTransmissionTime(dev, cn)))
			{
				exists = true;
				break;
			}
		return exists;
	}

	private double computeAverageCost(MobileSoftwareComponent msc, ComputationalNode cn,
			ArrayList<MobileDevice> subscriberDevices) {
		double nDevs = subscriberDevices.size();
		double cost = 0.0;
		for(MobileDevice dev : subscriberDevices) 
		{
			if(Double.isInfinite(computeTransmissionTime(dev,cn)))
				return Double.POSITIVE_INFINITY;
			cost += cn.computeCost(msc, getInfrastructure()) * computeTransmissionTime(dev, cn);
		}
		return cost / nDevs;
		
	}

	
		
	private ConnectionMap extractSubgraph(ConnectionMap infrastructureMap, ArrayList<IoTDevice> publishers, ArrayList<MobileDevice> subscribers) 
	{
		MobileDataDistributionInfrastructure currInf = this.getInfrastructure();
		ArrayList<NetworkedNode> vertexList = new ArrayList<NetworkedNode>(infrastructureMap.vertexSet());
		for(NetworkedNode n : vertexList)
		{
			if(!subscribers.contains(n) && !publishers.contains(n) 
					&& !(currInf.getCloudNodes().containsKey(n.getId()) 
							|| currInf.getEdgeNodes().containsKey(n.getId()) ) )
			{
				/*ArrayList<NetworkConnection> outConnList = new ArrayList<NetworkConnection>(infrastructureMap.outgoingEdgesOf(n));
				for(NetworkConnection nc : outConnList) 
					infrastructureMap.removeEdge(nc);
				ArrayList<NetworkConnection> inConnList = new ArrayList<NetworkConnection>(infrastructureMap.incomingEdgesOf(n));
				for(NetworkConnection nc : inConnList) 
					infrastructureMap.removeEdge(nc);*/
				infrastructureMap.removeVertex(n);
			}	
		}
		return infrastructureMap;
	}

	private ArrayList<ComputationalNode> findCenters(ConnectionMap infrastructureMap, int nCenters) {
		ArrayList<ComputationalNode> toReturn = new ArrayList<ComputationalNode>();
		ConnectionMap subGraph = extractSubgraph(infrastructureMap, publisherDevices, subscriberDevices);
		//ConnectionMap subGraph = infrastructureMap;
		
		ComputationalNode center = null;
		Double minMaxDist = Double.MAX_VALUE;

		for(ComputationalNode c : getInfrastructure().getAllNodes()) {
			double tmp = computeMaxDistance(subGraph,c,publisherDevices,subscriberDevices);

			double nDist = (tmp > c.getMaxDistance())? tmp : c.getMaxDistance();
			if(nDist < minMaxDist) 
			{
				minMaxDist = tmp;
				center = c;
			}
		
		}	
		
		for(ComputationalNode c : getInfrastructure().getAllNodes())
			if(infrastructureMap.computeDistance(center, c) <= nCenters)
				toReturn.add(c);
		
		return toReturn;				
	}

	private double computeMaxDistance(ConnectionMap infrastructureMap, ComputationalNode computationalNode, ArrayList<IoTDevice> publisherDevices2,
			ArrayList<MobileDevice> subscriberDevices2) {
		double maxDist = Double.MIN_VALUE;
		double tmp;
		
		for(NetworkedNode n : subscriberDevices2)
			if((tmp = infrastructureMap.computeDistance(computationalNode,n)) > maxDist)
					maxDist = tmp;
		
		
		return maxDist;
	}

	
	
}
