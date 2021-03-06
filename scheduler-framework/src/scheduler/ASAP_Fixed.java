package scheduler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ASAP_Fixed extends ASAP {
	
	public Schedule schedule(final Graph graph, final Schedule partialSchedule) {
		Set<Node> queue = new HashSet<Node>();
		Schedule schedule = partialSchedule.clone();
		
		// schedule root nodes and add unscheduled nodes to queue
		for (Node node : graph) {
			if (!schedule.getNodes().keySet().contains(node)) {
				if (node.root()) {
					// add unscheduled root nodes to schedule
					schedule.add(node, new Interval(0, node.getDelay() - 1));
				} else {
					// add all non-root unscheduled nodes to queue
					queue.add(node);
				}
			}
		}
		
		while (queue.size() > 0) {
			
			// Choose node with all predecessors scheduled
			Node candidate = null;
			Iterator<Node> iter = queue.iterator();
			while (candidate == null && iter.hasNext()) {
				Node node = iter.next();
				boolean predUnplanned = false;
				for (Node predecessor : node.allPredecessors().keySet()) {
					if (!schedule.containsNode(predecessor)) {
						predUnplanned = true;
						break;
					}
				}
				candidate = predUnplanned ? null : node;
			}
			if (candidate == null) {
				System.err.println("Found no node with all predecessors planned. Cyclic dependencies?");
				return null;
			}
			
			// Determine earliest possible starting time
			int maxPredEnd = Integer.MIN_VALUE;
			for (Node predecessor : candidate.allPredecessors().keySet()) {
				int predEnd = schedule.getNodes().get(predecessor).ubound;
				if (predEnd > maxPredEnd) {
					maxPredEnd = predEnd; 
				}
			}
			Interval slot = new Interval(maxPredEnd + 1, maxPredEnd + candidate.getDelay());
			
			// Check legality of found slot with successors (data dependencies)
			for (Node successor : candidate.allSuccessors().keySet()) {
				if (schedule.containsNode(successor)) {
					int succBegin = schedule.getNodes().get(successor).lbound;
					if (succBegin <= slot.ubound) {
						System.out.println("Found critical timing problem. No legal schedule possible with given partial schedule.");
						return null;
					}
				}
			}
			
			// Schedule node and remove it from the queue
			schedule.add(candidate, slot);
			queue.remove(candidate);
			
		}
		
		graph.reset();
		return schedule;
	}
}
