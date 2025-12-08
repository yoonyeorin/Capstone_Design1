package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.CitySearchDto;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CitySearchService {

    private final OkHttpClient client = new OkHttpClient();

    @Value("${google.maps.api.key}")
    private String GOOGLE_API_KEY;

    public List<CitySearchDto> searchCityList(String keyword) {

        List<CitySearchDto> list = new ArrayList<>();
        try {
            String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            String url =
                    "https://maps.googleapis.com/maps/api/place/textsearch/json"
                            + "?query=" + query
                            + "&language=ko"
                            + "&type=locality"            // ⭐ 도시만 필터링
                            + "&key=" + GOOGLE_API_KEY;

            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) return list;

            String body = response.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");

            for (int i = 0; i < results.size(); i++) {
                JsonObject obj = results.get(i).getAsJsonObject();

                CitySearchDto dto = convert(obj);
                list.add(dto);
            }

            // ⭐ 정확히 일치하면 해당 도시만 반환
            for (CitySearchDto c : list) {
                if (c.getName().equalsIgnoreCase(keyword)) {
                    return List.of(c);
                }
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            return list;
        }
    }

    private CitySearchDto convert(JsonObject obj) {

        String name = obj.get("name").getAsString();
        String fullName = obj.has("formatted_address")
                ? obj.get("formatted_address").getAsString()
                : name;

        JsonObject loc = obj.getAsJsonObject("geometry")
                .getAsJsonObject("location");

        return CitySearchDto.builder()
                .name(name)
                .fullName(fullName)
                .lat(loc.get("lat").getAsDouble())
                .lng(loc.get("lng").getAsDouble())
                .placeId(obj.get("place_id").getAsString())
                .build();
    }
}
