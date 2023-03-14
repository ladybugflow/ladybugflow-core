/*
 * Copyright (c) 2021-present, NoBugLady Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.github.ladybugflow.network.fw.queue.complete;

import io.github.ladybugflow.network.fw.component.ICompleteQueueSender;
import io.github.ladybugflow.network.fw.starter.FlowStarter;

/**
 * 
 * @author NoBugLady
 *
 */
public class CompleteQueueSender implements ICompleteQueueSender {

	/**
	 * putCompleteNode
	 * 
	 * @param flowId     flowId
	 * @param historyId  historyId
	 * @param nodeId     nodeId
	 * @param nodeStatus nodeStatus
	 * @param nodeResult nodeResult
	 */
	public void putCompleteNode(String flowId, String historyId, String nodeId, int nodeStatus, String nodeResult) {
		try {
			FlowStarter.nodeCompleteQueue
					.put(new CompleteNodeResult(flowId, historyId, nodeId, nodeStatus, nodeResult));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
