package fdse.microservice.service;

import edu.fudan.common.util.Response;
import fdse.microservice.entity.*;
import fdse.microservice.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class StationServiceImpl implements StationService {

    @Autowired
    private StationRepository repository;

    String success = "Success";

    @Override
    public Response create(Station station, HttpHeaders headers) {
        if (repository.findById(station.getId()) == null) {
            station.setStayTime(station.getStayTime());
            repository.save(station);
            return new Response<>(1, "Create success", station);
        }
        return new Response<>(0, "Already exists", station);
    }


    @Override
    public boolean exist(String stationName, HttpHeaders headers) {
        boolean result = false;
        if (repository.findByName(stationName) != null) {
            result = true;
        }
        return result;
    }

    @Override
    public Response update(Station info, HttpHeaders headers) {

        if (repository.findById(info.getId()) == null) {
            return new Response<>(0, "Station not exist", null);
        } else {
            Station station = new Station(info.getId(), info.getName());
            station.setStayTime(info.getStayTime());
            repository.save(station);
            return new Response<>(1, "Update success", station);
        }
    }

    @Override
    public Response delete(Station info, HttpHeaders headers) {

        if (repository.findById(info.getId()) != null) {
            Station station = new Station(info.getId(), info.getName());
            repository.delete(station);
            return new Response<>(1, "Delete success", station);
        }
        return new Response<>(0, "Station not exist", null);
    }

    @Override
    public Response query(HttpHeaders headers) {
        List<Station> stations = repository.findAll();
        if (stations != null && !stations.isEmpty()) {
            return new Response<>(1, "Find all content", stations);
        } else {
            return new Response<>(0, "No content", null);
        }
    }

    @Override
    public Response queryForId(String stationName, HttpHeaders headers) {
        Station station = repository.findByName(stationName);

        if (station  != null) {
            return new Response<>(1, success, station.getId());
        } else {
            return new Response<>(0, "Not exists", stationName);
        }
    }


    @Override
    public Response queryForIdBatch(List<String> nameList, HttpHeaders headers) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < nameList.size(); i++) {
            Station station = repository.findByName(nameList.get(i));
            if (station == null) {
                result.add("Not Exist");
            } else {
                result.add(station.getId());
            }
        }

        if (!result.isEmpty()) {
            return new Response<>(1, success, result);
        } else {
            return new Response<>(0, "No content according to name list", null);
        }

    }

    @Override
    public Response queryById(String stationId, HttpHeaders headers) {
        Station station = repository.findById(stationId).get();
        if (station != null) {
            return new Response<>(1, success, station.getName());
        } else {
            return new Response<>(0, "No that stationId", stationId);
        }
    }

    @Override
    public Response queryByIdBatch(List<String> idList, HttpHeaders headers) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < idList.size(); i++) {
            Optional<Station> stationOptional = repository.findById(idList.get(i));
            if (stationOptional.isPresent()) {
                Station station = stationOptional.get();
                result.add(station.getName());
            }
        }

        if (!result.isEmpty()) {
            return new Response<>(1, "success", result);
        } else {
            return new Response<>(0, "No stationName list according to stationIdList", result);
        }
    }

}
