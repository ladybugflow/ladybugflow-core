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
package io.github.ladybugflow.network.fw;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.ladybugflow.network.fw.annotation.Node;
import io.github.ladybugflow.network.fw.logger.ConsoleLogger;
import io.github.ladybugflow.network.fw.persistance.entity.HistoryNodeEntity;
import io.github.ladybugflow.network.fw.starter.FlowStarter;
import io.github.ladybugflow.network.fw.FlowRunnerHelper;

/**
 * 
 * @author NoBugLady
 *
 */
public class FlowRunner {

	private BlockingQueue<String> completeQueue = new LinkedBlockingQueue<>();

	private Object runnerObj = null;

	static {

		try {
			Class.forName(FlowStarter.class.getName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public FlowRunner() {
		runnerObj = this;
	}

	public FlowRunner(Object customRunnerObject) {
		runnerObj = customRunnerObject;
	}

	/**
	 * execute
	 * 
	 * @param flowId     flowId
	 * @param nodeId     nodeId
	 * @param historyId  historyId
	 * @param nodeEntity nodeEntity
	 * @return execute result
	 * @throws Exception Exception
	 */
	public String execute(String flowId, String nodeId, String historyId, HistoryNodeEntity nodeEntity)
			throws Exception {

		ConsoleLogger logger = ConsoleLogger.getInstance(flowId, historyId);

		String nodeName = nodeEntity.getNodeName();
		logger.info("execute node id:" + nodeId);
		logger.info("execute node name:" + nodeEntity.getNodeName());

		Method methods[] = runnerObj.getClass().getMethods();
		if (methods != null) {
			for (Method method : methods) {
				Node node = method.getAnnotation(Node.class);
				if (node != null) {
					if (node.id().equals(nodeId) || node.label().equals(nodeName)) {
						try {
							Object rtnObj = method.invoke(runnerObj);
							if (rtnObj != null) {
								return rtnObj.toString();
							} else {
								return null;
							}
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
							throw e;
						}
					}
				}
			}
		}
		return null;

	}

	/**
	 * startFlow
	 * 
	 * @return historyId
	 */
	public String startFlow() {
		return startFlow(null);
	}

	/**
	 * startFlow
	 * 
	 * @param startParam startParam
	 * @return historyId
	 */
	public String startFlow(String startParam) {
		return FlowRunnerHelper.startFlow(this, null, null, startParam);
	}

	/**
	 * startFlow
	 * 
	 * @param sync sync
	 * @return historyId
	 */
	public String startFlow(boolean sync) {
		return startFlow(sync, null);
	}

	/**
	 * startFlow
	 * 
	 * @param sync       sync
	 * @param startParam startParam
	 * @return historyId
	 */
	public String startFlow(boolean sync, String startParam) {

		if (!sync) {
			return startFlow(startParam);
		} else {
			String historyId = startFlow(startParam);
			try {
				completeQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return historyId;
		}
	}

	/**
	 * startFlowFromJson
	 * 
	 * @param jsonFileName    jsonFileName
	 * @param jsonFileContent jsonFileContent
	 * @return historyId
	 */
	public String startFlowFromJson(String jsonFileName, String jsonFileContent) {
		return startFlowFromJson(jsonFileName, jsonFileContent, null);
	}

	/**
	 * startFlowFromJson
	 * 
	 * @param jsonFileName    jsonFileName
	 * @param jsonFileContent jsonFileContent
	 * @param startParam      startParam
	 * @return historyId
	 */
	public String startFlowFromJson(String jsonFileName, String jsonFileContent, String startParam) {
		return FlowRunnerHelper.startFlow(this, jsonFileName, jsonFileContent, startParam);
	}

	/**
	 * startFlowFromJson
	 * 
	 * @param jsonFileName    jsonFileName
	 * @param jsonFileContent jsonFileContent
	 * @param sync            sync
	 * @return historyId
	 */
	public String startFlowFromJson(String jsonFileName, String jsonFileContent, boolean sync) {
		return startFlowFromJson(jsonFileName, jsonFileContent, sync, null);
	}

	/**
	 * startFlowFromJson
	 * 
	 * @param jsonFileName    jsonFileName
	 * @param jsonFileContent jsonFileContent
	 * @param sync            sync
	 * @param startParam      startParam
	 * @return historyId
	 */
	public String startFlowFromJson(String jsonFileName, String jsonFileContent, boolean sync, String startParam) {

		if (!sync) {
			return startFlowFromJson(jsonFileName, jsonFileContent, startParam);
		} else {
			String historyId = startFlowFromJson(jsonFileName, jsonFileContent, startParam);
			try {
				completeQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return historyId;
		}

	}

	/**
	 * putComplete
	 * 
	 * @param result result
	 */
	public void putComplete(String result) {
		try {
			completeQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
