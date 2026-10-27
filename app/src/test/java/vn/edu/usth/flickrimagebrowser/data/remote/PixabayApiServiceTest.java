package vn.edu.usth.flickrimagebrowser.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

import retrofit2.Call;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabayHitDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabaySearchResponse;

/**
 * Unit tests verifying Pixabay API service contract, DTO deserialization, and Retrofit client initialization.
 */
public class PixabayApiServiceTest {

    private Gson gson;

    @Before
    public void setUp() {
        gson = new Gson();
    }

    @Test
    public void createService_returnsValidApiServiceInstance() {
        PixabayApiService service = PixabayClient.createService("https://pixabay.com/api/");
        assertNotNull(service);

        Call<PixabaySearchResponse> searchCall = service.searchPhotos(
                "test_key", "nature", "photo", true, 1, 20
        );
        assertNotNull(searchCall);

        Call<PixabaySearchResponse> featuredCall = service.getFeaturedPhotos(
                "test_key", true, "popular", "photo", true, 1, 30
        );
        assertNotNull(featuredCall);

        Call<PixabaySearchResponse> idCall = service.getPhotoById("test_key", "12345");
        assertNotNull(idCall);
    }

    @Test
    public void jsonDeserialization_parsesPixabaySearchResponseCorrectly() {
        String json = "{\n"
                + "  \"total\": 4692,\n"
                + "  \"totalHits\": 500,\n"
                + "  \"hits\": [\n"
                + "    {\n"
                + "      \"id\": 195893,\n"
                + "      \"pageURL\": \"https://pixabay.com/en/blossom-bloom-flower-195893/\",\n"
                + "      \"type\": \"photo\",\n"
                + "      \"tags\": \"blossom, bloom, flower\",\n"
                + "      \"previewURL\": \"https://cdn.pixabay.com/photo/2013/10/15/09/12/flower-195893_150.jpg\",\n"
                + "      \"previewWidth\": 150,\n"
                + "      \"previewHeight\": 99,\n"
                + "      \"webformatURL\": \"https://pixabay.com/get/35bbf209e13e39d2_640.jpg\",\n"
                + "      \"webformatWidth\": 640,\n"
                + "      \"webformatHeight\": 424,\n"
                + "      \"largeImageURL\": \"https://pixabay.com/get/ed6a78f2966b3d4f_1280.jpg\",\n"
                + "      \"imageWidth\": 4000,\n"
                + "      \"imageHeight\": 2250,\n"
                + "      \"imageSize\": 4731420,\n"
                + "      \"views\": 12700,\n"
                + "      \"downloads\": 6400,\n"
                + "      \"likes\": 56,\n"
                + "      \"user\": \"Bob\",\n"
                + "      \"userImageURL\": \"https://cdn.pixabay.com/user/2013/11/05/02-10-23-764_250x250.jpg\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        PixabaySearchResponse response = gson.fromJson(json, PixabaySearchResponse.class);

        assertNotNull(response);
        assertEquals(4692, response.getTotal());
        assertEquals(500, response.getTotalHits());
        assertNotNull(response.getHits());
        assertEquals(1, response.getHits().size());

        PixabayHitDto hit = response.getHits().get(0);
        assertEquals(Long.valueOf(195893L), hit.getId());
        assertEquals("blossom, bloom, flower", hit.getTags());
        assertEquals("Bob", hit.getUser());
        assertEquals("https://cdn.pixabay.com/photo/2013/10/15/09/12/flower-195893_150.jpg", hit.getPreviewUrl());
        assertEquals("https://pixabay.com/get/35bbf209e13e39d2_640.jpg", hit.getWebformatUrl());
        assertEquals("https://pixabay.com/get/ed6a78f2966b3d4f_1280.jpg", hit.getLargeImageUrl());
        assertEquals(150, hit.getPreviewWidth());
        assertEquals(99, hit.getPreviewHeight());
        assertEquals(640, hit.getWebformatWidth());
        assertEquals(424, hit.getWebformatHeight());
        assertEquals(4000, hit.getImageWidth());
        assertEquals(2250, hit.getImageHeight());
        assertEquals(4731420L, hit.getImageSize());
        assertEquals(12700, hit.getViews());
        assertEquals(6400, hit.getDownloads());
        assertEquals(56, hit.getLikes());
        assertEquals("https://pixabay.com/en/blossom-bloom-flower-195893/", hit.getPageUrl());
        assertEquals("photo", hit.getType());
        assertEquals("https://cdn.pixabay.com/user/2013/11/05/02-10-23-764_250x250.jpg", hit.getUserImageUrl());
    }

    @Test
    public void jsonDeserialization_handlesEmptyHitsList() {
        String emptyJson = "{\"total\": 0, \"totalHits\": 0, \"hits\": []}";
        PixabaySearchResponse response = gson.fromJson(emptyJson, PixabaySearchResponse.class);

        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getTotalHits());
        assertNotNull(response.getHits());
        assertTrue(response.getHits().isEmpty());
    }

    @Test
    public void jsonDeserialization_handlesMissingFieldsGracefully() {
        String minimalJson = "{\"hits\": [{}]}";
        PixabaySearchResponse response = gson.fromJson(minimalJson, PixabaySearchResponse.class);

        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getTotalHits());
        assertNotNull(response.getHits());
        assertEquals(1, response.getHits().size());

        PixabayHitDto hit = response.getHits().get(0);
        assertNull(hit.getId());
        assertNull(hit.getTags());
        assertNull(hit.getUser());
        assertNull(hit.getPreviewUrl());
        assertNull(hit.getWebformatUrl());
        assertNull(hit.getLargeImageUrl());
    }
}
