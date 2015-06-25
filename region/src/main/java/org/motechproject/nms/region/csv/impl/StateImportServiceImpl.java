package org.motechproject.nms.region.csv.impl;

import org.motechproject.nms.csv.utils.GetLong;
import org.motechproject.nms.csv.utils.GetString;
import org.motechproject.nms.csv.utils.Store;
import org.motechproject.nms.region.csv.StateImportService;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.DistrictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.cellprocessor.ift.CellProcessor;

import java.util.HashMap;
import java.util.Map;

@Service("stateImportService")
public class StateImportServiceImpl extends BaseLocationImportService<State> implements StateImportService {

    public static final String STATE_ID = "StateID";
    public static final String NAME = "Name";

    public static final String STATE_CODE_FIELD = "code";
    public static final String NAME_FIELD = "name";
    public StateDataService stateDataService;

    @Autowired
    public StateImportServiceImpl(StateDataService stateDataService) {
        super(State.class, stateDataService);
        this.stateDataService = stateDataService;
    }

    @Override
    protected Map<String, CellProcessor> getProcessorMapping() {
        Map<String, CellProcessor> mapping = new HashMap<>();
        mapping.put(STATE_ID, new GetLong());
        mapping.put(NAME, new GetString());
        return mapping;
    }

    @Override
    protected Map<String, String> getFieldNameMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(STATE_ID, STATE_CODE_FIELD);
        mapping.put(NAME, NAME_FIELD);
        return mapping;
    }

    @Override
    protected void upsert(State state) {
        State existingState = stateDataService.findByCode(state.getCode());
        if (existingState == null) {
            stateDataService.create(state);
        } else {
            existingState.setCircles(state.getCircles());
            existingState.setName(state.getName());
            existingState.setDistricts(state.getDistricts());
            stateDataService.update(existingState);
        }
    }
}
