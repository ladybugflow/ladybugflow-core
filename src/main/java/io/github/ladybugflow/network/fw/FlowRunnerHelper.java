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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ladybugflow.network.fw.component.FlowComponentFactory;
import io.github.ladybugflow.network.fw.component.IFlowAccessor;
import io.github.ladybugflow.network.fw.constant.FlowStatus;
import io.github.ladybugflow.network.fw.constant.NodeExecuteType;
import io.github.ladybugflow.network.fw.constant.NodeStartType;
import io.github.ladybugflow.network.fw.constant.NodeStatus;
import io.github.ladybugflow.network.fw.logger.ConsoleLogger;
import io.github.ladybugflow.network.fw.model.EdgeDto;
import io.github.ladybugflow.network.fw.model.FlowDto;
import io.github.ladybugflow.network.fw.model.NodeDto;
import io.github.ladybugflow.network.fw.persistance.entity.FlowEntity;
import io.github.ladybugflow.network.fw.persistance.entity.HistoryEdgeEntity;
import io.github.ladybugflow.network.fw.persistance.entity.HistoryFlowEntity;
import io.github.ladybugflow.network.fw.persistance.entity.HistoryNodeEntity;
import io.github.ladybugflow.network.fw.starter.FlowStarter;
import io.github.ladybugflow.network.fw.util.StringUtil;
import io.github.ladybugflow.network.fw.FlowRunner;
import io.github.ladybugflow.network.fw.FlowRunnerHelper;

/**
 * 
 * @author NoBugLady
 *
 */
public class FlowRunnerHelper {

	private static ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();;

	private static Map<String, String> scheduledFlowIdCronMap = new HashMap<String, String>();

	private static Map<String, ScheduledFuture<?>> scheduledFutureMap = new HashMap<String, ScheduledFuture<?>>();

	private static IFlowAccessor flowAccessor = FlowComponentFactory.getFlowAccessor();

	static {
		taskScheduler.initialize();
	}

	/**
	 * startFlow
	 * 
	 * @param flowRunner      flowRunner
	 * @param jsonFileName    jsonFileName
	 * @param jsonFileContent jsonFileContent
	 * @param startParam      startParam
	 * @return historyId
	 */
	public static String startFlow(FlowRunner flowRunner, String jsonFileName, String jsonFileContent,
			String startParam) {

		String json = null;

		if (StringUtil.isNotEmpty(jsonFileContent)) {
			json = jsonFileContent;
		} else {

			if (StringUtil.isNotEmpty(jsonFileName)) {
				String flowPath = jsonFileName.replace(".json", "");
				json = getJsonString(flowPath);
			} else {
				String flowPath = flowRunner.getClass().getName();
				json = getJsonString(flowPath);
			}
		}

		FlowEntity flow = createHistory(json, startParam);
		String flowId = flow.flowEntity.getFlowId();
		String historyId = flow.flowEntity.getHistoryId();

		FlowStarter.flowRunnerMap.put(flowId + "," + historyId, flowRunner);

		List<HistoryNodeEntity> firstNodeList = getFirstNodeId(flow);

		if (firstNodeList != null) {
			for (HistoryNodeEntity firstNode : firstNodeList) {

				if (firstNode.getStartType() == NodeStartType.NODE_START_TYPE_TIMER) {
					String existCron = scheduledFlowIdCronMap.get(firstNode.getFlowId());

					if (existCron == null) {
						ScheduledFuture<?> future = register(firstNode.getStartCron(), flowRunner, jsonFileName,
								jsonFileContent, firstNode.getFlowId(), firstNode.getHistoryId(), firstNode.getNodeId(),
								startParam);
						scheduledFlowIdCronMap.put(firstNode.getFlowId(), firstNode.getStartCron());
						scheduledFutureMap.put(firstNode.getFlowId(), future);
					} else {
						if (existCron.equals(firstNode.getStartCron())) {
							System.out.println("already scheduled:" + firstNode.getFlowId());
						} else {
							boolean cancelResult = scheduledFutureMap.get(firstNode.getFlowId()).cancel(false);

							if (cancelResult) {
								ScheduledFuture<?> future = register(firstNode.getStartCron(), flowRunner, jsonFileName,
										jsonFileContent, firstNode.getFlowId(), firstNode.getHistoryId(),
										firstNode.getNodeId(), startParam);
								scheduledFlowIdCronMap.put(firstNode.getFlowId(), firstNode.getStartCron());
								scheduledFutureMap.put(firstNode.getFlowId(), future);
							} else {
								System.out.println("task cancel faild:" + firstNode.getFlowId());
							}

						}

					}

				} else {

					String firstNodeId = firstNode.getNodeId();
					flowAccessor.updateNodeStatusByNodeId(flowId, historyId, firstNodeId, NodeStatus.READY);
					startFristNode(flowId, historyId, firstNodeId);

				}
			}
		}

		return historyId;

	}

	/**
	 * register
	 * 
	 * @param cron            cron
	 * @param flowRunner      flowRunner
	 * @param jsonFileName    jsonFileName
	 * @param jsonFileContent jsonFileContent
	 * @param flowId          flowId
	 * @param historyId       historyId
	 * @param nodeId          nodeId
	 * @param startParam      startParam
	 * @return ScheduledFuture
	 */
	private static ScheduledFuture<?> register(String cron, FlowRunner flowRunner, String jsonFileName,
			String jsonFileContent, String flowId, String historyId, String nodeId, String startParam) {

		if (!CronExpression.isValidExpression(cron)) {
			System.out.println("not a valied expression:" + cron);
			return null;
		}

		CronExpression exp = CronExpression.parse(cron);
		LocalDateTime nextTime = exp.next(LocalDateTime.now());

		if (nextTime != null) {
			System.out.println("[" + flowId + "] next execute time:"
					+ nextTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
		}

		return taskScheduler.schedule(new Runnable() {
			@Override
			public void run() {

//				FlowRunner flowRunnerNew = flowRunner.getClass().newInstance();
				FlowRunner flowRunnerNew = flowRunner;

				String json = null;

				if (StringUtil.isNotEmpty(jsonFileContent)) {
					json = jsonFileContent;
				} else {

					if (StringUtil.isNotEmpty(jsonFileName)) {
						String flowPath = jsonFileName.replace(".json", "");
						json = getJsonString(flowPath);
					} else {
						String flowPath = flowRunnerNew.getClass().getName();
						json = getJsonString(flowPath);
					}
				}

				FlowEntity flow = createHistory(json, startParam);
				String flowId = flow.flowEntity.getFlowId();
				String historyId = flow.flowEntity.getHistoryId();

				FlowStarter.flowRunnerMap.put(flowId + "," + historyId, flowRunnerNew);

				flowAccessor.updateNodeStatusByNodeId(flowId, historyId, nodeId, NodeStatus.READY);
				startFristNode(flowId, historyId, nodeId);

			}
		}, new CronTrigger(cron));
	}

	/**
	 * createHistory
	 * 
	 * @param json       json
	 * @param startParam startParam
	 * @return FlowEntity
	 */
	private static FlowEntity createHistory(String json, String startParam) {

		String historyId = flowAccessor.createHistoryId();

		FlowEntity flow = loadJson(json, historyId, startParam);
		flowAccessor.saveFlow(flow, json);

		return flow;
	}

	/**
	 * getJsonString
	 * 
	 * @param flowPath flowPath
	 * @return json
	 */
	private static String getJsonString(String flowPath) {
		try (Reader reader = new InputStreamReader(
				FlowRunnerHelper.class.getResourceAsStream("/" + flowPath.replace(".", "/") + ".json"))) {

			int bufferSize = 1024;
			char[] buffer = new char[bufferSize];
			StringBuilder out = new StringBuilder();
			for (int numRead; (numRead = reader.read(buffer, 0, buffer.length)) > 0;) {
				out.append(buffer, 0, numRead);
			}
			return out.toString().trim();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * loadJson
	 * 
	 * @param json       json
	 * @param historyId  historyId
	 * @param startParam startParam
	 * @return FlowEntity
	 */
	private static FlowEntity loadJson(String json, String historyId, String startParam) {

		FlowEntity flowEntityDB = new FlowEntity();

		try (Reader reader = new StringReader(json)) {

			ObjectMapper mapper = new ObjectMapper();
			FlowDto flowDto = mapper.readValue(reader, FlowDto.class);

			HistoryFlowEntity flowEntity = new HistoryFlowEntity();
			flowEntity.setFlowId(flowDto.flowId);
			flowEntity.setHistoryId(historyId);
			flowEntity.setStartParam(startParam);

			ConsoleLogger logger = ConsoleLogger.getInstance(flowDto.flowId, historyId);

			flowEntityDB.flowEntity = flowEntity;

			if (flowDto.nodes != null) {
				for (NodeDto nodeDto : flowDto.nodes) {
					HistoryNodeEntity nodeEntity = new HistoryNodeEntity();
					nodeEntity.setHistoryId(historyId);
					nodeEntity.setFlowId(flowEntity.getFlowId());
					nodeEntity.setNodeId(nodeDto.id);
					nodeEntity.setNodeName(nodeDto.label);
					nodeEntity.setReadyCheck(nodeDto.readyCheck);
					nodeEntity.setStartType(nodeDto.startType);
					nodeEntity.setExecuteType(nodeDto.executeType);
					nodeEntity.setStartCron(nodeDto.startCron);
					nodeEntity.setDisplayFlag(nodeDto.displayFlag);

					flowEntityDB.nodeEntityList.add(nodeEntity);
				}
			}

			if (flowDto.edges != null) {
				for (EdgeDto edgeDto : flowDto.edges) {
					HistoryEdgeEntity edgeEntity = new HistoryEdgeEntity();
					edgeEntity.setFlowId(flowEntity.getFlowId());
					edgeEntity.setHistoryId(historyId);
					edgeEntity.setEdgeId(edgeDto.id);
					edgeEntity.setFromNodeId(edgeDto.from);
					edgeEntity.setToNodeId(edgeDto.to);
					edgeEntity.setEdgeName(edgeDto.label);
					if (StringUtil.isEmpty(edgeDto.label)) {
						edgeEntity.setEdgeName("ok");
					}
					edgeEntity.setEdgeCondition(edgeDto.condition);
					edgeEntity.setDisplayFlag(edgeDto.displayFlag);

					flowEntityDB.edgeEntityList.add(edgeEntity);
				}

			}

			logger.info("json:\n" + json);

			return flowEntityDB;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	//////////////////////////////
	// help function
	//////////////////////////////

	/**
	 * getFirstNodeId
	 * 
	 * @param flow flow
	 * @return HistoryNodeEntity
	 */
	private static List<HistoryNodeEntity> getFirstNodeId(FlowEntity flow) {

		List<HistoryNodeEntity> resultList = new ArrayList<>();

		resultList.addAll(flow.nodeEntityList);

		Set<String> toNodeIdSet = new HashSet<String>();
		for (HistoryEdgeEntity edgeEntity : flow.edgeEntityList) {
			toNodeIdSet.add(edgeEntity.getToNodeId());
		}

		for (int i = resultList.size() - 1; i >= 0; i--) {

			HistoryNodeEntity nodeEntity = resultList.get(i);

			if (toNodeIdSet.contains(nodeEntity.getNodeId())) {
				resultList.remove(i);
			}
		}
		return resultList;
	}

	/**
	 * startFristNode
	 * 
	 * @param flowId    flowId
	 * @param historyId historyId
	 * @param nodeId    nodeId
	 */
	private static void startFristNode(String flowId, String historyId, String nodeId) {

		flowAccessor.updateFlowStatusAndStartTime(flowId, historyId, FlowStatus.PROCESSING);

		HistoryNodeEntity historyNodeEntity = flowAccessor.selectNodeByKey(flowId, nodeId, historyId);
		if (NodeExecuteType.NODE_EXECUTE_TYPE_WAIT_REQUEST == historyNodeEntity.getExecuteType()) {
			flowAccessor.updateNodeStatusByNodeId(flowId, historyId, nodeId, NodeStatus.OPENNING);
		} else {
			flowAccessor.updateNodeStatusByNodeId(flowId, historyId, nodeId, NodeStatus.RUNNING);
		}
		FlowComponentFactory.getReadyQueueSender().putReadyNode(flowId, historyId, nodeId);
	}

}
