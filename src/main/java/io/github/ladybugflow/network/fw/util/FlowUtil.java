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
package io.github.ladybugflow.network.fw.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ladybugflow.network.fw.component.FlowComponentFactory;
import io.github.ladybugflow.network.fw.component.IFlowAccessor;
import io.github.ladybugflow.network.fw.model.FlowDto;
import io.github.ladybugflow.network.fw.persistance.entity.HistoryEdgeEntity;
import io.github.ladybugflow.network.fw.persistance.entity.HistoryNodeEntity;
import io.github.ladybugflow.network.fw.util.model.Convert2JsonEdgeDto;
import io.github.ladybugflow.network.fw.util.model.Convert2JsonFlowDto;
import io.github.ladybugflow.network.fw.util.model.Convert2JsonNodeDto;

/**
 * 
 * @author NoBugLady
 *
 */
public class FlowUtil {

	private static IFlowAccessor flowAccessor = FlowComponentFactory.getFlowAccessor();

	/**
	 * dumpJson
	 * 
	 * @param flowId    flowId
	 * @param historyId historyId
	 * @return json
	 */
	public static String dumpJson(String flowId, String historyId) {

		/*
		 * flow
		 */
		Convert2JsonFlowDto nodeFlowDto = new Convert2JsonFlowDto();

		/*
		 * node
		 */
		List<HistoryNodeEntity> nodeEntityList = flowAccessor.selectNodeByFlowHistoryId(flowId, historyId);
		for (HistoryNodeEntity item : nodeEntityList) {

			Convert2JsonNodeDto nodeNodeDto = new Convert2JsonNodeDto();
			nodeNodeDto.id = item.getNodeId();
			nodeNodeDto.label = item.getNodeName();
			nodeNodeDto.status = item.getNodeStatus();
			nodeNodeDto.status_detail = item.getNodeStatusDetail();

			nodeFlowDto.nodes.add(nodeNodeDto);
		}

		/*
		 * edge
		 */
		List<HistoryEdgeEntity> edgeEntityList = flowAccessor.selectEdgeByFlowHistoryId(flowId, historyId);
		for (HistoryEdgeEntity item : edgeEntityList) {

			Convert2JsonEdgeDto nodeEdgeDto = new Convert2JsonEdgeDto();
			nodeEdgeDto.id = item.getEdgeId();
			nodeEdgeDto.from = item.getFromNodeId();
			nodeEdgeDto.to = item.getToNodeId();

			nodeFlowDto.edges.add(nodeEdgeDto);
		}

		/*
		 * convert flow to json
		 */
		return nodeFlowDto.toJson();

	}
	
	public static FlowDto jsonToFlowDto(String json) {
		Reader reader = new StringReader(json);
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(reader, FlowDto.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
